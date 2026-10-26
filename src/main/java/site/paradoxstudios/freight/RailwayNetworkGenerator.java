package site.paradoxstudios.freight;

import com.simibubi.create.content.trains.track.TrackBlock;
import com.simibubi.create.content.trains.track.TrackMaterial;
import com.simibubi.create.content.trains.track.TrackShape;
import com.simibubi.create.content.trains.station.StationBlockEntity;
import com.simibubi.create.AllBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.tags.BlockTags;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds one chunk-local section of a deterministic infinite *physical* grid.
 * The geometry of each border is a pure function of world coordinates and
 * config; opposite sides of independently generated chunks always agree.
 *
 * No neighboring chunk is read, force-loaded or written by this method.
 * We refuse to overwrite block entities, bedrock, and structure materials.
 */
public final class RailwayNetworkGenerator {
    private RailwayNetworkGenerator(){}
    public record StationRequest(BlockPos stationPos,String name){}
    public record Result(boolean relevant,boolean built,boolean blocked,
                         int tracks,int supports,List<StationRequest> stations){}

    public static boolean relevant(ChunkPos chunk) {
        int size=RailwayGenerationConfig.GRID_SPACING.get();
        return Math.floorMod(chunk.x,size)==0 || Math.floorMod(chunk.z,size)==0;
    }

