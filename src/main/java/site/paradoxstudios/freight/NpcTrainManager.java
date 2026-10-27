package site.paradoxstudios.freight;

import com.simibubi.create.Create;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.contraptions.glue.SuperGlueEntity;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.graph.EdgePointType;
import com.simibubi.create.content.trains.schedule.Schedule;
import com.simibubi.create.content.trains.schedule.ScheduleEntry;
import com.simibubi.create.content.trains.schedule.destination.DestinationInstruction;
import com.simibubi.create.content.trains.schedule.condition.ScheduledDelay;
import com.simibubi.create.content.trains.station.GlobalStation;
import com.simibubi.create.content.trains.station.StationBlockEntity;
import com.simibubi.create.content.trains.track.TrackBlock;
import com.simibubi.create.content.trains.track.TrackMaterial;
import com.simibubi.create.content.trains.track.TrackShape;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Real Create freight NPC trains, assembled from supplied vanilla structure
 * NBTs at a real Create Station. No simulated minecarts or fake rails.
 *
 * A station needs 68 flat, straight, unoccupied Create rail segments ahead.
 * The physical train is 3 wide x 7 tall x 67 long (six bogey-pair carriages).
 * No world blocks are placed until the complete footprint is validated.
 */
public final class NpcTrainManager {
    private NpcTrainManager(){}
    private static final UUID OWNER=UUID.nameUUIDFromBytes(
        "freightlogistics:system_npc".getBytes(java.nio.charset.StandardCharsets.UTF_8));
    private static final Map<String,Pending> PENDING=new LinkedHashMap<>();
    private record Endpoint(net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dim,
                            BlockPos pos,String name,int attempts){}
    private static final Map<String,Endpoint> ENDPOINTS=new LinkedHashMap<>();
    private static final int[] CARRIAGE_START={0,13,24,35,46,57};
    private static final int[] CARRIAGE_END={10,22,33,44,55,66};
    public record Result(boolean success,String message){}
    private record Pending(net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dim,
                           BlockPos station,String kind,int tries){}

    public static int count(){
        int n=0;
        for(Train t:Create.RAILWAYS.trains.values())if(OWNER.equals(t.owner))n++;
        return n;
    }
    public static List<String> status(){
        List<String> lines=new ArrayList<>();
        for(Train t:Create.RAILWAYS.trains.values()){
            if(!OWNER.equals(t.owner))continue;
            String at=t.getCurrentStation()!=null?t.getCurrentStation().name:"in transit";
            String heading=t.navigation.destination!=null?
                " toward "+t.navigation.destination.name:"";
            String position="";
            if(t.graph!=null&&!t.carriages.isEmpty()){
                var point=t.carriages.get(0).getLeadingPoint();
                if(point!=null && point.edge!=null){
                    var xyz=point.getPosition(t.graph);
                    position=String.format(java.util.Locale.ROOT,
                        " [x %.1f, y %.1f, z %.1f]",xyz.x,xyz.y,xyz.z);
                }
            }
            lines.add(t.name.getString()+" at "+at+heading+position+
                " / speed "+String.format(java.util.Locale.ROOT,"%.3f",t.speed)+
                " / "+(t.derailed?"DERAILED":t.navigation.destination!=null?"moving":"idle"));
        }
        return lines;
    }

    public static void queue(ServerLevel level,BlockPos station,String kind){
        PENDING.put(level.dimension().location()+":"+station.asLong(),
            new Pending(level.dimension(),station,kind,0));
    }

