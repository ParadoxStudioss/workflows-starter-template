package site.paradoxstudios.freight;

import com.simibubi.create.content.trains.track.TrackBlock;
import com.simibubi.create.content.trains.track.TrackMaterial;
import com.simibubi.create.content.trains.track.TrackShape;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.tags.BlockTags;
import java.util.*;

/** New-chunk-only terrain-following generation of ballasted Create railway. */
public final class RailwayNetworkGenerator {
    private RailwayNetworkGenerator(){}
    public record StationRequest(BlockPos stationPos,String name){}
    public record Result(boolean relevant,boolean built,boolean blocked,
                         int tracks,int supports,List<StationRequest> stations){}
    public static boolean relevant(ChunkPos chunk){
        int s=RailwayGenerationConfig.GRID_SPACING.get();
        return Math.floorMod(chunk.x,s)==0||Math.floorMod(chunk.z,s)==0;
    }
    private record Rail(BlockPos pos,TrackShape shape,Direction side,int ground){}

    public static Result generate(ServerLevel level,LevelChunk chunk) {
        ChunkPos cp=chunk.getPos();
        int spacing=RailwayGenerationConfig.GRID_SPACING.get();
        int dist=spacing*16;
        boolean eastWest=Math.floorMod(cp.z,spacing)==0;
        boolean northSouth=Math.floorMod(cp.x,spacing)==0;
        if(!eastWest&&!northSouth)return new Result(false,false,false,0,0,List.of());
        int startX=cp.getMinBlockX(),startZ=cp.getMinBlockZ();
        int trackX=startX+8,trackZ=startZ+8;
        Block block=TrackMaterial.ANDESITE.getBlock();
        List<Rail> plan=new ArrayList<>();
        // Build independent railway geometries with the same 2-D altitude field.
        // Lines meet at the same height in the shared intersection block.
        for(int i=0;i<16;i++){
            if(eastWest){
                int x=startX+i,z=trackZ;
                if(!railFor(level,plan,x,z,true,dist))return blocked();
            }
            if(northSouth){
                int x=trackX,z=startZ+i;
                if(eastWest&&i==8)continue;
                if(!railFor(level,plan,x,z,false,dist))return blocked();
            }
        }
        // The intersection rail replaces the one horizontal rail with an
        // orthogonal crossing, retaining one physical block.
        if(eastWest&&northSouth) {
            int index=8;
            Rail existing=plan.get(index);
            plan.set(index,new Rail(existing.pos,TrackShape.CR_O,existing.side,existing.ground));
        }
        // Entire route is rejected if a protected building, block entity or
        // player's track occupies its 5-wide clearance. No half-build.
        for(Rail r:plan){
            if(!safeCorridor(level,r.pos,r.side,block))return blocked();
        }
        int count=0,supports=0;
        for(Rail r:plan){
            BlockPos p=r.pos;
            BlockState current=level.getBlockState(p);
            int ground=r.ground,railY=p.getY();
            int delta=railY-ground;
            boolean bridge=delta>4;
            boolean tunnel=delta< -3;
            if(bridge&&!RailwayGenerationConfig.BRIDGES.get())return blocked();
            if(tunnel&&!RailwayGenerationConfig.TUNNELS.get())return blocked();

            // Keep five blocks of lateral/vertical train clearance.
            for(int side=-2;side<=2;side++){
                BlockPos along=p.relative(r.side,side);
                for(int dy=0;dy<=5;dy++){
                    BlockPos clear=along.above(dy);
                    if(clear.equals(p))continue;
                    BlockState old=level.getBlockState(clear);
                    if(replaceableNatural(old))level.setBlock(clear,Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL);
                }
            }

            // Real gravel ballast, with stone shoulders and subtle sleeper
            // pattern. Ground-bound corridors get a two-layer earthwork instead
            // of 40-block-tall viaduct columns.
            for(int side=-2;side<=2;side++){
                BlockPos bed=p.relative(r.side,side).below();
                BlockState wanted=side==0?Blocks.GRAVEL.defaultBlockState():
                    Math.abs(side)==1?Blocks.ANDESITE.defaultBlockState():
                    Blocks.COARSE_DIRT.defaultBlockState();
                if(safe(level,bed))level.setBlock(bed,wanted,Block.UPDATE_ALL);
                if(Math.abs(side)==2&&delta<=3){
                    BlockPos shoulder=bed.below();
                    if(safe(level,shoulder)&&level.getBlockState(shoulder).isAir())
                        level.setBlock(shoulder,Blocks.DIRT.defaultBlockState(),Block.UPDATE_ALL);
                }
            }
            // Short embankment infill only up to four blocks: remaining open
            // gaps become infrequent timber/stone bridge spans over streams.
            if(delta>0&&delta<=4){
                for(int d=2;d<=Math.min(5,delta+1);d++){
                    BlockPos fill=p.below(d);
                    if(safe(level,fill)&&level.getBlockState(fill).isAir())
                        level.setBlock(fill,Blocks.GRAVEL.defaultBlockState(),Block.UPDATE_ALL);
                }
            }
            if(bridge && Math.floorMod((r.side==Direction.SOUTH?p.getX():p.getZ()),12)==0){
                int bottom=Math.max(ground+1,railY-22);
                for(int y=railY-2;y>=bottom;y--){
                    BlockPos pillar=new BlockPos(p.getX(),y,p.getZ());
                    if(!safe(level,pillar))break;
                    level.setBlock(pillar,Blocks.STONE_BRICKS.defaultBlockState(),Block.UPDATE_ALL);
                    supports++;
                }
                for(int side:new int[]{-2,2}){
                    BlockPos fence=p.relative(r.side,side);
                    if(level.getBlockState(fence).isAir())
                        level.setBlock(fence,Blocks.IRON_BARS.defaultBlockState(),Block.UPDATE_ALL);
                }
            }
            if(tunnel){
                for(int side=-3;side<=3;side++){
                    BlockPos top=p.relative(r.side,side).above(6);
                    if(safe(level,top))
                        level.setBlock(top,Blocks.STONE_BRICKS.defaultBlockState(),Block.UPDATE_ALL);
                }
                // Do not obstruct the junction or the parallel station platform.
                boolean nearNode=eastWest&&northSouth&&Math.abs(p.getX()-trackX)<=9&&Math.abs(p.getZ()-trackZ)<=9;
                if(!nearNode)for(int side:new int[]{-3,3}){
                    for(int dy=0;dy<=5;dy++){
                        BlockPos wall=p.relative(r.side,side).above(dy);
                        if(safe(level,wall))
                            level.setBlock(wall,Blocks.STONE_BRICKS.defaultBlockState(),Block.UPDATE_ALL);
                    }
                }
            }
            BlockState placed=block.defaultBlockState().setValue(TrackBlock.SHAPE,r.shape);
            if(current.getBlock()!=block||!current.hasProperty(TrackBlock.SHAPE)||current.getValue(TrackBlock.SHAPE)!=r.shape){
                level.setBlock(p,placed,Block.UPDATE_ALL);count++;
            }
        }
        List<StationRequest> requests=new ArrayList<>();
        // Stations are assembled by a separate service after neighboring
        // chunks are present: never cause implicit loads from a worldgen job.
        if(eastWest&&RailwayGenerationConfig.STATIONS.get()&&
            Math.floorMod(cp.x,spacing)==4){
            int x=trackX,z=trackZ;
            int y=RailwayTerrain.route(level,x,z,dist,true);
            int rx=Math.floorDiv(x,dist),rz=Math.floorDiv(z,dist);
            requests.add(new StationRequest(new BlockPos(x,y,z+5),"Freight Valley "+rx+"-"+rz));
        }
        return new Result(true,count>0,false,count,supports,requests);
    }

