package dev.yogi.yogiessentials.client.module.hud;

import dev.yogi.yogiessentials.client.setting.BooleanSetting;
import dev.yogi.yogiessentials.client.setting.EnumSetting;
import dev.yogi.yogiessentials.client.setting.NumberSetting;
import dev.yogi.yogiessentials.client.setting.Setting;

public class TotemHudModule extends StyledHudModule {

    public enum DisplayMode {
        TEXT,
        ICON_COUNT,
        ICON_OVERLAY_COUNT
    }

    private final EnumSetting<DisplayMode> displayMode =
            new EnumSetting<>(
                    "Display Mode",
                    DisplayMode.ICON_COUNT,
                    DisplayMode.class
            );

    private final NumberSetting iconSize =
            new NumberSetting(
                    "Icon Size",
                    18.0,
                    12.0,
                    30.0,
                    1.0
            );

    private final BooleanSetting textBackground =
            new BooleanSetting("Text Background", true);

    public TotemHudModule() {
        super(
                "Totem Counter",
                "Shows your totem count as text or as a compact icon with count.",
                0.01,
                0.90
        );

        addSetting(displayMode);
        addSetting(iconSize);
        addSetting(textBackground);
    }

    @Override
    public boolean isSettingVisible(
            Setting<?> setting
    ) {
        if (setting == iconSize) {
            return displayMode.get() != DisplayMode.TEXT;
        }

        if (setting == textBackground) {
            return displayMode.get() == DisplayMode.TEXT;
        }

        return super.isSettingVisible(
                setting
        );
    }

    public EnumSetting<DisplayMode> getDisplayMode() {
        return displayMode;
    }

    public NumberSetting getIconSize() {
        return iconSize;
    }

    public BooleanSetting getTextBackground() {
        return textBackground;
    }
}
