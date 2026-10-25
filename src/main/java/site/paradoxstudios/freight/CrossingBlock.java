package site.paradoxstudios.freight;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.StateDefinition.Builder;

public final class CrossingBlock extends Block {
    public static final BooleanProperty POWERED=BlockStateProperties.POWERED;
    public CrossingBlock(BlockBehaviour.Properties properties){
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(POWERED,false));
    }
    @Override protected void createBlockStateDefinition(Builder<Block,BlockState> builder){
        builder.add(POWERED);
    }
    @Override protected void neighborChanged(BlockState state,Level level,BlockPos pos,Block fromBlock,BlockPos fromPos,boolean moving){
        if(!level.isClientSide){
            boolean powered=level.hasNeighborSignal(pos);
            if(powered!=state.getValue(POWERED)) level.setBlock(pos,state.setValue(POWERED,powered),3);
        }
    }
}
