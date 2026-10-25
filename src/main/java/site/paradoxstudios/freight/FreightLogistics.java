package site.paradoxstudios.freight;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import java.util.LinkedHashMap;
import java.util.Map;

@Mod(FreightLogistics.ID)
public final class FreightLogistics {
    public static final String ID="freightlogistics";
    public static final DeferredRegister.Blocks BLOCKS=DeferredRegister.createBlocks(ID);
    public static final DeferredRegister.Items ITEMS=DeferredRegister.createItems(ID);
    public static final DeferredRegister<CreativeModeTab> TABS=DeferredRegister.create(Registries.CREATIVE_MODE_TAB,ID);
    public static final Map<String, DeferredBlock<Block>> MACHINES=new LinkedHashMap<>();
    public static final Map<String, DeferredItem<Item>> PARTS=new LinkedHashMap<>();

    static {
        machine("management_computer","Railway Management Computer");
        machine("server_rack","Industrial Server Rack");
        machine("factory_controller","Factory Controller");
        machine("warehouse_controller","Warehouse Controller");
        machine("depot_controller","Depot Controller");
        machine("freight_terminal","Freight Terminal");
        machine("display_relay","Display Relay");
        machine("signal_controller","Railway Signal Controller");
        machine("railway_shop","Railway Marketplace Terminal");
        part("freight_manifest");
        part("train_purchase_contract");
        part("industrial_cpu");
        part("memory_module");
        part("storage_drive");
        part("network_card");
        part("cooling_unit");
        part("railway_credit");
    }
    public static final DeferredBlock<Block> CROSSING = BLOCKS.registerBlock(
        "crossing_signal", CrossingBlock::new,
        BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2.0f).lightLevel(s->s.getValue(CrossingBlock.POWERED)?12:0));
    public static final DeferredItem<net.minecraft.world.item.BlockItem> CROSSING_ITEM = ITEMS.registerSimpleBlockItem("crossing_signal",CROSSING);
    private static void machine(String id,String displayName) {
        DeferredBlock<Block> block=BLOCKS.registerBlock(id, properties->new MachineBlock(properties,displayName),
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2.4f));
        MACHINES.put(id,block);
        ITEMS.registerSimpleBlockItem(id,block);
    }
    private static void part(String id) {
        PARTS.put(id,ITEMS.registerSimpleItem(id,new Item.Properties()));
    }
    public static final DeferredHolder<CreativeModeTab,CreativeModeTab> TAB=TABS.register("main",()->
        CreativeModeTab.builder().title(Component.literal("Freight & Logistics"))
        .withTabsBefore(CreativeModeTabs.REDSTONE_BLOCKS)
        .icon(()->new net.minecraft.world.item.ItemStack(MACHINES.get("management_computer").get()))
        .displayItems((params,output)->{
            for(var machine:MACHINES.values()) output.accept(machine.get());
            output.accept(CROSSING.get());
            for(var part:PARTS.values()) output.accept(part.get());
        }).build());
    public FreightLogistics(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        TABS.register(bus);
        NeoForge.EVENT_BUS.register(FreightCommands.class);
    }
    public static class MachineBlock extends Block {
        private final String title;
        public MachineBlock(Properties props,String title){super(props);this.title=title;}
        @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){
            if(!level.isClientSide){
                player.sendSystemMessage(Component.literal("§6"+title+" §7["+pos.getX()+", "+pos.getY()+", "+pos.getZ()+"]"));
                if(title.contains("Depot")) player.sendSystemMessage(Component.literal("§eUse /freight depot set to register your company's home depot."));
                else if(title.contains("Marketplace")) player.sendSystemMessage(Component.literal("§eUse /freight shop to view equipment available for company credits."));
                else if(title.contains("Freight Terminal")) player.sendSystemMessage(Component.literal("§eUse /freight contracts, /freight accept <id> and /freight deliver."));
                else if(title.contains("Server Rack")) player.sendSystemMessage(Component.literal("§ePhysical server component slots and automation processing are planned, not enabled in alpha."));
                else player.sendSystemMessage(Component.literal("§eUse /freight to open the available management commands."));
            }
            return InteractionResult.SUCCESS;
        }
    }
}
