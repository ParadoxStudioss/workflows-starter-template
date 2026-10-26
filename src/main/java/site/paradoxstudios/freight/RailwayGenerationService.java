package site.paradoxstudios.freight;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import com.simibubi.create.content.trains.station.StationBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * NEW-CHUNKS ONLY. ChunkEvent.Load is too early to manipulate a LevelChunk;
 * defer work to a later server tick after it reaches FULL.
 */
public final class RailwayGenerationService {
    private RailwayGenerationService(){}
    private record Pending(ResourceKey<Level> dimension,ChunkPos pos,int attempts,boolean generate){}
    private record StationPending(ResourceKey<Level> dimension,BlockPos pos,String name,int attempts){}
    private static final ConcurrentLinkedQueue<Pending> PENDING=new ConcurrentLinkedQueue<>();
    private static final ArrayDeque<StationPending> STATIONS=new ArrayDeque<>();
    private static final Set<String> QUEUED=Collections.synchronizedSet(new HashSet<>());
    public static int builtChunks,blockedChunks,builtRails,builtStations,bridgeSegments,turnoutCurves;

    @SubscribeEvent public static void chunkLoaded(ChunkEvent.Load event){
        if(!RailwayGenerationConfig.ENABLED.get())return;
        if(!(event.getLevel() instanceof ServerLevel level) || level.dimension()!=Level.OVERWORLD)return;
        ChunkPos chunk=event.getChunk().getPos();
        if(!RailwayNetworkGenerator.relevant(chunk))return;
        String key=level.dimension().location()+":"+chunk.toLong();
        // Bounded to prevent massive background pre-generation from filling RAM.
        if(PENDING.size()>2048 || !QUEUED.add(key))return;
        PENDING.add(new Pending(level.dimension(),chunk,0,event.isNewChunk()));
    }
    /** Explicit operator repair of already-loaded rail-bearing chunks only. */
    public static boolean queueRepair(ServerLevel level,ChunkPos chunk){
        if(level.dimension()!=Level.OVERWORLD || !RailwayGenerationConfig.ENABLED.get() ||
            !RailwayNetworkGenerator.relevant(chunk) ||
            !level.getChunkSource().hasChunk(chunk.x,chunk.z))return false;
        String key=level.dimension().location()+":"+chunk.toLong();
        if(PENDING.size()>2048||!QUEUED.add(key))return false;
        PENDING.add(new Pending(level.dimension(),chunk,0,true));
        return true;
    }

    public static void enqueueStation(ServerLevel level,RailwayNetworkGenerator.StationRequest station){
        STATIONS.add(new StationPending(level.dimension(),station.stationPos(),station.name(),0));
    }
    @SubscribeEvent public static void tick(ServerTickEvent.Post event){
        if(!RailwayGenerationConfig.ENABLED.get())return;
        MinecraftServer server=event.getServer();
        int budget=RailwayGenerationConfig.BUILD_BUDGET.get();
        for(int i=0;i<budget;i++){
            Pending job=PENDING.poll();
            if(job==null)break;
            String key=job.dimension().location()+":"+job.pos().toLong();
            ServerLevel level=server.getLevel(job.dimension());
            if(level==null){QUEUED.remove(key);continue;}
            if(!level.getChunkSource().hasChunk(job.pos().x,job.pos().z)){
                if(job.attempts()<100)PENDING.add(new Pending(job.dimension(),job.pos(),job.attempts()+1,job.generate()));
                else QUEUED.remove(key);
                continue;
            }
            try {
                LevelChunk chunk=level.getChunk(job.pos().x,job.pos().z);
                var result=job.generate()?RailwayNetworkGenerator.generate(level,chunk):
                    new RailwayNetworkGenerator.Result(false,false,false,0,0,List.of());
                if(result.built()){
                    builtChunks++;builtRails+=result.tracks();bridgeSegments+=result.supports();
                    FreightLogistics.LOGGER.info("Generated railway segment in chunk {}: {} Create rails, {} supports, {} station(s)",
                        job.pos(),result.tracks(),result.supports(),result.stations().size());
                }
                if(result.blocked())blockedChunks++;
                for(var s:result.stations())
                    STATIONS.add(new StationPending(job.dimension(),s.stationPos(),s.name(),0));
                // A curve's endpoints may be generated or reloaded in a different
                // order, so retry attachment whenever either endpoint loads.
                turnoutCurves+=RailwayCurveBuilder.makeAlignmentConnections(level,job.pos());
                // A junction is native track geometry, not a one-time decoration.
                // Reconcile its four links when an old hub chunk is loaded too.
                if(Math.floorMod(job.pos().x,RailwayGenerationConfig.GRID_SPACING.get())==0 &&
                   Math.floorMod(job.pos().z,RailwayGenerationConfig.GRID_SPACING.get())==0)
                    turnoutCurves+=RailwayCurveBuilder.makeJunction(level,
                        job.pos().getMinBlockX()+8,job.pos().getMinBlockZ()+8,
                        RailwayGenerationConfig.GRID_SPACING.get()*16);
            }catch(Exception ex){
                FreightLogistics.LOGGER.error("Unable to build railway segment in {}",job.pos(),ex);
                blockedChunks++;
            }finally {
                QUEUED.remove(key);
            }
        }
        if(server.getTickCount()%5!=0)return;
        // Create propagates its track graph on scheduled track ticks. Wait before
        // naming the generated station, so we don't require neighboring chunks.
        for(int i=0,n=Math.min(16,STATIONS.size());i<n;i++){
            StationPending station=STATIONS.poll();
            if(station==null)break;
            ServerLevel level=server.getLevel(station.dimension());
            if(level==null||!level.hasChunkAt(station.pos()))continue;
            var request=new RailwayNetworkGenerator.StationRequest(station.pos(),station.name());
            boolean completed=station.name().endsWith(" Return")
                ?RailwayStationBuilder.buildReturnStation(level,request)
                :RailwayStationBuilder.build(level,request);
            if(completed && level.getBlockEntity(station.pos()) instanceof StationBlockEntity be){
                var global=be.getStation();
                if(global==null && station.attempts()>3)
                    global=be.edgePoint.createEdgePoint();
                if(global!=null){
                    global.name=station.name();
                    be.notifyUpdate();
                    builtStations++;
                    continue;
                }
            }
            if(station.attempts()<800)
                STATIONS.add(new StationPending(station.dimension(),station.pos(),station.name(),station.attempts()+1));
        }
    }
}
