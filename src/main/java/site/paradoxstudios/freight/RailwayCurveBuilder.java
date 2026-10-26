package site.paradoxstudios.freight;

import com.simibubi.create.content.trains.track.BezierConnection;
import com.simibubi.create.content.trains.track.TrackBlock;
import com.simibubi.create.content.trains.track.TrackBlockEntity;
import com.simibubi.create.content.trains.track.TrackMaterial;
import com.simibubi.create.content.trains.track.TrackShape;
import net.createmod.catnip.data.Couple;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Four genuine Create Bezier turnouts so the old X is a connected junction. */
public final class RailwayCurveBuilder {
    private RailwayCurveBuilder(){}
    public static int makeJunction(ServerLevel level,int cx,int cz,int spacing){
        int y=RailwayTerrain.route(level,cx,cz,spacing,true);
        int finished=0;
        for(int east:new int[]{-1,1})for(int south:new int[]{-1,1}){
            BlockPos a=new BlockPos(cx+7*east,y,cz);
            BlockPos b=new BlockPos(cx,y,cz+7*south);
            if(curve(level,a,b,east,south))finished++;
        }
        return finished;
    }

    /**
     * Install the native Create edges spanning each 64-block S-curve.
     * Can be called again when an old chunk loads: no duplicates, and no
     * neighboring chunk is force-loaded by this operation.
     */
    public static int makeAlignmentConnections(ServerLevel level,ChunkPos current){
        int span=RailwayGenerationConfig.GRID_SPACING.get()*16;
        if(!RailwayAlignment.curved(span))return 0;
        int made=0;
        for(boolean eastWest:new boolean[]{true,false}){
            int along=eastWest?current.getMinBlockX():current.getMinBlockZ();
            int cross=eastWest?current.getMinBlockZ():current.getMinBlockX();
            int nearest=RailwayAlignment.line(cross+8,span);
            int section=RailwayAlignment.section(along,span);
            for(int segment=section-1;segment<=section+1;segment++){
                int origin=8+segment*span;
                for(int lane=-1;lane<=1;lane++){
                    int line=nearest+lane*span;
                    for(int[] pair:new int[][]{
                        {RailwayAlignment.FIRST_START,RailwayAlignment.FIRST_END},
                        {RailwayAlignment.SECOND_START,RailwayAlignment.SECOND_END}}){
                        int aAlong=origin+pair[0],bAlong=origin+pair[1];
                        int aCross=line+RailwayAlignment.offsetAt(level.getSeed(),eastWest,aAlong,line,span);
                        int bCross=line+RailwayAlignment.offsetAt(level.getSeed(),eastWest,bAlong,line,span);
                        int ax=eastWest?aAlong:aCross,az=eastWest?aCross:aAlong;
                        int bx=eastWest?bAlong:bCross,bz=eastWest?bCross:bAlong;
                        // Cheap chunk-coordinate rejection BEFORE querying 256
                        // base-height samples twice for the candidate curve.
                        boolean touches=(ax>>4)==current.x&&(az>>4)==current.z ||
                                        (bx>>4)==current.x&&(bz>>4)==current.z;
                        if(!touches)continue;
                        if(!level.getChunkSource().hasChunk(ax>>4,az>>4) ||
                           !level.getChunkSource().hasChunk(bx>>4,bz>>4))continue;
                        BlockPos a=new BlockPos(ax,RailwayTerrain.route(level,ax,az,span,eastWest),az);
                        BlockPos b=new BlockPos(bx,RailwayTerrain.route(level,bx,bz,span,eastWest),bz);
                        if(connectAlignment(level,a,b,eastWest))made++;
                    }
                }
            }
        }
        return made;
    }

