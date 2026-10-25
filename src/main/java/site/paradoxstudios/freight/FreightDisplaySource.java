package site.paradoxstudios.freight;

import java.util.*;
import com.simibubi.create.api.behaviour.display.DisplaySource;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;
import com.simibubi.create.foundation.gui.ModularGuiLineBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;

/** Actual Create Display Link data source, including per-link pages 1 to 10. */
public final class FreightDisplaySource extends DisplaySource {
    @Override public List<MutableComponent> provideText(DisplayLinkContext ctx,DisplayTargetStats stats){
        if(!(ctx.level() instanceof ServerLevel level))return EMPTY;
        int page=ctx.sourceConfig().getInt("Page");
        if(page<1||page>10)page=1;
        var output=new ArrayList<MutableComponent>();
        var be=ctx.getSourceBlockEntity();
        if(be instanceof WarehouseBlockEntity wh){
            output.add(Component.literal("WAREHOUSE STORAGE"));
            output.add(Component.literal("Items stored: "+wh.storedItems()));
            output.add(Component.literal("Slots: 27"));
        }else if(be instanceof RackBlockEntity rack){
            output.add(Component.literal("INDUSTRIAL SERVER"));
            output.add(Component.literal("CPU "+rack.cpu()+" | RAM "+rack.memory()));
            output.add(Component.literal("SSD "+rack.storage()+" | NET "+rack.network()));
            output.add(Component.literal("COOLING "+rack.cooling()));
        }else{
            FreightData ledger=FreightData.get(level.getServer());
            List<FreightData.Contract> orders=new ArrayList<>(ledger.contracts.values());
            orders.sort(Comparator.comparingInt(o->o.id));
            output.add(Component.literal("FREIGHT ORDERS "+page+"/10"));
            int start=(page-1)*4;
            for(int i=start;i<Math.min(start+4,orders.size());i++){
                var c=orders.get(i);
                String item=c.item.replace("minecraft:","");
                if(item.length()>14)item=item.substring(0,14);
                output.add(Component.literal("#"+c.id+" "+item+" x"+c.count+
                    " "+(c.completed?"DONE":c.assignedTo==null?"OPEN":"BUSY")));
            }
            if(orders.size()<=start)output.add(Component.literal("No orders on this page"));
        }
        return output;
    }
    @Override public int getPassiveRefreshTicks(){return 60;}
    @Override public void initConfigurationWidgets(DisplayLinkContext context,ModularGuiLineBuilder builder,boolean isFirstLine){
        builder.addScrollInput(0,65,(input,label)->{
            input.withRange(1,11).titled(Component.literal("Page 1-10"));
        },"Page");
    }
}
