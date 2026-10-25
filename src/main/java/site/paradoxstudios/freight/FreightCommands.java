package site.paradoxstudios.freight;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
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
                .then(Commands.literal("set").executes(c->depot(c.getSource()))))
            .then(Commands.literal("shop").executes(c->shop(c.getSource()))
                .then(Commands.literal("buy")
                    .then(Commands.argument("part",StringArgumentType.word())
                        .executes(c->buy(c.getSource(),StringArgumentType.getString(c,"part"))))))
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
    private static int help(CommandSourceStack source){
        info(source,"§6Freight & Logistics §7v0.1.2");
        info(source,"§e/freight company create <name> §7- create company");
        info(source,"§e/freight balance §7- company funds");
        info(source,"§e/freight contracts [1-10] §7- paginated contract list");
        info(source,"§e/freight accept <id> §7- accept available contract");
        info(source,"§e/freight deliver §7- submit real inventory items for your contract");
        info(source,"§e/freight depot set §7- set depot to current position");
        info(source,"§e/freight shop §7and §e/freight shop buy <item>");
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
        company.depot=p.blockPosition().asLong();data.setDirty();
        info(source,"§aCompany depot registered at "+p.blockPosition().toShortString()+".");
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
