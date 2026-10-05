package dev.yogi.yogiessentials.client.module.hud;

import dev.yogi.yogiessentials.client.setting.EnumSetting;
import dev.yogi.yogiessentials.client.setting.Setting;

public class AttackCooldownHudModule extends StyledHudModule {

    public enum DisplayMode {
        PERCENT,
        BAR,
        BAR_PERCENT
    }

    private final EnumSetting<DisplayMode> displayMode =
            new EnumSetting<>(
                    "Display Mode",
                    DisplayMode.BAR_PERCENT,
                    DisplayMode.class
            );

    public AttackCooldownHudModule() {
        super(
                "Attack Cooldown",
                "Shows your vanilla attack cooldown as a customizable HUD.",
                0.50,
                0.78
        );

        addSetting(displayMode);
    }

    @Override
    public boolean isSettingVisible(
            Setting<?> setting
    ) {
        if (
                setting
                        == getCompactMode()
        ) {
            return false;
        }

        return super.isSettingVisible(
                setting
        );
    }

    public EnumSetting<DisplayMode> getDisplayMode() {
        return displayMode;
    }
}
