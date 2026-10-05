package dev.yogi.yogiessentials.client.module.hud;

import dev.yogi.yogiessentials.client.setting.BooleanSetting;
import dev.yogi.yogiessentials.client.setting.ColorSetting;
import dev.yogi.yogiessentials.client.setting.EnumSetting;
import dev.yogi.yogiessentials.client.setting.NumberSetting;
import dev.yogi.yogiessentials.client.setting.Setting;

public class ArmorHudModule extends StyledHudModule {

    public enum DisplayMode {
        TEXT,
        ICONS_PERCENT,
        ICONS_ONLY,
        PERCENT_ONLY
    }

    public enum Layout {
        VERTICAL,
        HORIZONTAL
    }

    private final EnumSetting<DisplayMode> displayMode =
            new EnumSetting<>("Display Mode", DisplayMode.ICONS_PERCENT, DisplayMode.class);

    private final BooleanSetting autoLayout =
            new BooleanSetting("Auto Layout", true);

    private final EnumSetting<Layout> layout =
            new EnumSetting<>("Locked Layout", Layout.VERTICAL, Layout.class);

    private final NumberSetting iconSize =
            new NumberSetting("Icon Size", 18.0, 12.0, 30.0, 1.0);

    private final NumberSetting iconSpacing =
            new NumberSetting("Icon Spacing", 3.0, 0.0, 12.0, 1.0);

    private final BooleanSetting showTools =
            new BooleanSetting("Show Tools", false);

    private final BooleanSetting showOffhand =
            new BooleanSetting("Show Offhand", false);

    private final BooleanSetting individualArmorPositions =
            new BooleanSetting("Individual Item Positions", false);

    private final NumberSetting[] pieceOffsetX = new NumberSetting[] {
            new NumberSetting("Helmet X", 0.0, -4096.0, 4096.0, 1.0),
            new NumberSetting("Chestplate X", 0.0, -4096.0, 4096.0, 1.0),
            new NumberSetting("Leggings X", 0.0, -4096.0, 4096.0, 1.0),
            new NumberSetting("Boots X", 0.0, -4096.0, 4096.0, 1.0),
            new NumberSetting("Tool X", 0.0, -4096.0, 4096.0, 1.0),
            new NumberSetting("Offhand X", 0.0, -4096.0, 4096.0, 1.0)
    };

    private final NumberSetting[] pieceOffsetY = new NumberSetting[] {
            new NumberSetting("Helmet Y", 0.0, -4096.0, 4096.0, 1.0),
            new NumberSetting("Chestplate Y", 0.0, -4096.0, 4096.0, 1.0),
            new NumberSetting("Leggings Y", 0.0, -4096.0, 4096.0, 1.0),
            new NumberSetting("Boots Y", 0.0, -4096.0, 4096.0, 1.0),
            new NumberSetting("Tool Y", 0.0, -4096.0, 4096.0, 1.0),
            new NumberSetting("Offhand Y", 0.0, -4096.0, 4096.0, 1.0)
    };


    private final NumberSetting[] pieceScale = new NumberSetting[] {
            new NumberSetting("Helmet Scale", 1.0, 0.25, 4.0, 0.01),
            new NumberSetting("Chestplate Scale", 1.0, 0.25, 4.0, 0.01),
            new NumberSetting("Leggings Scale", 1.0, 0.25, 4.0, 0.01),
            new NumberSetting("Boots Scale", 1.0, 0.25, 4.0, 0.01),
            new NumberSetting("Tool Scale", 1.0, 0.25, 4.0, 0.01),
            new NumberSetting("Offhand Scale", 1.0, 0.25, 4.0, 0.01)
    };

    private final BooleanSetting showDurabilityBar =
            new BooleanSetting("Durability Bar", true);

    private final ColorSetting durabilityBarColor =
            new ColorSetting("Durability Bar Color", 0xFF35C759);

    private final ColorSetting lowDurabilityBarColor =
            new ColorSetting("Low Durability Color", 0xFFFF4D4D);

    private final NumberSetting lowDurabilityThreshold =
            new NumberSetting("Low Durability Threshold", 50.0, 1.0, 99.0, 1.0);

    public ArmorHudModule() {
        super(
                "Armor Durability",
                "Shows armor durability with optional held-tool and offhand displays using the same HUD style.",
                0.90,
                0.02
        );

        addSetting(displayMode);
        addSetting(autoLayout);
        addSetting(layout);
        addSetting(iconSize);
        addSetting(iconSpacing);
        addSetting(showTools);
        addSetting(showOffhand);
        addSetting(individualArmorPositions);
        for (NumberSetting setting : pieceOffsetX) addSetting(setting);
        for (NumberSetting setting : pieceOffsetY) addSetting(setting);
        for (NumberSetting setting : pieceScale) addSetting(setting);
        addSetting(showDurabilityBar);
        addSetting(durabilityBarColor);
        addSetting(lowDurabilityBarColor);
        addSetting(lowDurabilityThreshold);
    }

