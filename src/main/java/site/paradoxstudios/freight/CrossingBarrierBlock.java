package site.paradoxstudios.freight;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class CrossingBarrierBlock extends CrossingBlock {
    private static final VoxelShape POST=box(4,0,4,12,16,12);
    private static final VoxelShape CLOSED=Shapes.or(POST,box(8,10,6,16,13,10));
    public CrossingBarrierBlock(BlockBehaviour.Properties props){super(props);}
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){
        return state.getValue(POWERED)?CLOSED:POST;
    }
    @Override protected VoxelShape getCollisionShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){
        return state.getValue(POWERED)?CLOSED:POST;
    }
}
