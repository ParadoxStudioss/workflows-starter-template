package site.paradoxstudios.freight;

import java.util.*;
import com.simibubi.create.Create;
import com.simibubi.create.content.trains.entity.Train;
import com.simibubi.create.content.trains.graph.EdgePointType;
import com.simibubi.create.content.trains.schedule.Schedule;
import com.simibubi.create.content.trains.schedule.ScheduleEntry;
import com.simibubi.create.content.trains.schedule.condition.ScheduledDelay;
import com.simibubi.create.content.trains.schedule.destination.DestinationInstruction;
import com.simibubi.create.content.trains.station.GlobalStation;
import com.simibubi.create.content.trains.station.StationBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Return-to-depot scheduling. Create only reverses with a conductor on the
 * trailing controls, and normally changes direction while at a station.
 * Never teleport, reverse a moving train's points, or assume a graph exists.
 */
public final class DepotAutomation {
    private DepotAutomation(){}
    private static final Map<UUID,String> REQUESTED_RETURNS=new HashMap<>();
    public record ReturnReport(int dispatched,int queued,int missingDriver,int atDepot){}

    public static GlobalStation nearbyStation(ServerLevel level,BlockPos center,int radius) {
        GlobalStation nearest=null;
        double best=Double.MAX_VALUE;
        for(BlockPos pos:BlockPos.betweenClosed(center.offset(-radius,-5,-radius),center.offset(radius,5,radius))) {
            if(!level.hasChunkAt(pos))continue;
            if(!(level.getBlockEntity(pos) instanceof StationBlockEntity be))continue;
            GlobalStation station=be.getStation();
            double distance=pos.distSqr(center);
            if(station!=null && distance<best){nearest=station;best=distance;}
        }
        return nearest;
    }

    public static int ownedTrains(UUID owner) {
        int count=0;
        for(Train train:Create.RAILWAYS.trains.values())if(owner.equals(train.owner))count++;
        return count;
    }
    public static int parkedTrains(UUID owner,String depot) {
        int count=0;
        for(Train train:Create.RAILWAYS.trains.values()) {
            GlobalStation st=train.getCurrentStation();
            if(owner.equals(train.owner) && st!=null && st.name.equals(depot))count++;
        }
        return count;
    }
    public static int queuedTrains(UUID owner) {
        int count=0;
        for(Train train:Create.RAILWAYS.trains.values())
            if(owner.equals(train.owner)&&REQUESTED_RETURNS.containsKey(train.id))count++;
        return count;
    }

    /**
     * A request made while a train is moving stays queued until it next stops
     * at a station. Actual direction selection is delegated to Create.
     */
    public static ReturnReport requestReturn(UUID owner,String depot){
        int dispatched=0,queued=0,missing=0,parked=0;
        for(Train train:Create.RAILWAYS.trains.values()){
            if(!owner.equals(train.owner)||train.derailed||train.graph==null||train.runtime==null)
                continue;
            if(!train.hasForwardConductor()||!train.hasBackwardConductor()){
                missing++;continue;
            }
            GlobalStation current=train.getCurrentStation();
            if(current!=null&&current.name.equals(depot)){
                parked++;REQUESTED_RETURNS.remove(train.id);continue;
            }
            if(current!=null && Math.abs(train.speed)<0.001 && !train.manualTick &&
               dispatch(train,depot)) {
                dispatched++;REQUESTED_RETURNS.remove(train.id);
            }else {
                REQUESTED_RETURNS.put(train.id,depot);
                queued++;
            }
        }
        return new ReturnReport(dispatched,queued,missing,parked);
    }

    private static boolean dispatch(Train train,String depot){
        if(train.derailed||train.graph==null||train.runtime==null||train.manualTick ||
           Math.abs(train.speed)>0.001 || train.getCurrentStation()==null ||
           !train.hasForwardConductor()||!train.hasBackwardConductor())
            return false;
        GlobalStation target=null;
        for(GlobalStation station:train.graph.getPoints(EdgePointType.STATION)){
            if(depot.equals(station.name)){
                target=station;break;
            }
        }
        if(target==null)return false;
        if(train.navigation.findPathTo(target,Double.MAX_VALUE)==null)return false;
        DestinationInstruction instruction=new DestinationInstruction();
        instruction.getData().putString("Text",depot);
        Schedule schedule=new Schedule(
            List.of(new ScheduleEntry(instruction,List.of(List.of(new ScheduledDelay())))),false,0);
        train.runtime.setSchedule(schedule,true);
        return true;
    }

    @SubscribeEvent public static void tick(ServerTickEvent.Post e){
        MinecraftServer server=e.getServer();
        if(server.getTickCount()%20==0 && !REQUESTED_RETURNS.isEmpty()){
            Iterator<Map.Entry<UUID,String>> iterator=REQUESTED_RETURNS.entrySet().iterator();
            while(iterator.hasNext()){
                Map.Entry<UUID,String> request=iterator.next();
                Train train=Create.RAILWAYS.trains.get(request.getKey());
                if(train==null||train.derailed){iterator.remove();continue;}
                GlobalStation st=train.getCurrentStation();
                if(st!=null&&(st.name.equals(request.getValue())||
                    dispatch(train,request.getValue())))iterator.remove();
            }
        }
        if(server.getTickCount()%100!=0)return;
        FreightData ledger=FreightData.get(server);
        for(Train train:Create.RAILWAYS.trains.values()){
            if(train.owner==null||train.derailed||train.graph==null||train.runtime==null)
                continue;
            if(REQUESTED_RETURNS.containsKey(train.id))continue;
            FreightData.Company company=ledger.company(train.owner);
            if(company==null||!company.autoParking||company.depotStation.isBlank())continue;
            if(train.navigation.destination!=null||train.manualTick||Math.abs(train.speed)>0.001)
                continue;
            if(train.runtime.getSchedule()!=null&&!train.runtime.completed)continue;
            if(!train.hasForwardConductor()||!train.hasBackwardConductor())continue;
            GlobalStation present=train.getCurrentStation();
            if(present!=null&&company.depotStation.equals(present.name))continue;
            dispatch(train,company.depotStation);
        }
    }
}
