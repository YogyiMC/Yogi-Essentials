package dev.yogi.yogiessentials.client.module.pvp;

import dev.yogi.yogiessentials.client.setting.NumberSetting;

public class LowHealthWarningModule extends WarningModule {

    private final NumberSetting threshold =
            new NumberSetting(
                    "Health Threshold",
                    6.0,
                    1.0,
                    20.0,
                    0.5
            );

    public LowHealthWarningModule() {
        super(
                "Low Health Warning",
                "Warns when your health reaches the configured heart amount.",
                0.32
        );

        addSetting(threshold);
    }

    public NumberSetting getThreshold() {
        return threshold;
    }
}
