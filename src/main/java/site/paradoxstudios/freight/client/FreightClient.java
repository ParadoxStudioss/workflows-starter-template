package site.paradoxstudios.freight.client;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import site.paradoxstudios.freight.FreightLogistics;

@EventBusSubscriber(modid=FreightLogistics.ID,bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class FreightClient {
    @SubscribeEvent public static void ponder(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent e){
        net.createmod.ponder.foundation.PonderIndex.addPlugin(new FreightPonderPlugin());
    }
    @SubscribeEvent public static void menus(RegisterMenuScreensEvent ev){
        ev.register(FreightLogistics.MANAGEMENT_MENU.get(),ManagementScreen::new);
        ev.register(FreightLogistics.RACK_MENU.get(),RackScreen::new);
        ev.register(FreightLogistics.WAREHOUSE_MENU.get(),WarehouseScreen::new);
    }
}
