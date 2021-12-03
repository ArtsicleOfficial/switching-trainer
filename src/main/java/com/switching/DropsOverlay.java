package com.switching;

import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.util.ImageUtil;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

public class DropsOverlay extends Overlay {

    BufferedImage icon;

    public static final int XP_DROP_SPEED = 2;

    public ArrayList<XPDrop> xpDrops = new ArrayList<>();

    public DropsOverlay(SwitchingPlugin plugin) {
        super(plugin);

        icon = ImageUtil.loadImageResource(getClass(),"/smallicon.png");
        setPriority(OverlayPriority.LOW);
        setPreferredPosition(OverlayPosition.TOP_RIGHT);
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        //arbitrary idk why this offset works
        int offset = icon.getHeight();

        for(int i = 0; i < xpDrops.size(); i++) {
            int y = xpDrops.get(i).y;
            xpDrops.get(i).y -= 2;

            int amtBelow = y < 0 ? -y : 0;

            graphics.drawImage(icon,0,y+offset,icon.getWidth(),y+icon.getHeight()+offset-amtBelow,0,amtBelow,icon.getWidth(),icon.getHeight()-amtBelow,null);
            graphics.setColor(Color.WHITE);
            graphics.drawString(xpDrops.get(i).xp + "",icon.getWidth()+1,y+14+offset);
        }
        xpDrops.removeIf(i -> i.y + icon.getHeight() < 0);
        return new Dimension(icon.getWidth() + 10,200);
    }
}
