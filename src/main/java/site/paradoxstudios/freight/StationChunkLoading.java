package site.paradoxstudios.freight;

import java.util.*;
import com.simibubi.create.Create;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.station.GlobalStation;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.common.world.chunk.TicketController;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Up to four temporarily ticking 3x3 chunk windows at approached Create stations. */
public final class StationChunkLoading {
    private StationChunkLoading(){}
    public static final TicketController TICKET = new TicketController(
        ResourceLocation.fromNamespaceAndPath(FreightLogistics.ID,"station_transfer"),
        (level,helper)->{
            // Never resurrect old transient tickets after a restart.
            for(BlockPos owner:helper.getBlockTickets().keySet())helper.removeAllTickets(owner);
        });
    private static final Map<ServerLevel,Map<BlockPos,Long>> ACTIVE=new WeakHashMap<>();
    private static final int MAX_STATIONS=4;
    private static final long UNLOAD_AFTER_TICKS=20L*30L;
    @net.neoforged.bus.api.SubscribeEvent public static void tick(ServerTickEvent.Post ev){
        var server=ev.getServer();
        if(server.getTickCount()%20!=0)return;
        long now=server.getTickCount();
        for(ServerLevel level:server.getAllLevels()){
            Map<BlockPos,Long> active=ACTIVE.computeIfAbsent(level,$->new LinkedHashMap<>());
            // expire & release first, so old stations never leave chunks force loaded
            var iterator=active.entrySet().iterator();
            while(iterator.hasNext()){
                var e=iterator.next();
                if(now<=e.getValue())continue;
                set(level,e.getKey(),false);
                iterator.remove();
            }
            if(active.size()>=MAX_STATIONS)continue;
            for(Train train:Create.RAILWAYS.trains.values()){
                GlobalStation station=train.navigation==null?null:train.navigation.destination;
                if(station==null || station.getBlockEntityPos()==null)continue;
                if(!level.dimension().equals(station.getBlockEntityDimension()))continue;
                if(train.navigation.distanceToDestination>100)continue;
                BlockPos pos=station.getBlockEntityPos();
                if(!active.containsKey(pos)&&active.size()>=MAX_STATIONS)break;
                if(!active.containsKey(pos))set(level,pos,true);
                active.put(pos,now+UNLOAD_AFTER_TICKS);
            }
        }
    }
    private static void set(ServerLevel level,BlockPos station,boolean force){
        int cx=station.getX()>>4,cz=station.getZ()>>4;
        for(int dx=-1;dx<=1;dx++)
            for(int dz=-1;dz<=1;dz++)
                TICKET.forceChunk(level,station,cx+dx,cz+dz,force,true);
    }
}
