package site.paradoxstudios.freight;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

/** Real persisted 27-slot physical inventory; no virtual item creation. */
public final class WarehouseBlockEntity extends BlockEntity {
    public final ItemStackHandler inventory=new ItemStackHandler(27) {
        @Override protected void onContentsChanged(int slot) { setChanged(); }
    };
    public WarehouseBlockEntity(BlockPos pos,BlockState state) {
        super(FreightLogistics.WAREHOUSE_ENTITY.get(),pos,state);
    }
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider provider) {
        super.loadAdditional(tag,provider);
        if(tag.contains("Inventory"))inventory.deserializeNBT(provider,tag.getCompound("Inventory"));
    }
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider provider) {
        super.saveAdditional(tag,provider);
        tag.put("Inventory",inventory.serializeNBT(provider));
    }
    public int storedItems() {
        int count=0;
        for(int i=0;i<inventory.getSlots();i++)count+=inventory.getStackInSlot(i).getCount();
        return count;
    }
}
