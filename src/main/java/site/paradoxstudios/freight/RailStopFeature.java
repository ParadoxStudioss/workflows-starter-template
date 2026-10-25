package site.paradoxstudios.freight;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public final class RailStopFeature extends Feature<NoneFeatureConfiguration> {
    public RailStopFeature(Codec<NoneFeatureConfiguration> codec){super(codec);}
    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context){
        WorldGenLevel level=context.level();
        BlockPos origin=context.origin();
        int x=(origin.getX()&~15)+8,z=(origin.getZ()&~15)+8;
        int ground=level.getHeight(Heightmap.Types.WORLD_SURFACE_WG,x,z);
        if(ground<65||ground>155)return false;
        // Respect water, uneven ground and preexisting solid structures.
        for(int dz=-5;dz<=5;dz++){
            int h=level.getHeight(Heightmap.Types.WORLD_SURFACE_WG,x,z+dz);
            if(Math.abs(h-ground)>1)return false;
            BlockPos top=new BlockPos(x,ground,z+dz);
            if(!level.getFluidState(top.below()).isEmpty())return false;
        }
        return placeTrackStop(level,new BlockPos(x,ground,z),9,true);
    }
    public static boolean placeTrackStop(LevelAccessor level,BlockPos center,int length,boolean station){
        Block createTrack=BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("create","track"));
        if(createTrack==Blocks.AIR)return false;
        BlockState track=createTrack.defaultBlockState();
        int half=length/2;
        for(int n=-half;n<=half;n++){
            BlockPos trackPos=center.offset(0,0,n);
            if(!level.getBlockState(trackPos).canBeReplaced()&&level.getBlockState(trackPos).getBlock()!=createTrack)return false;
        }
        for(int n=-half;n<=half;n++){
            BlockPos rail=center.offset(0,0,n);
            level.setBlock(rail.below(),Blocks.GRAVEL.defaultBlockState(),3);
            level.setBlock(rail,track,3);
            if(station&&n>-half+1&&n<half-1){
                // A small side platform and trackside station equipment.
                level.setBlock(rail.offset(2,-1,0),Blocks.STONE_BRICKS.defaultBlockState(),3);
                level.setBlock(rail.offset(2,0,0),Blocks.SMOOTH_STONE_SLAB.defaultBlockState(),3);
            }
        }
        if(station){
            BlockPos p=center.offset(3,0,0);
            if(level.getBlockState(p).canBeReplaced())level.setBlock(p,FreightLogistics.MACHINES.get("freight_terminal").get().defaultBlockState(),3);
        }
        return true;
    }
}
