package site.paradoxstudios.freight;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Small deterministic railway structures, safely confined to their host chunk. */
public final class RailwayWaysideBuilder {
    private RailwayWaysideBuilder(){}

    public static int build(ServerLevel level,ChunkPos cp,boolean eastWest,boolean northSouth,int spacing){
        if(!eastWest||northSouth)return 0;
        int along=Math.floorMod(cp.x,spacing);
        int x=cp.getMinBlockX()+8,z=cp.getMinBlockZ()+8;
        int y=RailwayTerrain.route(level,x,z,spacing*16,true);
        int surface=level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x,z+5)-1;
        if(Math.abs(surface-y)>4)return 0;

        // Keep maintenance buildings outside the expanded 81-block station
        // footprint and away from the mainline's flexible curve bodies.
        if(along==7 || along==spacing-6)
            return hut(level,x,y,z);
        if(along==1 || along==spacing-2)
            return signalPost(level,x,y,z);
        if(along==spacing-4)
            return signalTower(level,x,y,z);
        if(along==spacing-3)
            return waterTower(level,x,y,z);
        return 0;
    }
    private static int hut(ServerLevel level,int x,int y,int z){
        // 7x5 masonry maintenance cabin with glazed windows and copper roof.
        for(int dx=-3;dx<=3;dx++)for(int dz=3;dz<=7;dz++)
            for(int dy=-1;dy<=5;dy++){
                BlockPos p=new BlockPos(x+dx,y+dy,z+dz);
                if(!RailwayNetworkGenerator.safe(level,p))return 0;
            }
        for(int dx=-3;dx<=3;dx++)for(int dz=3;dz<=7;dz++){
            place(level,new BlockPos(x+dx,y-1,z+dz),Blocks.STONE_BRICKS.defaultBlockState());
            for(int dy=0;dy<=3;dy++){
                boolean wall=(dx==-3||dx==3||dz==3||dz==7);
                BlockPos p=new BlockPos(x+dx,y+dy,z+dz);
                if(!wall){
                    if(RailwayNetworkGenerator.replaceableNatural(level.getBlockState(p)))
                        place(level,p,Blocks.AIR.defaultBlockState());
                }else{
                    boolean doorway=dx==0&&dz==3&&dy<=2;
                    boolean window=(dy==2 && (Math.abs(dx)==3 || dz==7) && (Math.abs(dx)==1||dz==5));
                    place(level,p,doorway?Blocks.AIR.defaultBlockState():
                        window?Blocks.GLASS_PANE.defaultBlockState():
                        dy==0?Blocks.COBBLED_DEEPSLATE.defaultBlockState():
                            Blocks.SPRUCE_PLANKS.defaultBlockState());
                }
            }
            place(level,new BlockPos(x+dx,y+4,z+dz),
                Math.abs(dx)==3?Blocks.CUT_COPPER.defaultBlockState():
                    Blocks.DARK_OAK_SLAB.defaultBlockState());
        }
        for(int dx:new int[]{-2,2}){
            BlockPos lamp=new BlockPos(x+dx,y+3,z+4);
            place(level,lamp,Blocks.LANTERN.defaultBlockState());
        }
        return 1;
    }
    private static int signalPost(ServerLevel level,int x,int y,int z) {
        BlockPos base=new BlockPos(x,y,z+4);
        for(int dy=-1;dy<=4;dy++)
            if(!RailwayNetworkGenerator.safe(level,base.above(dy)))return 0;
        place(level,base.below(),Blocks.COBBLED_DEEPSLATE.defaultBlockState());
        for(int dy=0;dy<3;dy++)
            place(level,base.above(dy),Blocks.IRON_BARS.defaultBlockState());
        place(level,base.above(3),FreightLogistics.CROSSING.get().defaultBlockState());
        place(level,base.above(4),Blocks.SMOOTH_STONE_SLAB.defaultBlockState());
        return 1;
    }

    /** Two-floor glazed dispatcher tower with balcony and copper roof. */
    private static int signalTower(ServerLevel level,int x,int y,int z){
        for(int dx=-3;dx<=3;dx++)
            for(int dz=3;dz<=7;dz++)
                for(int dy=-1;dy<=9;dy++){
                    BlockPos p=new BlockPos(x+dx,y+dy,z+dz);
                    if(!RailwayNetworkGenerator.safe(level,p))return 0;
                }
        for(int dx=-3;dx<=3;dx++)
            for(int dz=3;dz<=7;dz++){
                place(level,new BlockPos(x+dx,y-1,z+dz),
                    Blocks.POLISHED_ANDESITE.defaultBlockState());
                for(int dy=0;dy<=7;dy++){
                    BlockPos p=new BlockPos(x+dx,y+dy,z+dz);
                    boolean border=Math.abs(dx)==3||dz==3||dz==7;
                    if(!border){
                        place(level,p,dy==3?Blocks.SPRUCE_PLANKS.defaultBlockState():
                            Blocks.AIR.defaultBlockState());
                    }else{
                        boolean corner=Math.abs(dx)==3&&(dz==3||dz==7);
                        boolean window=dy>=4&&dy<=6&&!corner;
                        boolean entrance=dy<=2&&dx==0&&dz==3;
                        place(level,p,entrance?Blocks.AIR.defaultBlockState():
                            window?Blocks.GLASS.defaultBlockState():
                            corner?Blocks.STRIPPED_SPRUCE_LOG.defaultBlockState():
                            dy>=4?Blocks.SPRUCE_PLANKS.defaultBlockState():
                                Blocks.STONE_BRICKS.defaultBlockState());
                    }
                }
                place(level,new BlockPos(x+dx,y+8,z+dz),
                    Math.abs(dx)==3?Blocks.CUT_COPPER.defaultBlockState():
                    Blocks.DARK_OAK_SLAB.defaultBlockState());
            }
        place(level,new BlockPos(x,y+7,z+4),Blocks.SEA_LANTERN.defaultBlockState());
        return 1;
    }

    /** Elevated enclosed copper water tank, supported on iron columns. */
    private static int waterTower(ServerLevel level,int x,int y,int z){
        for(int dx=-2;dx<=2;dx++)
            for(int dz=3;dz<=7;dz++)
                for(int dy=-1;dy<=11;dy++){
                    BlockPos p=new BlockPos(x+dx,y+dy,z+dz);
                    if(!RailwayNetworkGenerator.safe(level,p))return 0;
                }
        for(int dx=-2;dx<=2;dx++)
            for(int dz=3;dz<=7;dz++){
                BlockPos bottom=new BlockPos(x+dx,y-1,z+dz);
                place(level,bottom,Blocks.STONE_BRICKS.defaultBlockState());
                boolean corner=Math.abs(dx)==2&&(dz==3||dz==7);
                if(corner)for(int dy=0;dy<=6;dy++)
                    place(level,new BlockPos(x+dx,y+dy,z+dz),
                        Blocks.IRON_BLOCK.defaultBlockState());
                for(int dy=7;dy<=10;dy++){
                    boolean rim=Math.abs(dx)==2||dz==3||dz==7;
                    place(level,new BlockPos(x+dx,y+dy,z+dz),
                        rim?Blocks.COPPER_BLOCK.defaultBlockState():
                            Blocks.CUT_COPPER.defaultBlockState());
                }
                place(level,new BlockPos(x+dx,y+11,z+dz),
                    Blocks.CUT_COPPER.defaultBlockState());
            }
        place(level,new BlockPos(x,y+5,z+5),Blocks.LANTERN.defaultBlockState());
        return 1;
    }

    private static void place(ServerLevel level,BlockPos pos,BlockState state){
        if(RailwayNetworkGenerator.safe(level,pos))
            level.setBlock(pos,state,net.minecraft.world.level.block.Block.UPDATE_ALL);
    }
}