    public static void tick(net.minecraft.server.MinecraftServer server){
        if(server.getTickCount()%20!=0)return;
        List<Map.Entry<String,Pending>> snapshot=new ArrayList<>(PENDING.entrySet());
        for(var entry:new ArrayList<>(ENDPOINTS.entrySet())){
            Endpoint end=entry.getValue();
            ServerLevel level=server.getLevel(end.dim());
            if(level==null||!level.hasChunkAt(end.pos()))continue;
            if(level.getBlockEntity(end.pos()) instanceof StationBlockEntity be){
                GlobalStation gs=be.getStation();
                if(gs==null && end.attempts()>=3)gs=be.edgePoint.createEdgePoint();
                if(gs!=null){
                    gs.name=end.name();
                    be.notifyUpdate();
                    ENDPOINTS.remove(entry.getKey());
                    continue;
                }
            }
            if(end.attempts()>60)ENDPOINTS.remove(entry.getKey());
            else ENDPOINTS.put(entry.getKey(),new Endpoint(end.dim(),end.pos(),
                end.name(),end.attempts()+1));
        }

        // Register all test stations before assembling the first train; its
        // Create Schedule must see both directions at the same time.
        for(var entry:snapshot){
            Pending p=entry.getValue();
            ServerLevel level=server.getLevel(p.dim());
            if(level==null||!level.hasChunkAt(p.station()))continue;
            if(level.getBlockEntity(p.station()) instanceof StationBlockEntity stationBE){
                GlobalStation gs=stationBE.getStation();
                if(gs==null && p.tries()>=3)gs=stationBE.edgePoint.createEdgePoint();
                if(gs!=null){
                    gs.name="NPC Test "+(p.kind().equals("item")?"Freight Start":"Tanker Start");
                    stationBE.notifyUpdate();
                }
            }
        }
        for(var entry:snapshot){
            Pending p=entry.getValue();
            ServerLevel level=server.getLevel(p.dim());
            if(level==null||!level.hasChunkAt(p.station()))continue;
            Result result=spawn(level,p.station(),p.kind());
            if(result.success()){
                FreightLogistics.LOGGER.info("NPC train spawned: {}",result.message());
                PENDING.remove(entry.getKey());
            }else if(p.tries()>=40){
                FreightLogistics.LOGGER.warn("NPC spawn cancelled at {}: {}",p.station(),result.message());
                PENDING.remove(entry.getKey());
            }else {
                PENDING.put(entry.getKey(),new Pending(p.dim(),p.station(),p.kind(),p.tries()+1));
            }
        }

        // Train graph updates and conductor discovery happen asynchronously.
        // Retry an absent timetable after both stations exist, without
        // clobbering user-edited schedules on already operating trains.
        if(server.getTickCount()%100==0){
            for(Train t:Create.RAILWAYS.trains.values()){
                if(!OWNER.equals(t.owner)||t.runtime==null||t.graph==null||
                   t.runtime.getSchedule()!=null)continue;
                GlobalStation at=t.getCurrentStation();
                if(at!=null)configureSchedule(t,at.name);
            }
        }
    }

    private static CompoundTag blueprint(String kind)throws Exception {
        String filename=switch(kind){
            case "fluid" -> "modern_fluid_tanker_train.nbt";
            case "item" -> "modern_item_freight_train.nbt";
            default -> throw new IllegalArgumentException("Use 'fluid' or 'item'.");
        };
        String name="/assets/freightlogistics/npc/"+filename;
        InputStream in=NpcTrainManager.class.getResourceAsStream(name);
        if(in==null){
            try(InputStream encoded=NpcTrainManager.class.getResourceAsStream(name+".b64")){
                if(encoded!=null){
                    String content=new String(encoded.readAllBytes(),
                        java.nio.charset.StandardCharsets.US_ASCII);
                    byte[] bytes=Base64.getMimeDecoder().decode(content);
                    in=new java.io.ByteArrayInputStream(bytes);
                }
            }
        }
        if(in==null) {
            Path external=Path.of("config","freightlogistics","npc",filename);
            if(Files.isRegularFile(external))in=Files.newInputStream(external);
        }
        if(in==null)throw new IllegalStateException(
            "Missing built-in train schematic "+filename+"; place it in config/freightlogistics/npc/");
        try(InputStream stream=in){return NbtIo.readCompressed(stream,NbtAccounter.unlimitedHeap());}
    }

    private static Rotation rotation(Direction dir){
        return switch(dir){
            case SOUTH -> Rotation.NONE;
            case NORTH -> Rotation.CLOCKWISE_180;
            case EAST -> Rotation.COUNTERCLOCKWISE_90;
            case WEST -> Rotation.CLOCKWISE_90;
            default -> Rotation.NONE;
        };
    }
    private static BlockPos map(BlockPos target,Direction dir,int x,int y,int z){
        return target.relative(dir,z-1).relative(dir.getCounterClockWise(),x-1).above(y+1);
    }

