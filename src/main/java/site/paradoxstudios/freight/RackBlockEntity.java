package site.paradoxstudios.freight;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

public final class RackBlockEntity extends BlockEntity {
    private static final String[] PARTS={"industrial_cpu","memory_module","storage_drive","network_card","cooling_unit"};
    public final ItemStackHandler parts=new ItemStackHandler(5){
        @Override public boolean isItemValid(int slot,ItemStack stack){
            return slot>=0&&slot<5&&stack.is(FreightLogistics.PARTS.get(PARTS[slot]).get());
        }
        @Override public int getSlotLimit(int slot){return slot==1?8:slot==2?4:slot==0?2:1;}
        @Override protected void onContentsChanged(int slot){setChanged();}
    };
    public RackBlockEntity(BlockPos pos,BlockState state){super(FreightLogistics.RACK_ENTITY.get(),pos,state);}
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider registries){
        super.saveAdditional(tag,registries);
        tag.put("Parts",parts.serializeNBT(registries));
    }
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider registries){
        super.loadAdditional(tag,registries);
        if(tag.contains("Parts"))parts.deserializeNBT(registries,tag.getCompound("Parts"));
    }
    public int cpu(){return parts.getStackInSlot(0).getCount();}
    public int memory(){return parts.getStackInSlot(1).getCount();}
    public int storage(){return parts.getStackInSlot(2).getCount();}
    public int network(){return parts.getStackInSlot(3).getCount();}
    public int cooling(){return parts.getStackInSlot(4).getCount();}
    public int capacity(){return cpu()*4+memory()*2+storage()*2+network()+cooling();}
}
