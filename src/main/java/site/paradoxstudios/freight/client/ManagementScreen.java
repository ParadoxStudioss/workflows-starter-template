package site.paradoxstudios.freight.client;

import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import site.paradoxstudios.freight.ManagementMenu;

/** Each physical control machine has its own distinct screen state and action set. */
public final class ManagementScreen extends AbstractContainerScreen<ManagementMenu> {
    private static final int BG=0xff18252c,SCREEN=0xff233940,BORDER=0xffbc9153,
        TEXT=0xfff2e1be,DIM=0xffa2b9bc,GREEN=0xffa4e7ae,RED=0xffff9c92;
    private final List<Button> shop=new ArrayList<>(),depot=new ArrayList<>(),orders=new ArrayList<>();
    private int activeView=-1;
    public ManagementScreen(ManagementMenu menu,Inventory inv,Component title){
        super(menu,inv,title);imageWidth=300;imageHeight=222;
        inventoryLabelY=1000;titleLabelY=1000;
    }
    private void action(int id){
        if(Minecraft.getInstance().gameMode!=null)
            Minecraft.getInstance().gameMode.handleInventoryButtonClick(menu.containerId,id);
    }
    private void add(List<Button> list,int x,int y,int w,int h,String name,int action){
        Button b=Button.builder(Component.literal(name),btn->action(action))
            .bounds(leftPos+x,topPos+y,w,h).build();
        list.add(b);addRenderableWidget(b);
    }
    @Override protected void init(){
        super.init();
        for(int i=0;i<7;i++){
            String[] names={"CPU","MEMORY","SSD","NETWORK","COOLING","MANIFEST","TRAIN VOUCHER"};
            String[] prices={"450","180","270","310","140","35","8500"};
            add(shop,20+i%2*132,61+(i/2)*32,122,26,names[i]+" · "+prices[i],30+i);
        }
        add(depot,20,121,260,26,"REGISTER NEARBY CREATE STATION",25);
        add(depot,20,154,260,26,"TOGGLE AUTOMATIC RETURN",26);
        add(orders,20,179,46,25,"<",10);
        add(orders,234,179,46,25,">",11);
        for(int i=0;i<4;i++)add(orders,218,61+i*27,62,21,"ACCEPT",20+i);
        add(orders,86,179,128,25,"DELIVER",24);
        sync();
    }
    private void sync(){
        activeView=menu.view();
        for(Button b:shop)b.visible=activeView==1;
        for(Button b:depot)b.visible=activeView==3;
        for(Button b:orders)b.visible=activeView==0||activeView==2;
    }
    private void line(GuiGraphics g,int yy,String left,String right,int colour){
        int x=leftPos+20,y=topPos+yy;
        g.drawString(font,left,x,y,TEXT,false);
        g.drawString(font,right,leftPos+279-font.width(right),y,colour,false);
    }
    @Override protected void renderBg(GuiGraphics g,float delta,int mx,int my){
        if(activeView!=menu.view())sync();
        int x=leftPos,y=topPos;
        g.fill(x,y,x+300,y+222,BORDER);
        g.fill(x+2,y+2,x+298,y+220,BG);
        g.fill(x+9,y+9,x+291,y+42,0xff11222a);
        g.fill(x+11,y+51,x+289,y+211,SCREEN);
        String heading=switch(activeView){
            case 1->"MARKETPLACE  /  PURCHASE TERMINAL";
            case 2->"FREIGHT DESK  /  CONTRACTS";
            case 3->"DEPOT DISPATCH  /  HOME STATION";
            case 4->"FACTORY  /  INDUSTRIAL MONITOR";
            case 5->"WAREHOUSE  /  STORAGE";
            case 6->"DISPLAY RELAY  /  NETWORK";
            case 7->"SIGNAL CABINET  /  TRACKSIDE";
            default->"PARADOX RAIL OS  /  OVERVIEW";
        };
        g.drawString(font,heading,x+19,y+20,TEXT,false);
        if(activeView==1){
            line(g,46,"EQUIPMENT SHOP","CR "+menu.val(1),GREEN);
            g.drawString(font,"All items are delivered to your inventory.",x+20,y+197,DIM,false);
        }else if(activeView==3){
            line(g,60,"Owned Create trains",""+menu.val(22),GREEN);
            line(g,82,"Parked at assigned station",""+menu.val(23),GREEN);
            line(g,104,"Depot station found",menu.val(25)==1?"YES":"NOT SET",menu.val(25)==1?GREEN:RED);
            line(g,192,"AUTO RETURN",menu.val(24)==1?"ENABLED":"DISABLED",menu.val(24)==1?GREEN:RED);
        }else if(activeView==4){
            line(g,64,"Nearby warehouse controllers",""+menu.val(26),GREEN);
            line(g,87,"Items in connected warehouses",""+menu.val(27),GREEN);
            g.drawString(font,"Use Create stock links and machines",x+21,y+123,DIM,false);
            g.drawString(font,"for physical production and packaging.",x+21,y+139,DIM,false);
            g.drawString(font,"Automatic production control: planned.",x+21,y+170,RED,false);
        }else if(activeView==6){
            line(g,64,"Source","COMPANY INFORMATION",GREEN);
            line(g,87,"Current contracts",""+menu.val(3),GREEN);
            g.drawString(font,"Create Display Link output is not wired",x+21,y+125,DIM,false);
            g.drawString(font,"to this relay yet.",x+21,y+139,DIM,false);
        }else if(activeView==7){
            line(g,64,"Relay state","INSTALLED",GREEN);
            g.drawString(font,"Use redstone to operate the warning",x+21,y+115,DIM,false);
            g.drawString(font,"lights and mechanical barrier.",x+21,y+129,DIM,false);
            g.drawString(font,"Train approach detection: planned.",x+21,y+170,RED,false);
        }else if(activeView==0||activeView==2){
            line(g,47,"CREDITS: "+menu.val(1),"OPEN ORDERS: "+menu.val(3),GREEN);
            for(int i=0;i<4;i++){
                int id=menu.val(6+i*4);
                int yy=64+i*27;
                if(id==0){g.drawString(font,"No order",x+20,y+yy,DIM,false);continue;}
                int itemId=menu.val(7+i*4);
                String item=BuiltInRegistries.ITEM.byId(itemId).getDescription().getString();
                if(item.length()>21)item=item.substring(0,21);
                g.drawString(font,"#"+id+" "+item,x+20,y+yy,TEXT,false);
                int reward=menu.val(9+i*4);
                g.drawString(font,menu.val(8+i*4)+" items  | "+
                    (reward>=0?reward+" credits":reward==-1?"ASSIGNED":"COMPLETED"),
                    x+20,y+yy+11,reward>=0?GREEN:DIM,false);
            }
            g.drawString(font,"PAGE "+menu.val(2)+" / 10",x+128,y+168,DIM,false);
        }
    }
    @Override public void render(GuiGraphics g,int mx,int my,float delta){
        renderBackground(g,mx,my,delta);
        super.render(g,mx,my,delta);
        renderTooltip(g,mx,my);
    }
}
