package dev.yogi.yogiessentials.client.module.pvp;

import dev.yogi.yogiessentials.client.setting.NumberSetting;

public class WeaponDurabilityWarningModule extends WarningModule {

    private final NumberSetting threshold =
            new NumberSetting(
                    "Durability Threshold",
                    10.0,
                    1.0,
                    100.0,
                    1.0
            );

    public WeaponDurabilityWarningModule() {
        super(
                "Weapon Durability Warning",
                "Warns when your currently held damageable item is low.",
                0.53
        );

        addSetting(threshold);
    }

    public NumberSetting getThreshold() {
        return threshold;
    }
}
