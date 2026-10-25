package site.paradoxstudios.freight;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;

public final class ManagementMenu extends AbstractContainerMenu {
    public static final String[] VIEWS={
        "management_computer","railway_shop","freight_terminal","depot_controller",
        "factory_controller","warehouse_controller","display_relay","signal_controller"};
    private final ContainerData data;
    private final ServerPlayer owner;
    private final BlockPos source;
    private int currentPage=1;

    public ManagementMenu(int id,Inventory inventory){
        this(id,inventory,null,null,BlockPos.ZERO);
    }
    public ManagementMenu(int id,Inventory inventory,String blockId,ServerPlayer player,BlockPos source){
        super(FreightLogistics.MANAGEMENT_MENU.get(),id);
        this.owner=player;this.source=source;
        int view=0;
        for(int i=0;i<VIEWS.length;i++)if(VIEWS[i].equals(blockId))view=i;
        final int fixedView=view;
        this.data=player==null?new SimpleContainerData(25):new ContainerData(){
            @Override public int get(int i){
                FreightData storage=FreightData.get(owner.getServer());
                FreightData.Company company=storage.company(owner.getUUID());
                if(i==0)return fixedView;
                if(i==1)return company==null?0:company.balance;
                if(i==2)return currentPage;
                if(i==3)return (int)storage.contracts.values().stream().filter(o->!o.completed).count();
                if(i==4)return company==null?0:1;
                if(i==5)return company!=null&&company.depot!=0?1:0;
                if(i>=6&&i<22){
                    int row=(i-6)/4, field=(i-6)%4;
                    var entries=new ArrayList<>(storage.contracts.values());
                    entries.sort(Comparator.comparingInt(c->c.id));
                    int index=(currentPage-1)*4+row;
                    if(index>=entries.size())return 0;
                    FreightData.Contract c=entries.get(index);
                    return switch(field){
                        case 0->c.id;
                        case 1->BuiltInRegistries.ITEM.getId(BuiltInRegistries.ITEM.get(ResourceLocation.parse(c.item)));
                        case 2->c.count;
                        default->c.completed?-2:c.assignedTo==null?c.reward:-1;
                    };
                }
                return 0;
            }
            @Override public void set(int i,int value){if(i==2)currentPage=Math.max(1,Math.min(10,value));}
            @Override public int getCount(){return 25;}
        };
        addDataSlots(this.data);
    }
    public int val(int i){return data.get(i);}
    public int view(){return val(0);}
    @Override public boolean stillValid(Player player){
        return owner==null||player.distanceToSqr(source.getX()+0.5,source.getY()+0.5,source.getZ()+0.5)<=64;
    }
    @Override public ItemStack quickMoveStack(Player player,int slot){return ItemStack.EMPTY;}
    @Override public boolean clickMenuButton(Player player,int buttonId){
        if(owner==null)return true;
        if(buttonId==10)currentPage=Math.max(1,currentPage-1);
        else if(buttonId==11)currentPage=Math.min(10,currentPage+1);
        else if(buttonId>=20&&buttonId<24){
            int index=(currentPage-1)*4+(buttonId-20);
            var entries=new ArrayList<>(FreightData.get(owner.getServer()).contracts.values());
            entries.sort(Comparator.comparingInt(c->c.id));
            if(index<entries.size())FreightCommands.accept(owner.createCommandSourceStack(),entries.get(index).id);
        } else if(buttonId==24){
            FreightCommands.deliver(owner.createCommandSourceStack());
        } else if(buttonId==25){
            FreightCommands.depot(owner.createCommandSourceStack());
        } else if(buttonId>=30&&buttonId<=36){
            String[] sale={"industrial_cpu","memory_module","storage_drive","network_card","cooling_unit",
            "freight_manifest","train_purchase_contract"};
            FreightCommands.buy(owner.createCommandSourceStack(),sale[buttonId-30]);
        }
        broadcastChanges();
        return true;
    }
}
