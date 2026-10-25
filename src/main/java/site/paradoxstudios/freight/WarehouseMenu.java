package site.paradoxstudios.freight;

import net.minecraft.core.BlockPos;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

public final class WarehouseMenu extends AbstractContainerMenu {
    private final ContainerLevelAccess access;
    public WarehouseMenu(int id,Inventory inv){this(id,inv,new ItemStackHandler(27),ContainerLevelAccess.NULL);}
    public WarehouseMenu(int id,Inventory inv,WarehouseBlockEntity e){
        this(id,inv,e.inventory,ContainerLevelAccess.create(inv.player.level(),e.getBlockPos()));
    }
    private WarehouseMenu(int id,Inventory inv,IItemHandler items,ContainerLevelAccess access){
        super(FreightLogistics.WAREHOUSE_MENU.get(),id);
        this.access=access;
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)
            addSlot(new SlotItemHandler(items,col+row*9,8+col*18,27+row*18));
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)
            addSlot(new Slot(inv,col+row*9+9,8+col*18,95+row*18));
        for(int i=0;i<9;i++)addSlot(new Slot(inv,i,8+i*18,153));
    }
    @Override public boolean stillValid(Player p){
        return stillValid(access,p,FreightLogistics.MACHINES.get("warehouse_controller").get());
    }
    @Override public ItemStack quickMoveStack(Player player,int idx){
        if(idx<0||idx>=slots.size())return ItemStack.EMPTY;
        Slot slot=slots.get(idx);if(!slot.hasItem())return ItemStack.EMPTY;
        ItemStack stack=slot.getItem(),copy=stack.copy();
        if(idx<27){
            if(!moveItemStackTo(stack,27,63,true))return ItemStack.EMPTY;
        }else if(!moveItemStackTo(stack,0,27,false))return ItemStack.EMPTY;
        if(stack.isEmpty())slot.set(ItemStack.EMPTY);else slot.setChanged();
        return copy;
    }
}