    @Override
    public boolean isSettingVisible(Setting<?> setting) {
        if (setting == getCompactMode()) {
            return false;
        }

        DisplayMode mode = displayMode.get();
        boolean iconMode = isIconMode();
        boolean percentageMode = mode == DisplayMode.ICONS_PERCENT || mode == DisplayMode.PERCENT_ONLY;

        if (setting == autoLayout) {
            return mode != DisplayMode.TEXT;
        }

        if (setting == layout) {
            return mode != DisplayMode.TEXT && !autoLayout.get();
        }

        if (setting == iconSize || setting == iconSpacing || setting == individualArmorPositions) {
            return iconMode;
        }

        for (int i = 0; i < 6; i++) {
            if (setting == pieceOffsetX[i] || setting == pieceOffsetY[i] || setting == pieceScale[i]) {
                return false;
            }
        }

        if (setting == showDurabilityBar) {
            return iconMode;
        }

        if (setting == durabilityBarColor || setting == lowDurabilityBarColor || setting == lowDurabilityThreshold) {
            return iconMode && showDurabilityBar.get();
        }

        if (mode == DisplayMode.ICONS_ONLY && (
                setting == getShowValue()
                        || setting == getValueColor()
                        || setting == getValueOffsetX()
                        || setting == getValueOffsetY()
        )) {
            return false;
        }

        if (mode == DisplayMode.PERCENT_ONLY && (
                setting == getShowLabel()
                        || setting == getShowColon()
                        || setting == getLabelColor()
                        || setting == getLabelOffsetX()
                        || setting == getLabelOffsetY()
                        || setting == getShowValue()
                        || setting == getValueOffsetX()
                        || setting == getValueOffsetY()
        )) {
            return false;
        }

        if (!percentageMode && setting == getShowValue()) {
            return false;
        }

        return super.isSettingVisible(setting);
    }


    




    public void resetIndividualEditorLayout() {
        for (int i = 0; i < 6; i++) {
            pieceOffsetX[i].set(0.0);
            pieceOffsetY[i].set(0.0);
            pieceScale[i].set(1.0);
        }
    }

    public void enforceIndividualModeStyle() {
        if (!individualArmorPositions.get()) return;
        getBackground().set(false);
        getBorder().set(false);
        getAccentLine().set(false);
        getShowLabel().set(false);
    }

    public boolean isIconMode() {
        DisplayMode mode = displayMode.get();
        return mode == DisplayMode.ICONS_ONLY || mode == DisplayMode.ICONS_PERCENT;
    }

    public EnumSetting<DisplayMode> getDisplayMode() { return displayMode; }
    public BooleanSetting getAutoLayout() { return autoLayout; }
    public EnumSetting<Layout> getLayout() { return layout; }
    public NumberSetting getIconSize() { return iconSize; }
    public NumberSetting getIconSpacing() { return iconSpacing; }
    public BooleanSetting getShowTools() { return showTools; }
    public BooleanSetting getShowOffhand() { return showOffhand; }
    public BooleanSetting getIndividualArmorPositions() { return individualArmorPositions; }
    public BooleanSetting getShowDurabilityBar() { return showDurabilityBar; }
    public ColorSetting getDurabilityBarColor() { return durabilityBarColor; }
    public ColorSetting getLowDurabilityBarColor() { return lowDurabilityBarColor; }
    public NumberSetting getLowDurabilityThreshold() { return lowDurabilityThreshold; }

    public NumberSetting getPieceOffsetX(int index) { return pieceOffsetX[Math.max(0, Math.min(5, index))]; }
    public NumberSetting getPieceOffsetY(int index) { return pieceOffsetY[Math.max(0, Math.min(5, index))]; }
    public NumberSetting getPieceScale(int index) { return pieceScale[Math.max(0, Math.min(5, index))]; }

    public int getConfiguredItemCount() {
        return 4 + (showTools.get() ? 1 : 0) + (showOffhand.get() ? 1 : 0);
    }

    public int getPieceOffsetXValue(int index) {
        return individualArmorPositions.get() ? getPieceOffsetX(index).get().intValue() : 0;
    }

    public int getPieceOffsetYValue(int index) {
        return individualArmorPositions.get() ? getPieceOffsetY(index).get().intValue() : 0;
    }

    public void setPieceOffset(int index, double x, double y) {
        getPieceOffsetX(index).set(x);
        getPieceOffsetY(index).set(y);
    }

    public int getDurabilityBarColorForPercent(int percent) {
        return percent <= lowDurabilityThreshold.get().intValue()
                ? lowDurabilityBarColor.getArgb()
                : durabilityBarColor.getArgb();
    }
}
