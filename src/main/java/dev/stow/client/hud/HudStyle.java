package dev.stow.client.hud;

import dev.stow.Stow;

/** One physical size for stock, equipment and project HUDs, independent of menu scale. */
public final class HudStyle {
    public static final int ICON=16,ROW=20,TEXT_X=22,TEXT_Y=4;
    private HudStyle(){}
    public static float scale(float guiScale,int relativePercent){
        // Retain the saved building-count size as the common size when upgrading.
        return Math.clamp(Stow.config.buildingStockScale,100,200)/100f
            *Math.clamp(relativePercent,60,150)/100f
            *Math.clamp(.75f*guiScale,1f,1.5f)/Math.max(1f,guiScale);
    }
}
