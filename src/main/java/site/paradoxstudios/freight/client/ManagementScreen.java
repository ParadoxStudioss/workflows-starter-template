package site.paradoxstudios.freight.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import site.paradoxstudios.freight.ManagementMenu;

public final class ManagementScreen extends AbstractContainerScreen<ManagementMenu> {
    private static final int BG=0xff202a32, PANEL=0xff2e3d45, BORDER=0xffa17946, TEXT=0xffe9dec2, MUTED=0xffa3b5b9;
    private final java.util.List<Button> shopButtons=new java.util.ArrayList<>();
    private final java.util.List<Button> depotButtons=new java.util.ArrayList<>();
    private final java.util.List<Button> orderButtons=new java.util.ArrayList<>();
    public ManagementScreen(ManagementMenu menu,Inventory inventory,Component title){
        super(menu,inventory,title);
        imageWidth=280;imageHeight=210;
        inventoryLabelY=1000;titleLabelY=1000;
    }
    private void act(int id){
        if(Minecraft.getInstance().gameMode!=null)Minecraft.getInstance().gameMode.handleInventoryButtonClick(menu.containerId,id);
    }
    private void button(int x,int y,int width,int height,String label,int id,java.util.List<Button> group){
        Button btn=Button.builder(Component.literal(label),b->act(id))
            .bounds(leftPos+x,topPos+y,width,height).build();
        group.add(btn);addRenderableWidget(btn);
    }
    @Override protected void init(){
        super.init();
        String[] labels={"CPU 450","RAM 180","SSD 270","NETWORK 310","COOLING 140","MANIFEST 35","TRAIN VOUCHER 8500"};
        for(int i=0;i<labels.length;i++)button(16+(i%2)*125,57+(i/2)*30,116,22,labels[i],30+i,shopButtons);
        button(25,127,226,25,"REGISTER CURRENT POSITION AS DEPOT",25,depotButtons);
        button(17,170,46,23,"<<",10,orderButtons);
        button(219,170,46,23,">>",11,orderButtons);
        for(int i=0;i<4;i++)button(207,62+i*25,57,18,"ACCEPT",20+i,orderButtons);
        button(85,170,116,23,"DELIVER CARGO",24,orderButtons);
        updateVisibility();
    }
    private void updateVisibility(){
        for(Button b:shopButtons)b.visible=menu.view()==1;
        for(Button b:depotButtons)b.visible=menu.view()==3;
        for(Button b:orderButtons)b.visible=menu.view()!=1&&menu.view()!=3;
    }
    @Override protected void renderBg(GuiGraphics g,float partialTick,int mouseX,int mouseY){
        updateVisibility();
        int x=leftPos,y=topPos;
        g.fill(x,y,x+imageWidth,y+imageHeight,BORDER);
        g.fill(x+2,y+2,x+imageWidth-2,y+imageHeight-2,BG);
        g.fill(x+8,y+9,x+imageWidth-8,y+36,0xff172228);
        g.fill(x+8,y+44,x+imageWidth-8,y+imageHeight-9,PANEL);
        String heading=switch(menu.view()){
            case 1->"RAILWAY MARKETPLACE";case 2->"FREIGHT TERMINAL";case 3->"DEPOT CONTROLLER";
            case 4->"FACTORY CONTROLLER";case 5->"WAREHOUSE CONTROL";
            case 6->"DISPLAY INFORMATION RELAY";case 7->"RAILWAY SIGNAL CONTROL";
            default->"FREIGHT & LOGISTICS OS";
        };
        g.drawString(font,heading,x+17,y+17,TEXT,false);
        if(menu.view()==1){
            g.drawString(font,"HARDWARE & EQUIPMENT",x+17,y+45,TEXT,false);
            g.drawString(font,"Company credits: "+menu.val(1),x+17,y+184,0xff8fe1a7,false);
            g.drawString(font,"Purchases delivered to inventory.",x+17,y+196,MUTED,false);
        }else if(menu.view()==3){
            g.drawString(font,"Company depot registered: "+(menu.val(5)==1?"YES":"NO"),x+19,y+60,TEXT,false);
            g.drawString(font,"Depot location saved to world data.",x+19,y+81,MUTED,false);
            g.drawString(font,"Automatic train parking is not active yet.",x+19,y+97,MUTED,false);
        }else{
            g.drawString(font,"Credits: "+menu.val(1)+" | Pending: "+menu.val(3),x+17,y+45,0xff9edfbc,false);
            for(int i=0;i<4;i++){
                int id=menu.val(6+i*4),yy=y+65+i*25;
                if(id==0){g.drawString(font,"-",x+18,yy,MUTED,false);continue;}
                int itemId=menu.val(7+i*4),count=menu.val(8+i*4),reward=menu.val(9+i*4);
                String item=BuiltInRegistries.ITEM.byId(itemId).getDescription().getString();
                if(item.length()>13)item=item.substring(0,13);
                g.drawString(font,"#"+id+" "+item+" x"+count,x+17,yy,TEXT,false);
                g.drawString(font,reward>=0?"+"+reward+"cr":reward==-1?"ASSIGNED":"DONE",x+17,yy+10,reward>0?0xff9edfbc:MUTED,false);
            }
            g.drawString(font,"Page "+menu.val(2)+"/10",x+117,y+158,MUTED,false);
            g.drawString(font,"Accept and deliver actual items for credits.",x+17,y+197,MUTED,false);
        }
    }
    @Override public void render(GuiGraphics graphics,int mx,int my,float ticks){
        renderBackground(graphics,mx,my,ticks);
        super.render(graphics,mx,my,ticks);
        renderTooltip(graphics,mx,my);
    }
}
