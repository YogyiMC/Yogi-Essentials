package dev.yogi.yogiessentials.client.module.hud;

import dev.yogi.yogiessentials.client.setting.BooleanSetting;
import dev.yogi.yogiessentials.client.setting.ColorSetting;
import dev.yogi.yogiessentials.client.setting.EnumSetting;
import dev.yogi.yogiessentials.client.setting.NumberSetting;
import dev.yogi.yogiessentials.client.setting.Setting;

public class PotionEffectsHudModule extends StyledHudModule {

    public enum DisplayMode {
        ICONS,
        TEXT,
        ICON_TIMER,
        ICON_NAME_TIMER
    }

    public enum Layout {
        HORIZONTAL,
        VERTICAL
    }

    private final EnumSetting<DisplayMode> displayMode =
            new EnumSetting<>("Display Mode", DisplayMode.ICONS, DisplayMode.class);

    private final BooleanSetting autoLayout =
            new BooleanSetting("Auto Icon Layout", true);

    private final EnumSetting<Layout> layout =
            new EnumSetting<>("Locked Icon Layout", Layout.HORIZONTAL, Layout.class);

    private final BooleanSetting showAmplifier = new BooleanSetting("Show Level", true);
    private final BooleanSetting showTimer = new BooleanSetting("Show Timer", true);
    private final ColorSetting effectTextColor = new ColorSetting("Effect Text Color", 0xFFF4F4F5);
    private final NumberSetting rowSpacing = new NumberSetting("Row Spacing", 18.0, 12.0, 28.0, 1.0);
    private final NumberSetting iconSize = new NumberSetting("Icon Size", 18.0, 10.0, 28.0, 1.0);
    private final NumberSetting iconSpacing = new NumberSetting("Icon Spacing", 3.0, 0.0, 12.0, 1.0);
    private final BooleanSetting textBackground = new BooleanSetting("Text Background", true);

    public PotionEffectsHudModule() {
        super(
                "Potion Timers",
                "Shows active potion effects using text or clean Minecraft effect icons.",
                0.82,
                0.35
        );

        addSetting(displayMode);
        addSetting(autoLayout);
        addSetting(layout);
        addSetting(showAmplifier);
        addSetting(showTimer);
        addSetting(effectTextColor);
        addSetting(rowSpacing);
        addSetting(iconSize);
        addSetting(iconSpacing);
        addSetting(textBackground);
    }

    @Override
    public boolean isSettingVisible(Setting<?> setting) {
        DisplayMode mode = displayMode.get();

        if (setting == textBackground) {
            return mode == DisplayMode.TEXT;
        }

        if (setting == getCompactMode()) {
            return false;
        }

        if (setting == autoLayout) {
            return mode == DisplayMode.ICONS;
        }

        if (setting == layout) {
            return mode == DisplayMode.ICONS && !autoLayout.get();
        }

        if (mode == DisplayMode.ICONS) {
            if (
                    setting == showAmplifier
                            || setting == rowSpacing
                            || setting == getLabelColor()
                            || setting == getShowLabel()
                            || setting == getShowColon()
                            || setting == getShowValue()
                            || setting == getLabelOffsetX()
                            || setting == getLabelOffsetY()
                            || setting == getValueOffsetX()
                            || setting == getValueOffsetY()
            ) {
                return false;
            }

            
            if (setting == getValueColor()) {
                return showTimer.get();
            }

            if (setting == effectTextColor) {
                return false;
            }

            return super.isSettingVisible(setting);
        }

        if (setting == iconSpacing) {
            return false;
        }

        if (setting == iconSize) {
            return mode != DisplayMode.TEXT;
        }

        if (setting == showAmplifier) {
            return mode != DisplayMode.ICON_TIMER;
        }

        if (setting == effectTextColor) {
            return mode == DisplayMode.TEXT || mode == DisplayMode.ICON_NAME_TIMER;
        }

        return super.isSettingVisible(setting);
    }

    public EnumSetting<DisplayMode> getDisplayMode() { return displayMode; }
    public BooleanSetting getAutoLayout() { return autoLayout; }
    public EnumSetting<Layout> getLayout() { return layout; }
    public BooleanSetting getShowAmplifier() { return showAmplifier; }
    public BooleanSetting getShowTimer() { return showTimer; }
    public BooleanSetting getIconTimerShadow() { return getTextShadow(); }
    public ColorSetting getEffectTextColor() { return effectTextColor; }
    public NumberSetting getRowSpacing() { return rowSpacing; }
    public NumberSetting getIconSize() { return iconSize; }
    public NumberSetting getIconSpacing() { return iconSpacing; }
    public BooleanSetting getTextBackground() { return textBackground; }
}
