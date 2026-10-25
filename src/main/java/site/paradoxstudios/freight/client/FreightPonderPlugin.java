package site.paradoxstudios.freight.client;

import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.minecraft.resources.ResourceLocation;
import site.paradoxstudios.freight.FreightLogistics;

public final class FreightPonderPlugin implements PonderPlugin {
    @Override public String getModId() {return FreightLogistics.ID;}
    @Override public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        add(helper,"management_computer","management","Company Management",
            "Right click to open the company control dashboard.",
            "View accepted freight orders and control your company.",
            "Contracts consume real items before issuing payment.");
        add(helper,"server_rack","rack","Industrial Servers",
            "Right click to access the component bays.",
            "Install a CPU, RAM, storage drive, network card and cooling.",
            "The parts stay installed when you save and reload the world.");
        add(helper,"depot_controller","depot","Automatic Train Parking",
            "Place a Create Train Station beside your depot controller.",
            "Use the Depot menu to register the nearby station.",
            "Enable automatic parking to return idle Create trains home.");
        add(helper,"warehouse_controller","warehouse","Warehouse Storage",
            "Right click the warehouse to open the cargo inventory.",
            "Store freight materials in its twenty-seven slots.",
            "The items remain stored after the world is saved.");
        add(helper,"railway_shop","market","Equipment Marketplace",
            "Your company earns credits by completing freight contracts.",
            "Spend credits in the Marketplace menu to obtain components.",
            "Place the components inside your Industrial Server Rack.");
        add(helper,"factory_controller","factory","Factory Controller",
            "Install a controller beside your Create factory.",
            "This controller currently opens its status interface.",
            "Automated factory production is an upcoming integration.");
        add(helper,"crossing_signal","crossing","Railway Crossing Signals",
            "Power the crossing signal with redstone to turn it on.",
            "Connect barriers and warning lamps to the same control line.",
            "Train approach detection is not automatic in this version.");
    }
    private static void add(PonderSceneRegistrationHelper<ResourceLocation> helper,
        String item,String id,String title,String a,String b,String c){
        helper.forComponents(ResourceLocation.fromNamespaceAndPath(FreightLogistics.ID,item))
            .addStoryBoard("guides/"+item,(scene,util)->FreightPonderScenes.show(scene,util,id,title,a,b,c));
    }
}
