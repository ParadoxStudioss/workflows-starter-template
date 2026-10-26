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
    private record Pending(ResourceKey<Level> dimension,ChunkPos pos,int attempts){}
    private record StationPending(ResourceKey<Level> dimension,BlockPos pos,String name,int attempts){}
    private static final ConcurrentLinkedQueue<Pending> PENDING=new ConcurrentLinkedQueue<>();
    private static final ArrayDeque<StationPending> STATIONS=new ArrayDeque<>();
    private static final Set<String> QUEUED=Collections.synchronizedSet(new HashSet<>());
    public static int builtChunks,blockedChunks,builtRails,builtStations,bridgeSegments,turnoutCurves;

    @SubscribeEvent public static void chunkLoaded(ChunkEvent.Load event){
        if(!event.isNewChunk() || !RailwayGenerationConfig.ENABLED.get())return;
        if(!(event.getLevel() instanceof ServerLevel level) || level.dimension()!=Level.OVERWORLD)return;
        ChunkPos chunk=event.getChunk().getPos();
        if(!RailwayNetworkGenerator.relevant(chunk))return;
        String key=level.dimension().location()+":"+chunk.toLong();
        // Bounded to prevent massive background pre-generation from filling RAM.
        if(PENDING.size()>2048 || !QUEUED.add(key))return;
        PENDING.add(new Pending(level.dimension(),chunk,0));
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
                if(job.attempts()<100)PENDING.add(new Pending(job.dimension(),job.pos(),job.attempts()+1));
                else QUEUED.remove(key);
                continue;
            }
            try {
                LevelChunk chunk=level.getChunk(job.pos().x,job.pos().z);
                var result=RailwayNetworkGenerator.generate(level,chunk);
                if(result.built()){
                    builtChunks++;builtRails+=result.tracks();bridgeSegments+=result.supports();
                    if(Math.floorMod(job.pos().x,RailwayGenerationConfig.GRID_SPACING.get())==0 &&
                        Math.floorMod(job.pos().z,RailwayGenerationConfig.GRID_SPACING.get())==0){
                        turnoutCurves+=RailwayCurveBuilder.makeJunction(level,
                            job.pos().getMinBlockX()+8,job.pos().getMinBlockZ()+8,
                            RailwayGenerationConfig.GRID_SPACING.get()*16);
                    }
                    FreightLogistics.LOGGER.info("Generated railway segment in chunk {}: {} Create rails, {} supports, {} station(s)",
                        job.pos(),result.tracks(),result.supports(),result.stations().size());
                }
                if(result.blocked())blockedChunks++;
                for(var s:result.stations())
                    STATIONS.add(new StationPending(job.dimension(),s.stationPos(),s.name(),0));
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
            if(RailwayStationBuilder.build(level,
                new RailwayNetworkGenerator.StationRequest(station.pos(),station.name()))
                && level.getBlockEntity(station.pos()) instanceof StationBlockEntity be){
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
