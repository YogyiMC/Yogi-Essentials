package dev.yogi.yogiessentials.client.module.pvp;

import dev.yogi.yogiessentials.client.setting.NumberSetting;

public class ArmorBreakWarningModule extends WarningModule {

    private final NumberSetting threshold =
            new NumberSetting(
                    "Durability Threshold",
                    15.0,
                    1.0,
                    100.0,
                    1.0
            );

    public ArmorBreakWarningModule() {
        super(
                "Armor Break Warning",
                "Warns when any equipped armor piece reaches low durability.",
                0.18
        );

        addSetting(threshold);
    }

    public NumberSetting getThreshold() {
        return threshold;
    }
}
