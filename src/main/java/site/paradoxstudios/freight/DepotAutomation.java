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
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Server-authoritative automatic return-to-depot using Create's own schedule runtime. */
public final class DepotAutomation {
    private DepotAutomation(){}

    public static GlobalStation nearbyStation(ServerLevel level,BlockPos center,int radius) {
        GlobalStation nearest=null;
        double best=Double.MAX_VALUE;
        for(BlockPos pos:BlockPos.betweenClosed(center.offset(-radius,-5,-radius),center.offset(radius,5,radius))) {
            if(!level.hasChunkAt(pos))continue;
            if(!(level.getBlockEntity(pos) instanceof StationBlockEntity be))continue;
            GlobalStation station=be.getStation();
            double distance=pos.distSqr(center);
            if(station!=null && distance<best) {nearest=station;best=distance;}
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
    @SubscribeEvent public static void tick(ServerTickEvent.Post e){
        var server=e.getServer();
        if(server.getTickCount()%100!=0)return;
        FreightData ledger=FreightData.get(server);
        for(Train train:Create.RAILWAYS.trains.values()){
            if(train.owner==null || train.derailed || train.graph==null || train.runtime==null)continue;
            FreightData.Company company=ledger.company(train.owner);
            if(company==null || !company.autoParking || company.depotStation.isBlank())continue;
            if(train.navigation.destination!=null || train.manualTick || Math.abs(train.speed)>0.001)continue;
            if(train.runtime.getSchedule()!=null&&!train.runtime.completed)continue;
            if(!train.hasForwardConductor()&&!train.hasBackwardConductor())continue;
            GlobalStation present=train.getCurrentStation();
            if(present!=null && company.depotStation.equals(present.name))continue;
            boolean reachable=false;
            for(GlobalStation station:train.graph.getPoints(EdgePointType.STATION)){
                if(company.depotStation.equals(station.name)){reachable=true;break;}
            }
            if(!reachable)continue;
            DestinationInstruction dest=new DestinationInstruction();
            dest.getData().putString("Text",company.depotStation);
            Schedule schedule=new Schedule(List.of(new ScheduleEntry(dest,List.of(List.of(new ScheduledDelay())))),false,0);
            train.runtime.setSchedule(schedule,true);
        }
    }
}
