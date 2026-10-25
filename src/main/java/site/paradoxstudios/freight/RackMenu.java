package site.paradoxstudios.freight;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.minecraft.core.BlockPos;

public final class RackMenu extends AbstractContainerMenu {
    private final net.neoforged.neoforge.items.IItemHandler parts;
    private final ContainerLevelAccess access;
    public RackMenu(int id,Inventory inv){
        this(id,inv,new ItemStackHandler(5),ContainerLevelAccess.NULL);
    }
    public RackMenu(int id,Inventory inv,RackBlockEntity entity){
        this(id,inv,entity.parts,ContainerLevelAccess.create(inv.player.level(),entity.getBlockPos()));
    }
    private RackMenu(int id,Inventory inv,net.neoforged.neoforge.items.IItemHandler parts,ContainerLevelAccess access){
        super(FreightLogistics.RACK_MENU.get(),id);
        this.parts=parts;this.access=access;
        for(int i=0;i<5;i++)addSlot(new SlotItemHandler(parts,i,26+i*30,43));
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)
            addSlot(new Slot(inv,col+row*9+9,8+col*18,89+row*18));
        for(int i=0;i<9;i++)addSlot(new Slot(inv,i,8+i*18,147));
    }
    @Override public boolean stillValid(Player p){
        return stillValid(access,p,FreightLogistics.MACHINES.get("server_rack").get());
    }
    @Override public ItemStack quickMoveStack(Player player,int index){
        if(index<0||index>=slots.size())return ItemStack.EMPTY;
        Slot slot=slots.get(index);if(!slot.hasItem())return ItemStack.EMPTY;
        ItemStack stack=slot.getItem(),copy=stack.copy();
        if(index<5){
            if(!moveItemStackTo(stack,5,41,true))return ItemStack.EMPTY;
        }else{
            if(!moveItemStackTo(stack,0,5,false))return ItemStack.EMPTY;
        }
        if(stack.isEmpty())slot.set(ItemStack.EMPTY);else slot.setChanged();
        return copy;
    }
}
