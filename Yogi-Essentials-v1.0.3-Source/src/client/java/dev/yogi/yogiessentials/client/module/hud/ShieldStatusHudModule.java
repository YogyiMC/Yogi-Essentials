package dev.yogi.yogiessentials.client.module.hud;

import dev.yogi.yogiessentials.client.setting.EnumSetting;
import dev.yogi.yogiessentials.client.setting.NumberSetting;
import dev.yogi.yogiessentials.client.setting.Setting;

public class ShieldStatusHudModule extends StyledHudModule {

    public enum DisplayMode {
        TEXT,
        ICON_TEXT
    }

    private final EnumSetting<DisplayMode> displayMode =
            new EnumSetting<>(
                    "Display Mode",
                    DisplayMode.ICON_TEXT,
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

    public ShieldStatusHudModule() {
        super(
                "Shield Status",
                "Shows shield state as text or as an icon with state text.",
                0.50,
                0.72
        );

        addSetting(displayMode);
        addSetting(iconSize);
    }

    @Override
    public boolean isSettingVisible(
            Setting<?> setting
    ) {
        if (
                setting == iconSize
        ) {
            return displayMode.get()
                    == DisplayMode.ICON_TEXT;
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
}