    public static Result generate(ServerLevel level,LevelChunk chunk){
        int spacing=RailwayGenerationConfig.GRID_SPACING.get();
        boolean zLine=Math.floorMod(chunk.getPos().z,spacing)==0;
        boolean xLine=Math.floorMod(chunk.getPos().x,spacing)==0;
        if(!zLine&&!xLine)return new Result(false,false,false,0,0,List.of());
        final int y=RailwayGenerationConfig.ELEVATION.get();
        final int bx=chunk.getPos().getMinBlockX(),bz=chunk.getPos().getMinBlockZ();
        final int lineX=bx+8,lineZ=bz+8;
        final Block trackBlock=TrackMaterial.ANDESITE.getBlock();
        List<BlockPos> rails=new ArrayList<>();
        for(int i=0;i<16;i++){
            if(zLine)rails.add(new BlockPos(bx+i,y,lineZ));
            if(xLine && (!zLine || i!=8))rails.add(new BlockPos(lineX,y,bz+i));
        }
        // Reserve entire 16-block rail segment in advance. Never leave a half-built
        // rail line if one protected tile is sitting in its path.
        for(BlockPos rail:rails){
            if(!safeCorridor(level,rail,trackBlock,y, zLine&&rail.getZ()==lineZ, xLine&&rail.getX()==lineX))return new Result(true,false,true,0,0,List.of());
        }
        boolean willStation=RailwayGenerationConfig.STATIONS.get()&&zLine
                &&Math.floorMod(chunk.getPos().x,spacing)==4;
        BlockPos station=willStation?new BlockPos(bx+8,y,lineZ+5):null;
        if(willStation&&!safeStationSite(level,station))willStation=false;

        int tracks=0,supports=0;
        for(BlockPos pos:rails) {
            boolean horizontal=zLine&&pos.getZ()==lineZ;
            boolean vertical=xLine&&pos.getX()==lineX;
            TrackShape shape=horizontal&&vertical?TrackShape.CR_O:
                horizontal?TrackShape.XO:TrackShape.ZO;
            BlockState existing=level.getBlockState(pos);
            if(existing.is(trackBlock)&&existing.hasProperty(TrackBlock.SHAPE)
                    &&existing.getValue(TrackBlock.SHAPE)==shape)continue;
            int surface=level.getHeight(Heightmap.Types.WORLD_SURFACE,pos.getX(),pos.getZ())-1;
            // Narrow 3-wide x 5-high tunnel clearance. No heavy mountain excavation;
            // everything outside the actual track and envelope stays intact.
            boolean shouldTunnel=surface>=y+2;
            if(shouldTunnel&&!RailwayGenerationConfig.TUNNELS.get())continue;
            if(surface<y-2&&!RailwayGenerationConfig.BRIDGES.get())continue;
            Direction lateral=horizontal?Direction.SOUTH:Direction.EAST;
            for(int side=-2;side<=2;side++){
                BlockPos center=pos.relative(lateral,side);
                for(int dy=0;dy<=5;dy++){
                    BlockPos clear=center.above(dy);
                    if(clear.getY()>level.getMaxBuildHeight()-2)continue;
                    // Actual track is placed after clearing; do not delete an
                    // already-existing other Create line at the intersection.
                    if(clear.equals(pos)&&existing.is(trackBlock))continue;
                    if(clear.equals(pos))continue;
                    BlockState current=level.getBlockState(clear);
                    if(current.isAir())continue;
                    if(current.getBlock()==trackBlock)continue;
                    if(replaceableNatural(current))level.setBlock(clear,Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL);
                }
            }
            // Enclose underground rock cuts so stone, gravel or water from above
            // cannot immediately fall or flow into the train's clearance area.
            if(shouldTunnel){
                for(int side=-2;side<=2;side++){
                    BlockPos roof=pos.relative(lateral,side).above(6);
                    if(safe(level,roof))
                        level.setBlock(roof,Blocks.STONE_BRICKS.defaultBlockState(),Block.UPDATE_ALL);
                }
                for(int side:new int[]{-3,3}){
                    for(int dy=0;dy<=5;dy++){
                        BlockPos wall=pos.relative(lateral,side).above(dy);
                        if(safe(level,wall))
                            level.setBlock(wall,Blocks.STONE_BRICKS.defaultBlockState(),Block.UPDATE_ALL);
                    }
                }
            }
            BlockPos base=pos.below();
            if(level.getBlockState(base).isAir()||replaceableNatural(level.getBlockState(base)))
                level.setBlock(base,Blocks.POLISHED_ANDESITE.defaultBlockState(),Block.UPDATE_ALL);
            // Every eighth block receives a viaduct support; no giant vertical
            // walls, and a support only descends inside THIS loaded chunk.
            if(surface<y-3 && Math.floorMod(horizontal?pos.getX():pos.getZ(),8)==0){
                int bottom=Math.max(surface+1,y-32);
                for(int py=y-2;py>=bottom;py--){
                    BlockPos pillar=new BlockPos(pos.getX(),py,pos.getZ());
                    BlockState old=level.getBlockState(pillar);
                    if(!old.isAir()&&!replaceableNatural(old))break;
                    level.setBlock(pillar,Blocks.STONE_BRICKS.defaultBlockState(),Block.UPDATE_ALL);
                    supports++;
                }
            }
            BlockState newTrack=trackBlock.defaultBlockState().setValue(TrackBlock.SHAPE,shape);
            level.setBlock(pos,newTrack,Block.UPDATE_ALL);
            tracks++;
        }

        List<StationRequest> stations=new ArrayList<>();
        if(willStation) {
            // Platform is entirely inside the owning chunk. The Create station
            // block targets the horizontal track 5 blocks to its north.
            int centerX=station.getX();
            for(int dx=-5;dx<=5;dx++) {
                int px=centerX+dx;
                for(int zOffset=2;zOffset<=5;zOffset++){
                    BlockPos deck=new BlockPos(px,y-1,lineZ+zOffset);
                    BlockState old=level.getBlockState(deck);
                    if(old.isAir()||replaceableNatural(old))
                        level.setBlock(deck,Blocks.STONE_BRICKS.defaultBlockState(),Block.UPDATE_ALL);
                    BlockPos clear=deck.above();
                    for(int dy=0;dy<=5;dy++){
                        BlockPos air=clear.above(dy);
                        if(replaceableNatural(level.getBlockState(air)))
                            level.setBlock(air,Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL);
                    }
                }
            }
            // Simple, readable passenger platform canopy and pillars.
            for(int dx=-6;dx<=6;dx++)
                for(int dz=3;dz<=5;dz++){
                    BlockPos canopy=new BlockPos(centerX+dx,y+6,lineZ+dz);
                    if(safe(level,canopy))
                        level.setBlock(canopy,dx==-6||dx==6?Blocks.CUT_COPPER.defaultBlockState()
                            :Blocks.SPRUCE_SLAB.defaultBlockState(),Block.UPDATE_ALL);
                }
            for(int dx:new int[]{-6,6}){
                for(int yy=y;yy<=y+5;yy++){
                    BlockPos col=new BlockPos(centerX+dx,yy,lineZ+5);
                    if(safe(level,col))
                        level.setBlock(col,Blocks.SPRUCE_FENCE.defaultBlockState(),Block.UPDATE_ALL);
                }
            }
            for(int dx:new int[]{-5,5}){
                BlockPos lamp=new BlockPos(centerX+dx,y,lineZ+4);
                if(safe(level,lamp))
                    level.setBlock(lamp,Blocks.LANTERN.defaultBlockState(),Block.UPDATE_ALL);
            }
            BlockPos track=new BlockPos(centerX,y,lineZ);
            if(level.getBlockState(track).getBlock()==trackBlock) {
                BlockPos stationBlockPos=station;
                level.setBlock(stationBlockPos,AllBlocks.TRACK_STATION.getDefaultState(),Block.UPDATE_ALL);
                if(level.getBlockEntity(stationBlockPos) instanceof StationBlockEntity be) {
                    CompoundTag targeting=new CompoundTag();
                    targeting.put("TargetTrack",NbtUtils.writeBlockPos(track.subtract(stationBlockPos)));
                    targeting.putBoolean("TargetDirection",true);
                    be.edgePoint.read(targeting,level.registryAccess(),false);
                    be.setChanged();
                    int rx=Math.floorDiv(centerX-8,spacing*16);
                    int rz=Math.floorDiv(lineZ-8,spacing*16);
                    stations.add(new StationRequest(stationBlockPos,"Railway "+rx+" / "+rz));
                }
            }
        }
        return new Result(true,tracks>0,false,tracks,supports,stations);
    }

