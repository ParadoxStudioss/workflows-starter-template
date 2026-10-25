package site.paradoxstudios.freight.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import site.paradoxstudios.freight.RackMenu;

public final class RackScreen extends AbstractContainerScreen<RackMenu> {
    public RackScreen(RackMenu menu,Inventory inventory,Component title){
        super(menu,inventory,title);
        imageWidth=176;imageHeight=172;
        titleLabelY=7;inventoryLabelY=77;
    }
    @Override protected void renderBg(GuiGraphics g,float delta,int mx,int my){
        int x=leftPos,y=topPos;
        g.fill(x,y,x+176,y+172,0xffb98b4b);
        g.fill(x+2,y+2,x+174,y+170,0xff24303b);
        g.fill(x+7,y+37,x+169,y+64,0xff111d27);
        for(int i=0;i<5;i++){
            int sx=x+25+i*30;
            g.fill(sx-2,y+41,sx+19,y+62,0xff8a7852);
            g.fill(sx,y+43,sx+17,y+60,0xff3f5665);
        }
        g.fill(x+6,y+87,x+170,y+165,0xff3a4854);
        for(int r=0;r<4;r++)for(int c=0;c<9;c++){
            int yy=r==3?y+146:y+88+r*18,xx=x+8+c*18;
            g.fill(xx-1,yy-1,xx+17,yy+17,0xff9b987f);
            g.fill(xx,yy,xx+16,yy+16,0xff202b35);
        }
        String[] titles={"CPU","RAM","SSD","NET","FAN"};
        for(int i=0;i<5;i++)g.drawString(font,titles[i],x+26+i*30,y+29,0xffdbc9a0,false);
    }
    @Override protected void renderLabels(GuiGraphics g,int mx,int my){
        g.drawString(font,"INDUSTRIAL SERVER RACK",6,9,0xffe9d7a9,false);
        g.drawString(font,"Install CPU / RAM / SSD / NIC / FAN",8,67,0xff9fb9bc,false);
    }
    @Override public void render(GuiGraphics graphics,int mx,int my,float ticks){
        renderBackground(graphics,mx,my,ticks);
        super.render(graphics,mx,my,ticks);
        renderTooltip(graphics,mx,my);
    }
}
