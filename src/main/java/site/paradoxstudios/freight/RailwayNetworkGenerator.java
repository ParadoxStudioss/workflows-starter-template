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
        int x=Math.floorMod(chunk.x,s),z=Math.floorMod(chunk.z,s);
        if(!RailwayAlignment.curved(s*16))return x==0||z==0;
        // The mainline can enter either adjoining chunk while meandering.
        return x==0||x==1||x==s-1||z==0||z==1||z==s-1;
    }
    private record Rail(BlockPos pos,TrackShape shape,Direction side,int ground){}

    public static Result generate(ServerLevel level,LevelChunk chunk) {
        ChunkPos cp=chunk.getPos();
        int spacing=RailwayGenerationConfig.GRID_SPACING.get();
        int dist=spacing*16;
        boolean eastWest=Math.floorMod(cp.z,spacing)==0;
        boolean northSouth=Math.floorMod(cp.x,spacing)==0;
        if(!relevant(cp))return new Result(false,false,false,0,0,List.of());
        int startX=cp.getMinBlockX(),startZ=cp.getMinBlockZ();
        int trackX=startX+8,trackZ=startZ+8;
        Block block=TrackMaterial.ANDESITE.getBlock();
        Map<BlockPos,Rail> plan=new LinkedHashMap<>();
        long seed=level.getSeed();

        // Both axes use the same globally derived alignment. A given chunk
        // contributes whichever tangent happens to pass through its footprint;
        // the 64-block meanders themselves are native Create Bezier tracks.
        int baseZ=RailwayAlignment.line(startZ+8,dist);
        for(int lane=-1;lane<=1;lane++){
            int zLine=baseZ+lane*dist;
            for(int i=0;i<16;i++){
                int x=startX+i;
                if(!RailwayAlignment.isTrackBlock(x,dist))continue;
                int z=zLine+RailwayAlignment.offsetAt(seed,true,x,zLine,dist);
                if(Math.floorDiv(z,16)!=cp.z)continue;
                if(!railFor(level,plan,x,z,true,dist))return blocked();
            }
        }
        int baseX=RailwayAlignment.line(startX+8,dist);
        for(int lane=-1;lane<=1;lane++){
            int xLine=baseX+lane*dist;
            for(int i=0;i<16;i++){
                int z=startZ+i;
                if(!RailwayAlignment.isTrackBlock(z,dist))continue;
                int x=xLine+RailwayAlignment.offsetAt(seed,false,z,xLine,dist);
                if(Math.floorDiv(x,16)!=cp.x)continue;
                if(!railFor(level,plan,x,z,false,dist))return blocked();
            }
        }
        // Entire route is rejected if a protected building, block entity or
        // player's track occupies its 5-wide clearance. No half-build.
        for(Rail r:plan.values()){
            if(!safeCorridor(level,r.pos,r.side,block,r.shape))return blocked();
        }
        int count=0,supports=0;
        for(Rail r:plan.values()){
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
                    if(!withinChunk(cp,clear))continue;
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
                if(withinChunk(cp,bed)&&safe(level,bed))
                    level.setBlock(bed,wanted,Block.UPDATE_ALL);
                if(Math.abs(side)==2&&delta<=3){
                    BlockPos shoulder=bed.below();
                    if(withinChunk(cp,shoulder)&&safe(level,shoulder)&&level.getBlockState(shoulder).isAir())
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
                    if(withinChunk(cp,fence)&&level.getBlockState(fence).isAir())
                        level.setBlock(fence,Blocks.IRON_BARS.defaultBlockState(),Block.UPDATE_ALL);
                }
            }
            if(tunnel){
                for(int side=-3;side<=3;side++){
                    BlockPos top=p.relative(r.side,side).above(6);
                    if(withinChunk(cp,top)&&safe(level,top))
                        level.setBlock(top,Blocks.STONE_BRICKS.defaultBlockState(),Block.UPDATE_ALL);
                }
                // Do not obstruct the junction or the parallel station platform.
                boolean nearNode=eastWest&&northSouth&&Math.abs(p.getX()-trackX)<=9&&Math.abs(p.getZ()-trackZ)<=9;
                if(!nearNode)for(int side:new int[]{-3,3}){
                    for(int dy=0;dy<=5;dy++){
                        BlockPos wall=p.relative(r.side,side).above(dy);
                        if(withinChunk(cp,wall)&&safe(level,wall))
                            level.setBlock(wall,Blocks.STONE_BRICKS.defaultBlockState(),Block.UPDATE_ALL);
                    }
                }
            }
            BlockState placed=block.defaultBlockState().setValue(TrackBlock.SHAPE,r.shape);
            if(current.getBlock()!=block||!current.hasProperty(TrackBlock.SHAPE)||current.getValue(TrackBlock.SHAPE)!=r.shape){
                level.setBlock(p,placed,Block.UPDATE_ALL);count++;
            }
        }
        // Build only within the current chunk; never implicitly load a neighbor.
        paveCurvedBeds(level,cp,dist);
        RailwayWaysideBuilder.build(level,cp,eastWest,northSouth,spacing);
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

    private static boolean withinChunk(ChunkPos cp,BlockPos pos){
        return (pos.getX()>>4)==cp.x && (pos.getZ()>>4)==cp.z;
    }

    private static Result blocked(){return new Result(true,false,true,0,0,List.of());}

    private static boolean railFor(ServerLevel level,Map<BlockPos,Rail> planned,int x,int z,boolean horizontal,int spacing) {
        int y=RailwayTerrain.route(level,x,z,spacing,horizontal);
        int along=horizontal?x:z;
        int p=RailwayAlignment.phase(along,spacing);
        boolean anchor=RailwayAlignment.curved(spacing) &&
            (p==RailwayAlignment.FIRST_START || p==RailwayAlignment.FIRST_END ||
             p==RailwayAlignment.SECOND_START || p==RailwayAlignment.SECOND_END);
        int prev=RailwayTerrain.route(level,horizontal?x-1:x,horizontal?z:z-1,spacing,horizontal);
        int next=RailwayTerrain.route(level,horizontal?x+1:x,horizontal?z:z+1,spacing,horizontal);
        if(!anchor&&(Math.abs(y-prev)>1||Math.abs(y-next)>1))return false;
        TrackShape shape;
        if(anchor)shape=horizontal?TrackShape.XO:TrackShape.ZO;
        else if(horizontal)shape=(next>y)?TrackShape.AE:(prev>y?TrackShape.AW:TrackShape.XO);
        else shape=(next>y)?TrackShape.AS:(prev>y?TrackShape.AN:TrackShape.ZO);
        int ground=level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x,z)-1;
        BlockPos point=new BlockPos(x,y,z);
        Rail old=planned.get(point);
        if(old!=null){
            // The two tangents meet at the same node; do not replace one
            // line with another or create two overlapping track blocks.
            if(old.shape!=shape &&
               ((old.shape==TrackShape.XO&&shape==TrackShape.ZO)||
                (old.shape==TrackShape.ZO&&shape==TrackShape.XO))){
                planned.put(point,new Rail(point,TrackShape.CR_O,old.side,ground));
                return true;
            }
            return old.shape==shape || old.shape==TrackShape.CR_O;
        }
        planned.put(point,new Rail(point,shape,
            horizontal?Direction.SOUTH:Direction.EAST,ground));
        return true;
    }

    /**
     * Ballast under native Bezier arcs is prepared by every intersecting chunk.
     * The actual Create connections are installed by RailwayCurveBuilder once
     * both endpoint chunks have generated and loaded.
     */
    private static void paveCurvedBeds(ServerLevel level,ChunkPos cp,int span){
        if(!RailwayAlignment.curved(span))return;
        long seed=level.getSeed();
        int bx=RailwayAlignment.line(cp.getMinBlockX()+8,span);
        int bz=RailwayAlignment.line(cp.getMinBlockZ()+8,span);
        for(boolean horizontal:new boolean[]{true,false}){
            int start=horizontal?cp.getMinBlockX():cp.getMinBlockZ();
            int nearestLine=horizontal?bz:bx;
            for(int lane=-1;lane<=1;lane++){
                int line=nearestLine+lane*span;
                for(int i=0;i<16;i++){
                    int along=start+i;
                    if(!RailwayAlignment.onCurve(along,span))continue;
                    int sideways=RailwayAlignment.lateralAt(seed,horizontal,along,line,span);
                    int x=horizontal?along:sideways,z=horizontal?sideways:along;
                    if(Math.floorDiv(x,16)!=cp.x||Math.floorDiv(z,16)!=cp.z)continue;
                    int[] range=RailwayAlignment.curveRange(along,span);
                    if(range==null)continue;
                    int p0=range[0],p1=range[1];
                    int lateral0=RailwayAlignment.lateralAt(seed,horizontal,p0,line,span);
                    int lateral1=RailwayAlignment.lateralAt(seed,horizontal,p1,line,span);
                    int y0=RailwayTerrain.route(level,horizontal?p0:lateral0,
                        horizontal?lateral0:p0,span,horizontal);
                    int y1=RailwayTerrain.route(level,horizontal?p1:lateral1,
                        horizontal?lateral1:p1,span,horizontal);
                    double t=(along-p0)/(double)(p1-p0);
                    int y=(int)Math.round(y0+(y1-y0)*t);
                    BlockPos center=new BlockPos(x,y,z);
                    int ground=level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x,z)-1;
                    Direction side=horizontal?Direction.SOUTH:Direction.EAST;
                    for(int k=-2;k<=2;k++){
                        BlockPos bed=center.relative(side,k).below();
                        if(Math.floorDiv(bed.getX(),16)!=cp.x ||
                           Math.floorDiv(bed.getZ(),16)!=cp.z)continue;
                        if(safe(level,bed)){
                            BlockState material=k==0?Blocks.GRAVEL.defaultBlockState():
                                Math.abs(k)==1?Blocks.ANDESITE.defaultBlockState():
                                    Blocks.COARSE_DIRT.defaultBlockState();
                            level.setBlock(bed,material,Block.UPDATE_ALL);
                        }
                        for(int dy=0;dy<=5;dy++){
                            BlockPos clearance=center.relative(side,k).above(dy);
                            if(RailwayNetworkGenerator.replaceableNatural(level.getBlockState(clearance)))
                                level.setBlock(clearance,Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL);
                        }
                    }
                    if(y-ground>=2 && y-ground<=5){
                        for(int d=2;d<=Math.min(6,y-ground+1);d++){
                            BlockPos fill=center.below(d);
                            if(safe(level,fill)&&level.getBlockState(fill).isAir())
                                level.setBlock(fill,Blocks.GRAVEL.defaultBlockState(),Block.UPDATE_ALL);
                        }
                    }
                    if(y-ground>5 && along%12==0){
                        for(int yy=y-2;yy>Math.max(ground,y-24);yy--){
                            BlockPos pillar=new BlockPos(x,yy,z);
                            if(!safe(level,pillar))break;
                            level.setBlock(pillar,Blocks.STONE_BRICKS.defaultBlockState(),Block.UPDATE_ALL);
                        }
                    }
                }
            }
        }
    }
    private static boolean safeCorridor(ServerLevel level,BlockPos pos,Direction side,Block block,TrackShape wanted){
        if(pos.getY()<level.getMinBuildHeight()+8||pos.getY()>level.getMaxBuildHeight()-10)return false;
        BlockState track=level.getBlockState(pos);
        if(track.getBlock()==block){
            if(!track.hasProperty(TrackBlock.SHAPE)||track.getValue(TrackBlock.SHAPE)!=wanted)
                return false;
        } else if(!safe(level,pos))return false;
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
        // Existing Create rails are not natural terrain. Do not repurpose
        // player-built tracks as a ballast foundation or tunnel wall.
        if(s.getBlock() instanceof TrackBlock)return false;
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
