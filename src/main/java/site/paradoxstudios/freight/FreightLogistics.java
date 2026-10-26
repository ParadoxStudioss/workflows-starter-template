package site.paradoxstudios.freight;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.SimpleMenuProvider;
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
    public static final org.slf4j.Logger LOGGER=org.slf4j.LoggerFactory.getLogger("FreightLogistics");
    public static final DeferredRegister.Blocks BLOCKS=DeferredRegister.createBlocks(ID);
    public static final DeferredRegister.Items ITEMS=DeferredRegister.createItems(ID);
    public static final DeferredRegister<CreativeModeTab> TABS=DeferredRegister.create(Registries.CREATIVE_MODE_TAB,ID);
    public static final DeferredRegister<com.simibubi.create.api.behaviour.display.DisplaySource> DISPLAY_SOURCES=
        DeferredRegister.create(com.simibubi.create.api.registry.CreateRegistries.DISPLAY_SOURCE,ID);
    public static final DeferredHolder<com.simibubi.create.api.behaviour.display.DisplaySource,FreightDisplaySource> FREIGHT_DISPLAY=
        DISPLAY_SOURCES.register("freight_info",FreightDisplaySource::new);
    public static final DeferredRegister<MenuType<?>> MENUS=DeferredRegister.create(Registries.MENU,ID);
    public static final DeferredRegister<net.minecraft.world.level.levelgen.feature.Feature<?>> FEATURES=DeferredRegister.create(Registries.FEATURE,ID);
    public static final DeferredHolder<net.minecraft.world.level.levelgen.feature.Feature<?>,RailStopFeature> RAIL_STOP=
        FEATURES.register("railway_stop",()->new RailStopFeature(net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration.CODEC));
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES=DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE,ID);
    public static final Map<String,DeferredBlock<Block>> MACHINES=new LinkedHashMap<>();
    public static final Map<String,DeferredItem<Item>> PARTS=new LinkedHashMap<>();

    static {
        machine("management_computer","Railway Management Computer");
        // The server rack is a real block entity with persistent component inventory
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
    public static final DeferredBlock<Block> CROSSING=BLOCKS.registerBlock("crossing_signal",CrossingBlock::new,
        BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2.0f)
        .lightLevel(s->s.getValue(CrossingBlock.POWERED)?12:0));
    public static final DeferredBlock<Block> CROSSING_BARRIER=BLOCKS.registerBlock("crossing_barrier",CrossingBarrierBlock::new,
        BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2.0f));
    public static final DeferredItem<net.minecraft.world.item.BlockItem> BARRIER_ITEM=ITEMS.registerSimpleBlockItem("crossing_barrier",CROSSING_BARRIER);
    public static final DeferredBlock<Block> YARD_LAMP=BLOCKS.registerBlock("yard_lamp",CrossingBlock::new,
        BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2.0f).lightLevel(state->state.getValue(CrossingBlock.POWERED)?15:0));
    public static final DeferredItem<net.minecraft.world.item.BlockItem> LAMP_ITEM=ITEMS.registerSimpleBlockItem("yard_lamp",YARD_LAMP);
    public static final DeferredItem<net.minecraft.world.item.BlockItem> CROSSING_ITEM=ITEMS.registerSimpleBlockItem("crossing_signal",CROSSING);

    public static final DeferredHolder<MenuType<?>,MenuType<ManagementMenu>> MANAGEMENT_MENU=
        MENUS.register("management",()->new MenuType<>(ManagementMenu::new,FeatureFlags.DEFAULT_FLAGS));
    public static final DeferredHolder<MenuType<?>,MenuType<WarehouseMenu>> WAREHOUSE_MENU=
        MENUS.register("warehouse",()->new MenuType<>(WarehouseMenu::new,FeatureFlags.DEFAULT_FLAGS));
    public static final DeferredHolder<MenuType<?>,MenuType<RackMenu>> RACK_MENU=
        MENUS.register("server_rack",()->new MenuType<>(RackMenu::new,FeatureFlags.DEFAULT_FLAGS));
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<WarehouseBlockEntity>> WAREHOUSE_ENTITY=
        BLOCK_ENTITIES.register("warehouse",()->
            BlockEntityType.Builder.of(WarehouseBlockEntity::new,MACHINES.get("warehouse_controller").get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<RackBlockEntity>> RACK_ENTITY=
        BLOCK_ENTITIES.register("server_rack",()->
            BlockEntityType.Builder.of(RackBlockEntity::new,MACHINES.get("server_rack").get()).build(null));

    private static void machine(String id,String title) {
        DeferredBlock<Block> block=BLOCKS.registerBlock(id, p->
            id.equals("server_rack")?new RackBlock(p):
            id.equals("warehouse_controller")?new WarehouseBlock(p):new MachineBlock(p,id,title),
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2.4f));
        MACHINES.put(id,block);
        ITEMS.registerSimpleBlockItem(id,block);
    }
    private static void part(String id) {PARTS.put(id,ITEMS.registerSimpleItem(id,new Item.Properties()));}
    public static final DeferredHolder<CreativeModeTab,CreativeModeTab> TAB=TABS.register("main",()->
        CreativeModeTab.builder().title(Component.literal("Freight & Logistics"))
        .withTabsBefore(CreativeModeTabs.REDSTONE_BLOCKS)
        .icon(()->new ItemStack(MACHINES.get("management_computer").get()))
        .displayItems((params,output)->{
            for(var block:MACHINES.values())output.accept(block.get());
            output.accept(CROSSING.get());output.accept(CROSSING_BARRIER.get());output.accept(YARD_LAMP.get());
            for(var item:PARTS.values())output.accept(item.get());
        }).build());

    public FreightLogistics(IEventBus bus){
        net.neoforged.fml.ModLoadingContext.get().getActiveContainer().registerConfig(
            net.neoforged.fml.config.ModConfig.Type.SERVER,RailwayGenerationConfig.SPEC,"freightlogistics-server.toml");
        BLOCKS.register(bus);ITEMS.register(bus);TABS.register(bus);
        MENUS.register(bus);BLOCK_ENTITIES.register(bus);FEATURES.register(bus);
        DISPLAY_SOURCES.register(bus);
        bus.addListener((net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event)->
            event.enqueueWork(()->{
                for(String id:new String[]{"management_computer","railway_shop","freight_terminal",
                    "display_relay","depot_controller","factory_controller","warehouse_controller","server_rack"}){
                    com.simibubi.create.api.behaviour.display.DisplaySource.BY_BLOCK.add(MACHINES.get(id).get(),FREIGHT_DISPLAY.get());
                }
            }));
        NeoForge.EVENT_BUS.register(FreightCommands.class);
        NeoForge.EVENT_BUS.register(DepotAutomation.class);
        NeoForge.EVENT_BUS.register(StationChunkLoading.class);
        NeoForge.EVENT_BUS.register(RailwayGenerationService.class);
        bus.addListener((net.neoforged.neoforge.common.world.chunk.RegisterTicketControllersEvent e)->
            e.register(StationChunkLoading.TICKET));
    }

    public static class MachineBlock extends Block {
        public static final net.minecraft.world.level.block.state.properties.DirectionProperty FACING=net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING;
        protected final String id;
        protected final String title;
        public MachineBlock(Properties props,String id,String title){super(props);this.id=id;this.title=title;registerDefaultState(stateDefinition.any().setValue(FACING,net.minecraft.core.Direction.NORTH));}
        @Override protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<Block,BlockState> builder){builder.add(FACING);}
        @Override public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext ctx){
            return defaultBlockState().setValue(FACING,ctx.getHorizontalDirection().getOpposite());
        }
        @Override public BlockState rotate(BlockState state,net.minecraft.world.level.block.Rotation rotation){
            return state.setValue(FACING,rotation.rotate(state.getValue(FACING)));
        }
        @Override public BlockState mirror(BlockState state,net.minecraft.world.level.block.Mirror mirror){
            return state.rotate(mirror.getRotation(state.getValue(FACING)));
        }
        @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){
            if(!level.isClientSide&&player instanceof ServerPlayer sp){
                sp.openMenu(new SimpleMenuProvider((menuId,inventory,viewer)->
                    new ManagementMenu(menuId,inventory,id,sp,pos),Component.literal(title)));
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
    }
    public static class WarehouseBlock extends MachineBlock implements net.minecraft.world.level.block.EntityBlock {
        public WarehouseBlock(Properties props){super(props,"warehouse_controller","Warehouse Controller");}
        @Override public net.minecraft.world.level.block.entity.BlockEntity newBlockEntity(BlockPos pos,BlockState state){
            return new WarehouseBlockEntity(pos,state);
        }
        @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){
            if(!level.isClientSide&&player instanceof ServerPlayer sp&&level.getBlockEntity(pos) instanceof WarehouseBlockEntity be){
                sp.openMenu(new SimpleMenuProvider((id,inv,p)->new WarehouseMenu(id,inv,be),
                    Component.literal("Freight Warehouse")));
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        @Override protected void onRemove(BlockState state,Level level,BlockPos pos,BlockState newState,boolean moved){
            if(!state.is(newState.getBlock())&&!level.isClientSide&&level.getBlockEntity(pos) instanceof WarehouseBlockEntity be){
                for(int i=0;i<be.inventory.getSlots();i++){
                    ItemStack stack=be.inventory.getStackInSlot(i);
                    if(!stack.isEmpty())net.minecraft.world.Containers.dropItemStack(level,
                        pos.getX()+0.5,pos.getY()+0.5,pos.getZ()+0.5,stack.copy());
                }
            }
            super.onRemove(state,level,pos,newState,moved);
        }
    }
    public static class RackBlock extends MachineBlock implements net.minecraft.world.level.block.EntityBlock {
        public RackBlock(Properties props){super(props,"server_rack","Industrial Server Rack");}
        @Override public net.minecraft.world.level.block.entity.BlockEntity newBlockEntity(BlockPos pos,BlockState state){
            return new RackBlockEntity(pos,state);
        }
        @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){
            if(!level.isClientSide&&player instanceof ServerPlayer sp&&level.getBlockEntity(pos) instanceof RackBlockEntity be){
                sp.openMenu(new SimpleMenuProvider((menuId,inv,viewer)->
                    new RackMenu(menuId,inv,be),Component.literal("Industrial Server Rack")));
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        @Override protected void onRemove(BlockState state,Level level,BlockPos pos,BlockState newState,boolean moved){
            if(!state.is(newState.getBlock())&&!level.isClientSide&&level.getBlockEntity(pos) instanceof RackBlockEntity be){
                for(int i=0;i<be.parts.getSlots();i++){
                    ItemStack stack=be.parts.getStackInSlot(i);
                    if(!stack.isEmpty()){
                        net.minecraft.world.Containers.dropItemStack(level,pos.getX()+0.5,pos.getY()+0.5,pos.getZ()+0.5,stack.copy());
                    }
                }
            }
            super.onRemove(state,level,pos,newState,moved);
        }
    }
}