    private static Result blocked(){return new Result(true,false,true,0,0,List.of());}

    private static boolean railFor(ServerLevel level,List<Rail> planned,int x,int z,boolean horizontal,int spacing) {
        int y=RailwayTerrain.route(level,x,z,spacing,horizontal);
        int prev=RailwayTerrain.route(level,horizontal?x-1:x,horizontal?z:z-1,spacing,horizontal);
        int next=RailwayTerrain.route(level,horizontal?x+1:x,horizontal?z:z+1,spacing,horizontal);
        if(Math.abs(y-prev)>1||Math.abs(y-next)>1)return false;
        TrackShape shape;
        if(horizontal)shape=(next>y)?TrackShape.AE:(prev>y?TrackShape.AW:TrackShape.XO);
        else shape=(next>y)?TrackShape.AS:(prev>y?TrackShape.AN:TrackShape.ZO);
        int ground=level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x,z)-1;
        planned.add(new Rail(new BlockPos(x,y,z),shape,horizontal?Direction.SOUTH:Direction.EAST,ground));
        return true;
    }
    private static boolean safeCorridor(ServerLevel level,BlockPos pos,Direction side,Block block){
        if(pos.getY()<level.getMinBuildHeight()+8||pos.getY()>level.getMaxBuildHeight()-10)return false;
        BlockState track=level.getBlockState(pos);
        if(track.getBlock()!=block&&!safe(level,pos))return false;
        for(int k=-2;k<=2;k++)for(int dy=1;dy<=5;dy++){
            if(!safe(level,pos.relative(side,k).above(dy)))return false;
        }
        return true;
    }
    static boolean safe(ServerLevel level,BlockPos pos){
        BlockState s=level.getBlockState(pos);
        if(s.isAir())return true;
        if(s.hasBlockEntity()||s.is(Blocks.BEDROCK)||s.is(Blocks.OBSIDIAN)||
            s.is(Blocks.END_PORTAL_FRAME)||s.is(BlockTags.DOORS)||s.is(BlockTags.BEDS)||
            s.is(BlockTags.RAILS)||s.is(BlockTags.SIGNS)||s.is(BlockTags.FENCES)||
            s.is(BlockTags.PLANKS)||s.is(Blocks.CHEST)||s.is(Blocks.BARREL)||
            s.is(Blocks.IRON_BLOCK))return false;
        if(s.getBlock() instanceof TrackBlock)return true;
        return replaceableNatural(s);
    }
    static boolean replaceableNatural(BlockState s){
        if(s.isAir())return true;
        if(s.is(BlockTags.LOGS)||s.is(BlockTags.LEAVES)||s.is(BlockTags.FLOWERS))return true;
        Block b=s.getBlock();
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