    private record Placed(BlockPos pos,BlockState state){}
    public static Result spawn(ServerLevel level,BlockPos stationPos,String kind){
        if(!kind.equals("item")&&!kind.equals("fluid"))
            return new Result(false,"Unknown train type. Use item or fluid.");
        if(count()>=8)return new Result(false,"NPC cap of 8 active trains reached.");
        if(!(level.getBlockEntity(stationPos) instanceof StationBlockEntity be))
            return new Result(false,"No Create Train Station at "+stationPos.toShortString());
        if(!be.edgePoint.hasValidTrack() || be.getStation()==null)
            return new Result(false,"Station has not attached to the Create rail graph yet.");
        GlobalStation current=be.getStation();
        if(current.getPresentTrain()!=null)
            return new Result(false,"Assembly station is occupied by another train.");
        Direction direction=be.getAssemblyDirection();
        if(!direction.getAxis().isHorizontal())
            return new Result(false,"Train assembly direction must be horizontal.");
        BlockPos track=be.edgePoint.getGlobalPosition();
        BlockState base=level.getBlockState(track);
        if(base.getBlock()!=TrackMaterial.ANDESITE.getBlock())
            return new Result(false,"Train requires Create Andesite Track.");
        TrackShape shape=direction.getAxis()==Direction.Axis.X?TrackShape.XO:TrackShape.ZO;

        // The 67-block schematic must follow the train's actual rail line.
        // Fail before placement if any loaded section is graded, curved or
        // belongs to another occupied build.
        for(int i=1;i<=67;i++){
            BlockPos p=track.relative(direction,i);
            if(!level.getChunkSource().hasChunk(p.getX()>>4,p.getZ()>>4))
                return new Result(false,"Load 67 blocks of railway ahead of the station.");
            BlockState rail=level.getBlockState(p);
            if(rail.getBlock()!=TrackMaterial.ANDESITE.getBlock() ||
                !rail.hasProperty(TrackBlock.SHAPE) ||
                rail.getValue(TrackBlock.SHAPE)!=shape)
                return new Result(false,"The first 67 blocks must be a single flat, straight track. Bad track at "+p.toShortString());
        }
        final CompoundTag nbt;
        try {nbt=blueprint(kind);}catch(Exception ex){
            return new Result(false,"Invalid train blueprint: "+ex.getMessage());
        }
        ListTag dims=nbt.getList("size",Tag.TAG_INT);
        if(dims.size()!=3||dims.getInt(0)!=3||dims.getInt(1)!=7||dims.getInt(2)!=67)
            return new Result(false,"Unsupported NBT dimensions; expected 3x7x67.");
        ListTag palette=nbt.getList("palette",Tag.TAG_COMPOUND);
        ListTag blocks=nbt.getList("blocks",Tag.TAG_COMPOUND);
        if(palette.isEmpty()||blocks.isEmpty()||blocks.size()>4096)
            return new Result(false,"Empty or oversized train NBT.");
        HolderLookup.RegistryLookup<Block> lookup=level.registryAccess().lookupOrThrow(Registries.BLOCK);
        List<BlockState> states=new ArrayList<>();
        for(int i=0;i<palette.size();i++){
            BlockState bs=NbtUtils.readBlockState(lookup,palette.getCompound(i));
            states.add(bs.rotate(rotation(direction)));
        }
        List<Placed> placed=new ArrayList<>();
        Set<BlockPos> occupied=new HashSet<>();
        for(int i=0;i<blocks.size();i++){
            CompoundTag obj=blocks.getCompound(i);
            ListTag coords=obj.getList("pos",Tag.TAG_INT);
            if(coords.size()!=3)return new Result(false,"Malformed block position.");
            int x=coords.getInt(0),y=coords.getInt(1),z=coords.getInt(2);
            int idx=obj.getInt("state");
            if(idx<0||idx>=states.size()||x<0||x>2||y<0||y>6||z<0||z>66)
                return new Result(false,"Malformed block palette or footprint.");
            BlockState state=states.get(idx);
            if(state.isAir())continue;
            BlockPos pos=map(track,direction,x,y,z);
            occupied.add(pos);
            placed.add(new Placed(pos,state));
        }

        // Inject two lit Blaze Burner conductors adjacent to the
        // original two opposed Create Train Controls. They belong to
        // the locomotive's glued carriage and survive assembly.
        BlockState conductor=AllBlocks.BLAZE_BURNER.getDefaultState()
            .setValue(BlazeBurnerBlock.HEAT_LEVEL,BlazeBurnerBlock.HeatLevel.KINDLED);
        for(int z:new int[]{6,10}){
            BlockPos at=map(track,direction,1,1,z);
            placed.removeIf(e->e.pos().equals(at));
            placed.add(new Placed(at,conductor));
            occupied.add(at);
        }

        // Validate the whole hull, not only individual visible blocks.
        for(int z=0;z<=66;z++)for(int x=0;x<=2;x++)
            for(int y=0;y<=6;y++){
                BlockPos at=map(track,direction,x,y,z);
                if(!level.getChunkSource().hasChunk(at.getX()>>4,at.getZ()>>4))
                    return new Result(false,"Load the whole 3x7x67 train footprint.");
                BlockState existing=level.getBlockState(at);
                if(!existing.isAir() && !existing.canBeReplaced())
                    return new Result(false,"Train placement obstructed at "+at.toShortString());
            }

        List<SuperGlueEntity> glue=new ArrayList<>();
        try {
            for(Placed entry:placed){
                if(!level.setBlock(entry.pos(),entry.state(),Block.UPDATE_ALL))
                    throw new IllegalStateException("Block placement failed at "+entry.pos());
            }
            for(int car=0;car<CARRIAGE_START.length;car++){
                BlockPos min=map(track,direction,0,0,CARRIAGE_START[car]);
                BlockPos max=map(track,direction,2,6,CARRIAGE_END[car]);
                AABB bb=new AABB(
                    Math.min(min.getX(),max.getX()),Math.min(min.getY(),max.getY()),Math.min(min.getZ(),max.getZ()),
                    Math.max(min.getX(),max.getX())+1,Math.max(min.getY(),max.getY())+1,Math.max(min.getZ(),max.getZ())+1);

                SuperGlueEntity glueEntity=new SuperGlueEntity(level,bb);
                if(!level.addFreshEntity(glueEntity))
                    throw new IllegalStateException("Could not add glue entity");
                glue.add(glueEntity);
            }
            Set<UUID> before=new HashSet<>(Create.RAILWAYS.trains.keySet());
            if(!be.enterAssemblyMode(null))
                throw new IllegalStateException("Create rejected station assembly mode");
            be.assemble(OWNER);
            Train train=null;
            for(Train t:Create.RAILWAYS.trains.values())
                if(!before.contains(t.id) && OWNER.equals(t.owner)){train=t;break;}
            if(train==null)throw new IllegalStateException(
                "Create failed to assemble the schematic. Check station assembly limits and train blocks.");
            be.exitAssemblyMode();
            train.name=Component.literal(kind.equals("fluid")?"NPC Fluid Tanker":"NPC Item Freight");
            net.createmod.catnip.platform.CatnipServices.NETWORK.sendToAllClients(
                new com.simibubi.create.content.trains.station.TrainEditPacket.TrainEditReturnPacket(
                    train.id,train.name.getString(),train.icon.getId(),train.mapColorIndex));
            for(SuperGlueEntity g:glue)if(g.isAlive())g.discard();
            configureSchedule(train,current.name);
            Create.RAILWAYS.markTracksDirty();
            return new Result(true,train.name.getString()+" assembled as genuine Create train "+train.id);
        } catch(Exception ex){
            // Failed assemblies must not leave train blocks or invisible glue
            // littering a station. Restore only this command's own footprint.
            for(SuperGlueEntity g:glue)g.discard();
            for(Placed entry:placed)
                if(occupied.contains(entry.pos()))
                    level.setBlock(entry.pos(),Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL);
            if(be.isAssembling())be.exitAssemblyMode();
            return new Result(false,"Assembly failed safely: "+ex.getMessage());
        }
    }

