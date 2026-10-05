package dev.yogi.yogiessentials.client.module.pvp;

import dev.yogi.yogiessentials.client.module.Category;
import dev.yogi.yogiessentials.client.module.Module;
import dev.yogi.yogiessentials.client.setting.BooleanSetting;
import dev.yogi.yogiessentials.client.setting.ColorSetting;
import dev.yogi.yogiessentials.client.setting.EnumSetting;
import dev.yogi.yogiessentials.client.setting.Setting;
import net.minecraft.class_1297;
import net.minecraft.class_1657;

/**
 * Cosmetic reach feedback using Minecraft's own target and hitbox systems.
 * Hitbox mode only recolors vanilla F3+B hitboxes; it never draws a second box.
 */
public final class ReachDisplayModule extends Module {

    public enum DisplayMode {
        HITBOX,
        CROSSHAIR_COLOR,
        BOTH
    }

    private final EnumSetting<DisplayMode> displayMode =
            new EnumSetting<>("Display Mode", DisplayMode.HITBOX, DisplayMode.class);

    private final BooleanSetting allEntities =
            new BooleanSetting("Highlight All Entities", false);

    private final BooleanSetting ignoreSpectators =
            new BooleanSetting("Ignore Spectators", true);

    private final ColorSetting highlightColor =
            new ColorSetting("Highlight Color", 0xFFFF0000);

    public ReachDisplayModule() {
        super(
                "Reach Display",
                "Highlights your current vanilla crosshair target. Hitbox mode recolors Minecraft's built-in F3+B hitbox and only works while hitboxes are enabled; Crosshair Color works independently.",
                Category.PVP
        );

        addSetting(displayMode);
        addSetting(allEntities);
        addSetting(ignoreSpectators);
        addSetting(highlightColor);
    }

    @Override
    public boolean isSettingVisible(Setting<?> setting) {
        return true;
    }

    public boolean accepts(class_1297 target) {
        if (target == null || !target.method_5805()) return false;
        if (!allEntities.get() && !(target instanceof class_1657)) return false;
        return !ignoreSpectators.get() || !target.method_7325();
    }

    public boolean usesHitbox() {
        DisplayMode mode = displayMode.get();
        return mode == DisplayMode.HITBOX || mode == DisplayMode.BOTH;
    }

    public boolean usesCrosshairColor() {
        DisplayMode mode = displayMode.get();
        return mode == DisplayMode.CROSSHAIR_COLOR || mode == DisplayMode.BOTH;
    }

    public EnumSetting<DisplayMode> getDisplayMode() { return displayMode; }
    public BooleanSetting getAllEntities() { return allEntities; }
    public BooleanSetting getIgnoreSpectators() { return ignoreSpectators; }
    public ColorSetting getHighlightColor() { return highlightColor; }
}
