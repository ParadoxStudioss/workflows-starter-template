package site.paradoxstudios.freight;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import java.util.*;
import java.util.function.Supplier;

public final class FreightCommands {
    private FreightCommands(){}
    @SubscribeEvent public static void register(RegisterCommandsEvent ev){
        ev.getDispatcher().register(Commands.literal("freight")
            .executes(c->help(c.getSource()))
            .then(Commands.literal("help").executes(c->help(c.getSource())))
            .then(Commands.literal("company")
                .then(Commands.literal("create")
                    .then(Commands.argument("name",StringArgumentType.greedyString())
                        .executes(c->create(c.getSource(),StringArgumentType.getString(c,"name")))))
                .then(Commands.literal("info").executes(c->balance(c.getSource()))))
            .then(Commands.literal("balance").executes(c->balance(c.getSource())))
            .then(Commands.literal("contracts").executes(c->list(c.getSource(),1))
                .then(Commands.argument("page",IntegerArgumentType.integer(1,10))
                    .executes(c->list(c.getSource(),IntegerArgumentType.getInteger(c,"page")))))
            .then(Commands.literal("accept")
                .then(Commands.argument("id",IntegerArgumentType.integer(1))
                    .executes(c->accept(c.getSource(),IntegerArgumentType.getInteger(c,"id")))))
            .then(Commands.literal("deliver").executes(c->deliver(c.getSource())))
            .then(Commands.literal("depot")
                .then(Commands.literal("set").executes(c->depot(c.getSource())))
                .then(Commands.literal("toggle").executes(c->toggleParking(c.getSource())))
                .then(Commands.literal("status").executes(c->depotStatus(c.getSource()))))
            .then(Commands.literal("shop").executes(c->shop(c.getSource()))
                .then(Commands.literal("buy")
                    .then(Commands.argument("part",StringArgumentType.word())
                        .executes(c->buy(c.getSource(),StringArgumentType.getString(c,"part"))))))
.then(Commands.literal("railway")
                .then(Commands.literal("status").executes(c->railwayStatus(c.getSource())))
                .then(Commands.literal("verify")
                    .then(Commands.argument("x",IntegerArgumentType.integer())
                        .then(Commands.argument("z",IntegerArgumentType.integer())
                            .executes(c->railwayVerify(c.getSource(),
                                IntegerArgumentType.getInteger(c,"x"),
                                IntegerArgumentType.getInteger(c,"z"))))))
                .then(Commands.literal("inspect")
                    .then(Commands.argument("x",IntegerArgumentType.integer())
                        .then(Commands.argument("z",IntegerArgumentType.integer())
                            .executes(c->railwayInspect(c.getSource(),
                                IntegerArgumentType.getInteger(c,"x"),
                                IntegerArgumentType.getInteger(c,"z"))))))
                .then(Commands.literal("locate").executes(c->railwayLocate(c.getSource())))
                .then(Commands.literal("generate")
                    .requires(s->s.hasPermission(2))
                    .executes(c->railwayGenerate(c.getSource()))))
            .then(Commands.literal("railgen")
                .requires(s->s.hasPermission(2))
                .executes(c->railgen(c.getSource())))
            .then(Commands.literal("issue")
                .requires(s->s.hasPermission(2))
                .then(Commands.argument("item",StringArgumentType.word())
                    .then(Commands.argument("count",IntegerArgumentType.integer(1,4096))
                        .then(Commands.argument("reward",IntegerArgumentType.integer(1,100000))
                            .executes(c->issue(c.getSource(),
                                StringArgumentType.getString(c,"item"),
                                IntegerArgumentType.getInteger(c,"count"),
                                IntegerArgumentType.getInteger(c,"reward")))))))
        );
    }
    private static int railwayStatus(CommandSourceStack source) {
        info(source,"§6Freight Railway Generation §7- server settings");
        info(source,"§7Enabled: "+RailwayGenerationConfig.ENABLED.get()+
            " | Grid: "+RailwayGenerationConfig.GRID_SPACING.get()*16+" blocks"+
            " | Elevation: TERRAIN-FOLLOWING");
        int graphs=com.simibubi.create.Create.RAILWAYS.trackNetworks.size();
        int nodes=com.simibubi.create.Create.RAILWAYS.trackNetworks.values().stream()
            .mapToInt(graph->graph.getNodes().size()).sum();
        info(source,"§7Create railway graphs: "+graphs+" | Track nodes: "+nodes);
        info(source,"§7Built rail chunks: "+RailwayGenerationService.builtChunks+
            " | Tracks placed: "+RailwayGenerationService.builtRails+
            " | Active stations named: "+RailwayGenerationService.builtStations+
            " | Curved junctions: "+RailwayGenerationService.turnoutCurves+
            " | Obstructed chunks: "+RailwayGenerationService.blockedChunks);
        info(source,"§eOnly newly generated chunks get automatic rails. Changes to spacing/Y break existing alignment.");
        return 1;
    }
    private static int railwayInspect(CommandSourceStack source,int x,int z){
        ServerLevel level=source.getLevel();
        int spacing=RailwayGenerationConfig.GRID_SPACING.get()*16;
        boolean horizontal=Math.floorMod(z-8,spacing)==0;
        int y=RailwayTerrain.route(level,x,z,spacing,horizontal);
        BlockPos pos=new BlockPos(x,y,z);
        info(source,"§6Railway terrain-height: "+y+" | block: "+
            level.getBlockState(pos).getBlock().builtInRegistryHolder().key().location());
        info(source,"§7Ground surface: "+(level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x,z)-1));
        return 1;
    }
    private static int railwayVerify(CommandSourceStack source,int x,int z) {
        ServerLevel level=source.getLevel();
        int spacing=RailwayGenerationConfig.GRID_SPACING.get()*16;
        boolean horizontal=Math.floorMod(z-8,spacing)==0;
        int y=RailwayTerrain.route(level,x,z,spacing,horizontal);
        // A section ending at x (usually a chunk border) and the next block
        // should lie in the SAME Create railway graph, not just look adjacent.
        var first=new net.minecraft.core.BlockPos(x,y,z);
        var second=first.east().atY(RailwayTerrain.route(level,x+1,z,spacing,horizontal));
        if(!level.hasChunkAt(first)||!level.hasChunkAt(second)){
            source.sendFailure(Component.literal("Both candidate track chunks must be loaded."));return 0;
        }
        var a=level.getBlockState(first),b=level.getBlockState(second);
        if(!(a.getBlock() instanceof com.simibubi.create.content.trains.track.ITrackBlock ta)||
           !(b.getBlock() instanceof com.simibubi.create.content.trains.track.ITrackBlock tb)) {
            source.sendFailure(Component.literal("Missing Create track blocks at these positions."));return 0;
        }
        var one=com.simibubi.create.content.trains.graph.TrackGraphHelper.getGraphLocationAt(
            level,first,net.minecraft.core.Direction.AxisDirection.POSITIVE,ta.getTrackAxes(level,first,a).get(0));
        var two=com.simibubi.create.content.trains.graph.TrackGraphHelper.getGraphLocationAt(
            level,second,net.minecraft.core.Direction.AxisDirection.POSITIVE,tb.getTrackAxes(level,second,b).get(0));
        if(one==null||two==null||one.graph!=two.graph){
            source.sendFailure(Component.literal("Rail graph DISCONNECTED at x="+x+"->"+(x+1)+", z="+z));
            return 0;
        }
        info(source,"§aRail graph CONNECTED across x="+x+" -> "+(x+1)+" at z="+z);
        return 1;
    }
    private static int railwayLocate(CommandSourceStack source){
        ServerPlayer player=player(source);if(player==null)return 0;
        int space=RailwayGenerationConfig.GRID_SPACING.get()*16;
        int centerX=8+(int)Math.round((player.getBlockX()-8-64)/(double)space)*space+64;
        int centerZ=8+(int)Math.round((player.getBlockZ()-8)/(double)space)*space;
        info(source,"§aNearest planned station: §e"+centerX+", "+RailwayTerrain.route(player.serverLevel(),centerX,centerZ,space,true)+", "+centerZ);
        info(source,"§7Generated only when its chunk was NEW. For operator test use /tp "
            +centerX+" "+(RailwayTerrain.route(player.serverLevel(),centerX,centerZ,space,true)+3)+" "+centerZ);
        return 1;
    }
    private static int railwayGenerate(CommandSourceStack source){
        ServerPlayer player=player(source);if(player==null)return 0;
        ServerLevel level=player.serverLevel();
        if(level.dimension()!=net.minecraft.world.level.Level.OVERWORLD) {
            source.sendFailure(Component.literal("Railway generation is Overworld-only."));return 0;
        }
        net.minecraft.world.level.ChunkPos pos=new net.minecraft.world.level.ChunkPos(player.blockPosition());
        if(!RailwayNetworkGenerator.relevant(pos)){
            source.sendFailure(Component.literal("No planned mainline in this chunk. Use /freight railway locate."));return 0;
        }
        var r=RailwayNetworkGenerator.generate(level,level.getChunk(pos.x,pos.z));
        if(!r.built()){source.sendFailure(Component.literal("Generation skipped (protected terrain or track already built)."));return 0;}
        for(var station:r.stations())RailwayGenerationService.enqueueStation(level,station);
        info(source,"§aGenerated "+r.tracks()+" real Create tracks in this chunk; "+r.stations().size()+" station(s).");
        return 1;
    }
    private static int railgen(CommandSourceStack source){
        ServerPlayer p=player(source);if(p==null)return 0;
        BlockPosAtPosition loc=new BlockPosAtPosition(p.blockPosition().offset(0,0,2));
        if(RailStopFeature.placeTrackStop(p.serverLevel(),loc.pos(),31,true)){
            info(source,"§aBuilt a 31-block straight Create track test section. This is a manual test route.");
            return 1;
        }
        source.sendFailure(Component.literal("Not enough clear space for railway test section."));
        return 0;
    }
    private record BlockPosAtPosition(net.minecraft.core.BlockPos pos){}
    private static int help(CommandSourceStack source){
        info(source,"§6Freight & Logistics §7v0.1.2");
        info(source,"§e/freight company create <name> §7- create company");
        info(source,"§e/freight balance §7- company funds");
        info(source,"§e/freight contracts [1-10] §7- paginated contract list");
        info(source,"§e/freight accept <id> §7- accept available contract");
        info(source,"§e/freight deliver §7- submit real inventory items for your contract");
        info(source,"§e/freight depot set/toggle/status §7- register a Create station and automate returns");
        info(source,"§e/freight shop §7and §e/freight shop buy <item>");
        info(source,"§e/freight railway status/locate §7- network overview");
        info(source,"§e/freight railway verify <x> <z> §7- actual Create graph continuity");
        info(source,"§e/freight railway inspect <x> <z> §7- planned terrain track height");
        info(source,"§e/freight railway generate §7- build planned rails in this chunk (operator)");
        info(source,"§e/freight railgen §7- old straight test track (operator)");
        info(source,"§e/freight issue <item_id> <count> <reward> §7- operators only");
        return 1;
    }
    private static void info(CommandSourceStack source,String message){
        source.sendSuccess(()->Component.literal(message),false);
    }
    private static ServerPlayer player(CommandSourceStack source){
        return source.getPlayer();
    }
    private static int create(CommandSourceStack source,String name){
        ServerPlayer p=player(source); if(p==null)return 0;
        FreightData data=FreightData.get(source.getServer());
        if(data.company(p.getUUID())!=null){ source.sendFailure(Component.literal("You already own a company."));return 0;}
        if(name.length()>40||name.isBlank()){source.sendFailure(Component.literal("Company name must be 1–40 characters."));return 0;}
        FreightData.Company company=data.create(p.getUUID(),name);
        info(source,"§aCreated "+company.name+" with "+company.balance+" credits. Use /freight contracts.");
        return 1;
    }
    private static int balance(CommandSourceStack source){
        ServerPlayer p=player(source);if(p==null)return 0;
        FreightData.Company company=FreightData.get(source.getServer()).company(p.getUUID());
        if(company==null){source.sendFailure(Component.literal("Create a company with /freight company create <name>."));return 0;}
        info(source,"§6"+company.name+" §7| balance: §a"+company.balance+" credits§7 | depot: "+(company.depot==0?"not registered":Long.toString(company.depot)));
        return 1;
    }
    private static int issue(CommandSourceStack source,String itemId,int count,int reward){
        ResourceLocation id=ResourceLocation.tryParse(itemId);
        if(id==null||!BuiltInRegistries.ITEM.containsKey(id)){source.sendFailure(Component.literal("Unknown item id. Example: minecraft:iron_ingot"));return 0;}
        FreightData data=FreightData.get(source.getServer());
        FreightData.Contract c=data.issue(id.toString(),count,reward);
        info(source,"§aIssued contract #"+c.id+" for "+count+" "+c.item+" worth "+c.reward+" credits.");
        return 1;
    }
    private static int list(CommandSourceStack source,int page){
        FreightData data=FreightData.get(source.getServer());
        info(source,"§6Freight Contracts · Page "+page+"/10");
        var contracts=new ArrayList<>(data.contracts.values());
        contracts.sort(Comparator.comparingInt(c->c.id));
        int start=(page-1)*5, stop=Math.min(contracts.size(),start+5);
        if(start>=contracts.size()){info(source,"§7No contracts on this page.");return 1;}
        for(int i=start;i<stop;i++){
            FreightData.Contract c=contracts.get(i);
            String state=c.completed?"§aDONE":c.assignedTo==null?"§eOPEN":"§bASSIGNED";
            info(source,"§7#"+c.id+" "+state+" §f"+c.count+" "+c.item+" §a+"+c.reward+"cr");
        }
        return 1;
    }
    public static int accept(CommandSourceStack source,int id){
        ServerPlayer p=player(source);if(p==null)return 0;
        FreightData data=FreightData.get(source.getServer());
        if(data.company(p.getUUID())==null){source.sendFailure(Component.literal("You need a company first."));return 0;}
        FreightData.Contract c=data.contracts.get(id);
        if(c==null||c.completed||c.assignedTo!=null){source.sendFailure(Component.literal("Contract not available."));return 0;}
        for(FreightData.Contract order:data.contracts.values())
            if(p.getUUID().equals(order.assignedTo)&&!order.completed) {
                source.sendFailure(Component.literal("Finish your existing accepted contract first."));return 0;
            }
        c.assignedTo=p.getUUID();data.setDirty();
        info(source,"§aAccepted contract #"+c.id+". Gather "+c.count+" "+c.item+" and use /freight deliver.");
        return 1;
    }
    public static int deliver(CommandSourceStack source){
        ServerPlayer p=player(source);if(p==null)return 0;
        FreightData data=FreightData.get(source.getServer());
        FreightData.Company company=data.company(p.getUUID());
        if(company==null)return 0;
        FreightData.Contract job=null;
        for(FreightData.Contract c:data.contracts.values())
            if(p.getUUID().equals(c.assignedTo)&&!c.completed){job=c;break;}
        if(job==null){source.sendFailure(Component.literal("No accepted contract."));return 0;}
        Item item=BuiltInRegistries.ITEM.get(ResourceLocation.parse(job.item));
        int remaining=job.count;
        for(int i=0;i<p.getInventory().getContainerSize();i++){
            ItemStack stack=p.getInventory().getItem(i);
            if(stack.is(item))remaining-=stack.getCount();
        }
        if(remaining>0){source.sendFailure(Component.literal("Missing "+remaining+" "+job.item+" to complete this contract."));return 0;}
        remaining=job.count;
        for(int i=0;i<p.getInventory().getContainerSize()&&remaining>0;i++){
            ItemStack stack=p.getInventory().getItem(i);
            if(stack.is(item)){int used=Math.min(remaining,stack.getCount());stack.shrink(used);remaining-=used;}
        }
        p.getInventory().setChanged();
        company.balance=Math.min(Integer.MAX_VALUE,company.balance+job.reward);
        job.completed=true;data.setDirty();
        info(source,"§aDelivery verified. Contract #"+job.id+" completed. +"+job.reward+" credits.");
        return 1;
    }
    public static int depot(CommandSourceStack source){
        ServerPlayer p=player(source);if(p==null)return 0;
        FreightData data=FreightData.get(source.getServer());
        FreightData.Company company=data.company(p.getUUID());
        if(company==null){source.sendFailure(Component.literal("Create a company first."));return 0;}
        var station=DepotAutomation.nearbyStation(p.serverLevel(),p.blockPosition(),12);
        if(station==null){source.sendFailure(Component.literal("Place a Create Train Station within 12 blocks, with a named station, first."));return 0;}
        company.depot=p.blockPosition().asLong();
        company.depotStation=station.name;company.autoParking=true;
        data.setDirty();
        info(source,"§aDepot registered at Create station: "+station.name+" | Automatic parking enabled.");
        return 1;
    }
    public static int toggleParking(CommandSourceStack source){
        ServerPlayer p=player(source);if(p==null)return 0;
        FreightData data=FreightData.get(source.getServer());
        FreightData.Company co=data.company(p.getUUID());
        if(co==null||co.depotStation.isBlank()){source.sendFailure(Component.literal("First register a nearby Create train station as a depot."));return 0;}
        co.autoParking=!co.autoParking;data.setDirty();
        info(source,"§6Automatic depot return: "+(co.autoParking?"§aENABLED":"§cDISABLED"));
        return 1;
    }
    public static int depotStatus(CommandSourceStack source){
        ServerPlayer p=player(source);if(p==null)return 0;
        FreightData.Company co=FreightData.get(source.getServer()).company(p.getUUID());
        if(co==null)return 0;
        info(source,"§6Depot: §e"+co.depotStation+" §7/ parking "+(co.autoParking?"§aON":"§cOFF"));
        info(source,"§7Eligible Create-owned trains: "+DepotAutomation.ownedTrains(p.getUUID()));
        return 1;
    }
    private static final Map<String,Integer> SHOP=Map.of(
       "industrial_cpu",450, "memory_module",180, "storage_drive",270,
       "network_card",310, "cooling_unit",140,
       "freight_manifest",35, "train_purchase_contract",8500);
    private static int shop(CommandSourceStack source){
        info(source,"§6Railway marketplace §7(item : credits)");
        SHOP.forEach((id,price)->info(source,"§e"+id+" §7: "+price));
        info(source,"§7Use /freight shop buy <item> to buy one.");
        return 1;
    }
    public static int buy(CommandSourceStack source,String requested){
        ServerPlayer p=player(source);if(p==null)return 0;
        FreightData data=FreightData.get(source.getServer());
        FreightData.Company company=data.company(p.getUUID());
        if(company==null){source.sendFailure(Component.literal("Create a company first."));return 0;}
        Integer price=SHOP.get(requested);
        if(price==null){source.sendFailure(Component.literal("Unknown shop item. Use /freight shop."));return 0;}
        if(company.balance<price){source.sendFailure(Component.literal("Not enough credits."));return 0;}
        var holder=FreightLogistics.PARTS.get(requested);
        if(holder==null)return 0;
        ItemStack purchase=new ItemStack(holder.get());
        if(!p.getInventory().add(purchase)){source.sendFailure(Component.literal("Inventory full. No money charged."));return 0;}
        company.balance-=price;data.setDirty();
        info(source,"§aPurchased "+requested+" for "+price+" credits. Remaining: "+company.balance);
        if(requested.equals("train_purchase_contract"))info(source,"§eThis is a purchase voucher. Automatic Create train assembly/delivery is not implemented in this alpha.");
        return 1;
    }
}
