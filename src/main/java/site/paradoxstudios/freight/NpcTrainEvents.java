package site.paradoxstudios.freight;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Defer graph-sensitive assembly until Create has registered the stations. */
public final class NpcTrainEvents {
    private NpcTrainEvents(){}
    @SubscribeEvent
    public static void tick(ServerTickEvent.Post event){
        NpcTrainManager.tick(event.getServer());
    }
}