    private static boolean connectAlignment(ServerLevel level,BlockPos a,BlockPos b,boolean eastWest){
        if(!level.getChunkSource().hasChunk(a.getX()>>4,a.getZ()>>4) ||
           !level.getChunkSource().hasChunk(b.getX()>>4,b.getZ()>>4))return false;
        Block track=TrackMaterial.ANDESITE.getBlock();
        BlockState firstState=level.getBlockState(a),secondState=level.getBlockState(b);
        TrackShape expected=eastWest?TrackShape.XO:TrackShape.ZO;
        if(firstState.getBlock()!=track||secondState.getBlock()!=track ||
           firstState.getValue(TrackBlock.SHAPE)!=expected ||
           secondState.getValue(TrackBlock.SHAPE)!=expected)return false;

        if(!firstState.getValue(TrackBlock.HAS_BE))
            level.setBlock(a,firstState.setValue(TrackBlock.HAS_BE,true),Block.UPDATE_ALL);
        if(!secondState.getValue(TrackBlock.HAS_BE))
            level.setBlock(b,secondState.setValue(TrackBlock.HAS_BE,true),Block.UPDATE_ALL);
        if(!(level.getBlockEntity(a) instanceof TrackBlockEntity ta) ||
           !(level.getBlockEntity(b) instanceof TrackBlockEntity tb))return false;
        if(ta.getConnections().containsKey(b))return false;

        Vec3 axisA=eastWest?new Vec3(1,0,0):new Vec3(0,0,1);
        Vec3 axisB=axisA.scale(-1);
        TrackBlock rail=(TrackBlock)track;
        Vec3 start=rail.getCurveStart(level,a,level.getBlockState(a),axisA);
        Vec3 finish=rail.getCurveStart(level,b,level.getBlockState(b),axisB);
        BezierConnection connection=new BezierConnection(Couple.create(a,b),
            Couple.create(start,finish),Couple.create(axisA,axisB),
            Couple.create(new Vec3(0,1,0),new Vec3(0,1,0)),
            true,false,TrackMaterial.ANDESITE);
        ta.addConnection(connection);
        tb.addConnection(connection.secondary());
        return true;
    }

    private static boolean curve(ServerLevel level,BlockPos a,BlockPos b,int east,int south){
        Block track=TrackMaterial.ANDESITE.getBlock();
        if(level.getBlockState(a).getBlock()!=track||level.getBlockState(b).getBlock()!=track)return false;
        BlockState sa=level.getBlockState(a),sb=level.getBlockState(b);
        if(sa.getValue(TrackBlock.SHAPE)!=TrackShape.XO ||
           sb.getValue(TrackBlock.SHAPE)!=TrackShape.ZO)return false;
        // Corners use two real Create track block entities + one linked Bezier
        // connection. Fake tracks between them are managed by Create.
        level.setBlock(a,sa.setValue(TrackBlock.HAS_BE,true),Block.UPDATE_ALL);
        level.setBlock(b,sb.setValue(TrackBlock.HAS_BE,true),Block.UPDATE_ALL);
        if(!(level.getBlockEntity(a) instanceof TrackBlockEntity first)||
           !(level.getBlockEntity(b) instanceof TrackBlockEntity second))return false;
        if(first.getConnections().containsKey(b))return true;
        Vec3 axisA=new Vec3(-east,0,0); // outward axis towards middle from eastern/western anchor
        Vec3 axisB=new Vec3(0,0,-south); // points from second endpoint back toward first
        TrackBlock blockA=(TrackBlock)track;
        Vec3 pointA=blockA.getCurveStart(level,a,sa,axisA);
        Vec3 pointB=blockA.getCurveStart(level,b,sb,axisB);
        BezierConnection bezier=new BezierConnection(Couple.create(a,b),
            Couple.create(pointA,pointB),
            Couple.create(axisA,axisB),
            Couple.create(new Vec3(0,1,0),new Vec3(0,1,0)),
            true,false,TrackMaterial.ANDESITE);
        // Gravel beneath the curved rail's render segments; prevents a curve
        // from hanging in empty air on normal landscape.
        int segments=Math.min(bezier.getSegmentCount(),100);
        for(int n=0;n<=segments;n+=2){
            Vec3 location=bezier.getPosition(n/(double)Math.max(1,segments));
            BlockPos floor=BlockPos.containing(location).below();
            if(RailwayNetworkGenerator.safe(level,floor))
                level.setBlock(floor,Blocks.GRAVEL.defaultBlockState(),Block.UPDATE_ALL);
        }
        first.addConnection(bezier);
        second.addConnection(bezier.secondary());
        return true;
    }
}
