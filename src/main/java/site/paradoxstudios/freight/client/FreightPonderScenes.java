package site.paradoxstudios.freight.client;

import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.Direction;

public final class FreightPonderScenes {
    private FreightPonderScenes(){}
    public static void show(SceneBuilder scene,SceneBuildingUtil util,
        String id,String title,String intro,String stepTwo,String stepThree){
        scene.title(id,title);
        scene.configureBasePlate(0,0,5);
        scene.showBasePlate();
        scene.idle(15);
        scene.world().showSection(util.select().position(util.grid().at(2,1,2)),Direction.DOWN);
        scene.idle(20);
        scene.overlay().showText(70).pointAt(util.vector().centerOf(util.grid().at(2,1,2)))
            .placeNearTarget().text(intro);
        scene.idle(85);
        scene.overlay().showText(70).pointAt(util.vector().centerOf(util.grid().at(2,1,2)))
            .placeNearTarget().attachKeyFrame().text(stepTwo);
        scene.idle(85);
        scene.overlay().showText(70).pointAt(util.vector().centerOf(util.grid().at(2,1,2)))
            .placeNearTarget().attachKeyFrame().text(stepThree);
        scene.idle(90);
    }
}