    private static void configureSchedule(Train train,String origin){
        if(train.graph==null)return;
        List<String> destinationNames=new ArrayList<>();
        for(GlobalStation station:train.graph.getPoints(EdgePointType.STATION)){
            if(station.name.startsWith("NPC Test ")||
               station.name.startsWith("Freight Valley "))
                destinationNames.add(station.name);
        }
        Collections.sort(destinationNames);
        destinationNames=new ArrayList<>(new LinkedHashSet<>(destinationNames));
        if(destinationNames.size()<2)return;
        String first=destinationNames.stream().filter(s->!s.equals(origin))
            .findFirst().orElse(destinationNames.get(0));
        List<ScheduleEntry> entries=new ArrayList<>();
        for(String destination:new String[]{first,origin}){
            DestinationInstruction instruction=new DestinationInstruction();
            instruction.getData().putString("Text",destination);
            entries.add(new ScheduleEntry(instruction,List.of(List.of(new ScheduledDelay()))));
        }
        train.runtime.setSchedule(new Schedule(entries,true,0),true);
    }

    /**
     * 192-block self-contained test siding. It is opt-in and will only place
     * into a completely empty, preloaded and straight volume. No terrain or
     * player buildings are replaced. Two stations are used to test timetable
     * reversing without claiming a world network is already available.
     */
    public static Result createTestYard(ServerLevel level,BlockPos origin){
        int length=288;
        int y=origin.getY(),z=origin.getZ(),x=origin.getX();
        if(y<level.getMinBuildHeight()+2||y+9>=level.getMaxBuildHeight())
            return new Result(false,"Choose a valid build height.");
        // Two physically separate test tracks stop real freight trains from
        // facing one another on a single line. The demo never changes an
        // existing railway and is deliberately not automatic world generation.
        for(int lane:new int[]{0,16}){
            int lineZ=z+lane;
            for(int i=0;i<length;i++)
                for(int dz=-2;dz<=6;dz++)
                    for(int dy=0;dy<=8;dy++){
                        BlockPos p=new BlockPos(x+i,y+dy,lineZ+dz);
                        BlockState old=level.getBlockState(p);
                        if(!old.isAir())
                            return new Result(false,
                                "NPC test yard needs empty space: "+p.toShortString());
                    }
        }
        for(int lane:new int[]{0,16}){
            int lineZ=z+lane;
            String type=lane==0?"item":"fluid";
            String label=lane==0?"Freight":"Tanker";
            for(int i=0;i<length;i++){
                BlockPos rail=new BlockPos(x+i,y,lineZ);
                BlockPos bed=rail.below();
                if(level.getBlockState(bed).isAir())
                    level.setBlock(bed,Blocks.STONE_BRICKS.defaultBlockState(),Block.UPDATE_ALL);
                level.setBlock(rail,TrackMaterial.ANDESITE.getBlock().defaultBlockState()
                    .setValue(TrackBlock.SHAPE,TrackShape.XO),Block.UPDATE_ALL);
            }
            // At least 96 tracks continue beyond either terminus, so a 67-block
            // consist can approach and reverse without falling off the edge.
            for(int offset:new int[]{96,192}){
                BlockPos stationPos=new BlockPos(x+offset,y,lineZ+5);
                if(level.getBlockState(stationPos.below()).isAir())
                    level.setBlock(stationPos.below(),Blocks.STONE_BRICKS.defaultBlockState(),Block.UPDATE_ALL);
                level.setBlock(stationPos,AllBlocks.TRACK_STATION.getDefaultState(),Block.UPDATE_ALL);
                if(level.getBlockEntity(stationPos) instanceof StationBlockEntity be){
                    CompoundTag target=new CompoundTag();
                    target.put("TargetTrack",NbtUtils.writeBlockPos(
                        new BlockPos(x+offset,y,lineZ).subtract(stationPos)));
                    target.putBoolean("TargetDirection",offset==96);
                    be.edgePoint.read(target,level.registryAccess(),false);
                    be.setChanged();
                    if(offset==96)queue(level,stationPos,type);
                    else ENDPOINTS.put(level.dimension().location()+":"+stationPos.asLong(),
                        new Endpoint(level.dimension(),stationPos,"NPC Test "+label+" End",0));
                }
            }
        }
        return new Result(true,
            "Two independent 288-block freight yards generated, four Create stations placed; item and tanker assembly queued.");
    }

}