    private static boolean safeStationSite(ServerLevel level,BlockPos station) {
        if(station==null||station.getY()<level.getMinBuildHeight()+4)return false;
        // Guarantee a passenger-sized 5-block-high space around the platform.
        for(int dx=-6;dx<=6;dx++)
            for(int dz=-4;dz<=1;dz++)
                for(int dy=0;dy<=5;dy++)
                    if(!safe(level,station.offset(dx,dy,dz)))return false;
        return true;
    }
    private static boolean safeCorridor(ServerLevel level,BlockPos p,Block block,int y,
                                        boolean horizontal,boolean vertical) {
        if(y<level.getMinBuildHeight()+4 || y>level.getMaxBuildHeight()-8)return false;
        BlockState rail=level.getBlockState(p);
        if(rail.getBlock()!=block&&!safe(level,p))return false;
        // Check only perpendicular sides; checking forward neighbors here would
        // accidentally request an adjacent chunk at the edge of our 16-block tile.
        for(int step=-2;step<=2;step++)for(int height=0;height<=6;height++){
            BlockPos test=horizontal?p.offset(0,height,step):p.offset(step,height,0);
            if(!safe(level,test))return false;
            if(horizontal&&vertical){
                BlockPos alternate=p.offset(step,height,0);
                if(!safe(level,alternate))return false;
            }
        }
        return true;
    }
    private static boolean safe(ServerLevel level,BlockPos p) {
        BlockState state=level.getBlockState(p);
        if(state.isAir()||state.getFluidState().isSource())return true;
        if(state.hasBlockEntity() || state.is(Blocks.BEDROCK) ||
           state.is(Blocks.OBSIDIAN) || state.is(Blocks.END_PORTAL_FRAME) ||
           state.is(BlockTags.DOORS) || state.is(BlockTags.BEDS) ||
           state.is(BlockTags.RAILS) || state.is(BlockTags.SIGNS) ||
           state.is(BlockTags.FENCES) || state.is(BlockTags.PLANKS) ||
           state.is(Blocks.CHEST) || state.is(Blocks.BARREL) ||
           state.is(Blocks.IRON_BLOCK))return false;
        // Existing Create railway is protected unless it is ours and aligned.
        if(state.getBlock() instanceof TrackBlock)return true;
        return replaceableNatural(state);
    }
    private static boolean replaceableNatural(BlockState state){
        if(state.isAir())return true;
        if(state.is(BlockTags.LOGS)||state.is(BlockTags.LEAVES)||state.is(BlockTags.FLOWERS))
            return true;
        Block b=state.getBlock();
        return b==Blocks.STONE||b==Blocks.DEEPSLATE||b==Blocks.GRANITE||
            b==Blocks.DIORITE||b==Blocks.ANDESITE||b==Blocks.TUFF||
            b==Blocks.DIRT||b==Blocks.GRASS_BLOCK||b==Blocks.COARSE_DIRT||
            b==Blocks.ROOTED_DIRT||b==Blocks.GRAVEL||b==Blocks.SAND||
            b==Blocks.RED_SAND||b==Blocks.SANDSTONE||b==Blocks.RED_SANDSTONE||
            b==Blocks.CLAY||b==Blocks.SNOW_BLOCK||b==Blocks.SNOW||
            b==Blocks.ICE||b==Blocks.PACKED_ICE||b==Blocks.POWDER_SNOW||
            b==Blocks.WATER||b==Blocks.LAVA||b==Blocks.SHORT_GRASS||
            b==Blocks.TALL_GRASS||b==Blocks.FERN||b==Blocks.LARGE_FERN||
            b==Blocks.MOSS_BLOCK||b==Blocks.MOSS_CARPET||
            b==Blocks.CALCITE||b==Blocks.DRIPSTONE_BLOCK;
    }
}
