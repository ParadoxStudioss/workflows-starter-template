package site.paradoxstudios.freight;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.trains.station.StationBlockEntity;
import com.simibubi.create.content.trains.track.TrackMaterial;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import java.util.*;

/**
 * 41-long, twin-platform railway station with departure hall, covered platforms,
 * columns, lamps, cargo shed, and a real Create train station connected to rail.
 *
 * The station is built only when all the required NEW track segments and their
 * chunks are present. Never forces terrain generation of unvisited chunks.
 */
public final class RailwayStationBuilder {
    private RailwayStationBuilder(){}
    private static final Set<String> COMPLETE=new HashSet<>();

    public static boolean build(ServerLevel level,RailwayNetworkGenerator.StationRequest req){
        BlockPos request=req.stationPos();
        int cx=request.getX(),cz=request.getZ()-5;
        String key=level.dimension().location()+":"+cx+":"+cz;
        if(COMPLETE.contains(key))return true;
        int spacing=RailwayGenerationConfig.GRID_SPACING.get()*16;
        // Plan ahead: all station width is in already loaded chunks.
        for(int x=cx-23;x<=cx+23;x+=8)
            for(int z=cz-9;z<=cz+17;z+=8)
                if(!level.getChunkSource().hasChunk(x>>4,z>>4))return false;
        for(int x=cx-20;x<=cx+20;x+=4){
            int y=RailwayTerrain.route(level,x,cz,spacing);
            if(level.getBlockState(new BlockPos(x,y,cz)).getBlock()!=TrackMaterial.ANDESITE.getBlock())
                return false;
        }
        int yCenter=RailwayTerrain.route(level,cx,cz,spacing);
        BlockPos stationPos=new BlockPos(cx,yCenter,cz+5);
        // A player's tile, door or unrelated construction must never be wiped.
        if(!RailwayNetworkGenerator.safe(level,stationPos))return false;

        // Both sides of the active railway get 41-block-long, graded platforms.
        for(int dx=-20;dx<=20;dx++){
            int x=cx+dx,y=RailwayTerrain.route(level,x,cz,spacing);
            for(int sign:new int[]{-1,1}){
                for(int d=2;d<=7;d++){
                    int z=cz+sign*d;
                    BlockPos foundation=new BlockPos(x,y-1,z);
                    if(RailwayNetworkGenerator.safe(level,foundation))level.setBlock(foundation,
                        d==2?Blocks.SMOOTH_STONE.defaultBlockState():
                            d==7?Blocks.POLISHED_ANDESITE.defaultBlockState():
                                Blocks.STONE_BRICKS.defaultBlockState(),Block.UPDATE_ALL);
                    BlockPos clear=foundation.above();
                    for(int dy=0;dy<6;dy++){
                        BlockPos at=clear.above(dy);
                        if(RailwayNetworkGenerator.replaceableNatural(level.getBlockState(at)))
                            level.setBlock(at,Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL);
                    }
                }
                // Copper edge and tall pine canopy with regular supports.
                if(dx%5==0){
                    BlockPos pillar=new BlockPos(x,y,cz+sign*7);
                    for(int dy=0;dy<=5;dy++){
                        BlockPos at=pillar.above(dy);
                        if(RailwayNetworkGenerator.safe(level,at))
                            level.setBlock(at,Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState(),Block.UPDATE_ALL);
                    }
                    BlockPos lighting=pillar.offset(0,5,-sign);
                    if(RailwayNetworkGenerator.safe(level,lighting))
                        level.setBlock(lighting,Blocks.SEA_LANTERN.defaultBlockState(),Block.UPDATE_ALL);
                }
                for(int d=2;d<=8;d++){
                    BlockPos roof=new BlockPos(x,y+6,cz+sign*d);
                    if(RailwayNetworkGenerator.safe(level,roof))
                        level.setBlock(roof,(dx%5==0)?Blocks.CUT_COPPER.defaultBlockState():
                            Blocks.DARK_OAK_SLAB.defaultBlockState(),Block.UPDATE_ALL);
                }
            }
            if(Math.abs(dx)<17&&dx%6==0){
                BlockPos bench=new BlockPos(x,y,cz+5);
                if(RailwayNetworkGenerator.safe(level,bench))
                    level.setBlock(bench,Blocks.SPRUCE_STAIRS.defaultBlockState(),Block.UPDATE_ALL);
            }
        }
        // A compact but complete wooden station building and freight office,
        // offset from the platform (never in the actual rail clearance).
        int baseY=yCenter;
        for(int dx=-10;dx<=10;dx++)for(int dz=10;dz<=16;dz++){
            BlockPos floor=new BlockPos(cx+dx,baseY-1,cz+dz);
            if(RailwayNetworkGenerator.safe(level,floor))
                level.setBlock(floor,Blocks.SPRUCE_PLANKS.defaultBlockState(),Block.UPDATE_ALL);
            boolean border=dx==-10||dx==10||dz==10||dz==16;
            for(int dy=0;dy<=4;dy++){
                BlockPos wall=floor.above(dy+1);
                if(!RailwayNetworkGenerator.safe(level,wall))continue;
                if(border) {
                    boolean window=dy==2&&dx%5==0;
                    level.setBlock(wall,window?Blocks.GLASS_PANE.defaultBlockState():
                        ((dx%5==0)?Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState()
                            :Blocks.SPRUCE_PLANKS.defaultBlockState()),Block.UPDATE_ALL);
                }else if(RailwayNetworkGenerator.replaceableNatural(level.getBlockState(wall)))
                    level.setBlock(wall,Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL);
            }
            BlockPos roof=floor.above(6);
            if(RailwayNetworkGenerator.safe(level,roof))
                level.setBlock(roof,Math.abs(dx)==10?Blocks.CUT_COPPER.defaultBlockState():
                    Blocks.SPRUCE_SLAB.defaultBlockState(),Block.UPDATE_ALL);
        }
        for(int dx=-2;dx<=2;dx++){
            BlockPos door=new BlockPos(cx+dx,baseY,cz+10);
            for(int dy=0;dy<3;dy++){
                BlockPos pos=door.above(dy);
                if(RailwayNetworkGenerator.safe(level,pos))
                    level.setBlock(pos,Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL);
            }
        }
        // Create station block remains a real track-targeting block entity.
        level.setBlock(stationPos,AllBlocks.TRACK_STATION.getDefaultState(),Block.UPDATE_ALL);
        if(level.getBlockEntity(stationPos) instanceof StationBlockEntity be){
            BlockPos track=new BlockPos(cx,yCenter,cz);
            CompoundTag nbt=new CompoundTag();
            nbt.put("TargetTrack",NbtUtils.writeBlockPos(track.subtract(stationPos)));
            nbt.putBoolean("TargetDirection",true);
            be.edgePoint.read(nbt,level.registryAccess(),false);
            be.setChanged();
        }
        COMPLETE.add(key);
        return true;
    }
}
