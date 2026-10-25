package site.paradoxstudios.freight.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import site.paradoxstudios.freight.WarehouseMenu;

public final class WarehouseScreen extends AbstractContainerScreen<WarehouseMenu> {
    public WarehouseScreen(WarehouseMenu menu,Inventory inv,Component title){
        super(menu,inv,title);
        imageWidth=176;imageHeight=178;
        inventoryLabelY=83;titleLabelY=7;
    }
    @Override protected void renderBg(GuiGraphics g,float delta,int x,int y){
        int left=leftPos,top=topPos;
        g.fill(left,top,left+176,top+178,0xffad8546);
        g.fill(left+2,top+2,left+174,top+176,0xff25313b);
        g.fill(left+5,top+22,left+171,top+78,0xff36464a);
        g.fill(left+5,top+91,left+171,top+173,0xff36464a);
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)slot(g,left+8+col*18,top+27+row*18);
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)slot(g,left+8+col*18,top+95+row*18);
        for(int col=0;col<9;col++)slot(g,left+8+col*18,top+153);
    }
    private static void slot(GuiGraphics g,int x,int y){
        g.fill(x-1,y-1,x+17,y+17,0xff8a7654);g.fill(x,y,x+16,y+16,0xff192833);
    }
    @Override protected void renderLabels(GuiGraphics g,int x,int y){
        g.drawString(font,"WAREHOUSE STORAGE",8,7,0xffe9ddbd,false);
        g.drawString(font,"INVENTORY",8,83,0xffc5cbcb,false);
    }
    @Override public void render(GuiGraphics graphics,int mx,int my,float delta){
        renderBackground(graphics,mx,my,delta);
        super.render(graphics,mx,my,delta);renderTooltip(graphics,mx,my);
    }
}
