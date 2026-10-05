package dev.yogi.yogiessentials.client.module.pvp;

import dev.yogi.yogiessentials.client.setting.NumberSetting;

public class LowTotemWarningModule extends WarningModule {

    private final NumberSetting threshold =
            new NumberSetting(
                    "Totem Threshold",
                    2.0,
                    0.0,
                    10.0,
                    1.0
            );

    public LowTotemWarningModule() {
        super(
                "Low Totem Warning",
                "Warns when your total totem count reaches the configured amount.",
                0.25
        );

        addSetting(threshold);
    }

    public NumberSetting getThreshold() {
        return threshold;
    }
}
