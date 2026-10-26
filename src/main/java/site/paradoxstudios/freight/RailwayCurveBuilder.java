package site.paradoxstudios.freight;

import com.simibubi.create.content.trains.track.BezierConnection;
import com.simibubi.create.content.trains.track.TrackBlock;
import com.simibubi.create.content.trains.track.TrackBlockEntity;
import com.simibubi.create.content.trains.track.TrackMaterial;
import com.simibubi.create.content.trains.track.TrackShape;
import net.createmod.catnip.data.Couple;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
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
