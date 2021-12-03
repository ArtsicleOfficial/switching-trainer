package com.switching;

import net.runelite.api.Experience;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.util.ImageUtil;

import java.awt.*;
import java.awt.image.BufferedImage;

public class XPOverlay extends Overlay {

    private SwitchingPlugin plugin;
    private boolean hovering = false;
    BufferedImage cursorSkill;
    public XPOverlay(SwitchingPlugin plugin) {
        super(plugin);

        this.plugin = plugin;

        cursorSkill = ImageUtil.loadImageResource(getClass(),"/switching.png");

        setDragTargetable(true);
        setPreferredPosition(OverlayPosition.BOTTOM_RIGHT);
    }

    @Override
    public void onMouseOver() {
        super.onMouseOver();
        this.hovering = true;
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        FontMetrics metrics = graphics.getFontMetrics();
        graphics.drawImage(cursorSkill,0,0,cursorSkill.getWidth(),cursorSkill.getHeight(),null);

        graphics.setColor(Color.YELLOW);
        graphics.drawString(Experience.getLevelForXp(plugin.switchingXP) + "",cursorSkill.getWidth()/2+1,17);
        graphics.drawString(Experience.getLevelForXp(plugin.switchingXP) + "",cursorSkill.getWidth()/2 + 15,27);

        if(this.hovering) {
            String text = "Switching XP: " + plugin.switchingXP;
            Rectangle rect = new Rectangle(5,cursorSkill.getHeight() + 5,metrics.stringWidth(text) + 4, metrics.getMaxAscent() + 6);
            graphics.setColor(Color.BLACK);
            graphics.fillRect(rect.x-1,rect.y-1,rect.width+2,rect.height+2);
            graphics.setColor(new Color(255,255,160));
            graphics.fillRect(rect.x,rect.y,rect.width,rect.height);
            graphics.setColor(Color.BLACK);
            graphics.drawString(text,rect.x + 2, rect.y + metrics.getMaxAscent() + 2);
        }

        this.hovering = false;
        return new Dimension(cursorSkill.getWidth(),cursorSkill.getHeight());
    }
}