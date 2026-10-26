package site.paradoxstudios.freight;

import java.util.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Consistent, chunk-independent railway grade.
 * Samples the UNGENERATED world's base height instead of calling Level.getHeight
 * on neighboring unloaded chunks (which would trigger unwanted generation).
 *
 * Lower slope-envelope of the base terrain field:
 *    grade(x,z) = min{ terrain(a,b) + ceil(distance(x,z,a,b)/4) } + 1
 * The max-norm distance bound limits grade to about 1 block per four horizontal
 * blocks except at the finite far-field horizon; train gradients are measured
 * and rejected if the local bound cannot be met.
 */
public final class RailwayTerrain {
    private RailwayTerrain(){}
    private static final int SAMPLE=32, RADIUS=7, MAX_CACHE=24000;
    private static final Map<ServerLevel,LinkedHashMap<Long,Integer>> CACHES=new WeakHashMap<>();
    private static final int MAX_HEIGHT_CACHE=100000;
    private static final Map<ServerLevel,LinkedHashMap<Long,Integer>> HEIGHT_CACHES=new WeakHashMap<>();

    private static Map<Long,Integer> heightCache(ServerLevel level) {
        return HEIGHT_CACHES.computeIfAbsent(level,l->new LinkedHashMap<>(8192,.75f,true){
            @Override protected boolean removeEldestEntry(Map.Entry<Long,Integer> entry){
                return size()>MAX_HEIGHT_CACHE;
            }
        });
    }

    private static int base(ServerLevel level,int x,int z) {
        long key=net.minecraft.core.BlockPos.asLong(x,0,z);
        var cache=CACHES.computeIfAbsent(level,l->new LinkedHashMap<>(8192,.75f,true){
            @Override protected boolean removeEldestEntry(Map.Entry<Long,Integer> entry){
                return size()>MAX_CACHE;
            }
        });
        Integer known=cache.get(key);
        if(known!=null)return known;
        int h=level.getChunkSource().getGenerator().getBaseHeight(x,z,
            Heightmap.Types.WORLD_SURFACE_WG,level,level.getChunkSource().randomState());
        h=Math.max(55,Math.min(210,h));
        cache.put(key,h);
        return h;
    }

    /** World-generator surface without triggering neighboring chunk loads. */
    public static int surface(ServerLevel level,int x,int z){
        return base(level,x,z)-1;
    }

    public static int height(ServerLevel level,int x,int z){
        // Avoid rescanning the same 256-point noise lattice for every
        // neighbouring tangent, block of ballast and returning chunk.
        long cacheKey=net.minecraft.core.BlockPos.asLong(x,0,z);
        Map<Long,Integer> finalHeights=heightCache(level);
        Integer cached=finalHeights.get(cacheKey);
        if(cached!=null)return cached;
        // Every query sees the same lattice points in world space, irrespective
        // of which chunk is being generated.
        int gx=Math.floorDiv(x,SAMPLE),gz=Math.floorDiv(z,SAMPLE);
        int result=Integer.MAX_VALUE;
        for(int ix=-RADIUS;ix<=RADIUS+1;ix++)
            for(int iz=-RADIUS;iz<=RADIUS+1;iz++){
                int xx=(gx+ix)*SAMPLE+SAMPLE/2;
                int zz=(gz+iz)*SAMPLE+SAMPLE/2;
                int dist=Math.max(Math.abs(x-xx),Math.abs(z-zz));
                int candidate=base(level,xx,zz)+(dist+3)/4;
                if(candidate<result)result=candidate;
            }
        int graded=Math.max(level.getMinBuildHeight()+8,
            Math.min(level.getMaxBuildHeight()-12,result+1));
        finalHeights.put(cacheKey,graded);
        return graded;
    }

    /** Flatten the last eight blocks around each large railway interchange. */
    public static int route(ServerLevel level,int x,int z,int lineSpacing,boolean horizontal){
        int nodeX=8+Math.floorDiv(x-8+lineSpacing/2,lineSpacing)*lineSpacing;
        int nodeZ=8+Math.floorDiv(z-8+lineSpacing/2,lineSpacing)*lineSpacing;
        int distance=horizontal?Math.abs(x-nodeX):Math.abs(z-nodeZ);
        int joint=height(level,nodeX,nodeZ);
        // A ten-block tangent on both sides of each turnout stays perfectly
        // horizontal. Beyond that, the grade's allowed departure from the
        // junction increases by one block every five horizontal blocks.
        // Unlike a finite blend, this never abruptly snaps to the natural
        // terrain height when a hillside lies just outside the corner.
        if(distance<=10)return joint;
        int natural=height(level,x,z);
        int allowed=(distance-10)/5;
        return Math.max(joint-allowed,Math.min(joint+allowed,natural));
    }
}
