package dev.yogi.yogiessentials.client.module.pvp;

import dev.yogi.yogiessentials.client.setting.NumberSetting;

public class EffectExpiryWarningModule extends WarningModule {

    private final NumberSetting seconds =
            new NumberSetting(
                    "Warn Before",
                    8.0,
                    1.0,
                    30.0,
                    1.0
            );

    public EffectExpiryWarningModule() {
        super(
                "Effect Expiry Warning",
                "Warns shortly before one of your active effects expires.",
                0.39
        );

        addSetting(seconds);
    }

    public NumberSetting getSeconds() {
        return seconds;
    }
}
