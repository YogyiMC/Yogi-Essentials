package dev.yogi.yogiessentials.client.module.pvp;

import dev.yogi.yogiessentials.client.setting.EnumSetting;
import dev.yogi.yogiessentials.client.setting.NumberSetting;

public class FoodWarningModule extends WarningModule {

    public enum Mode {
        FOOD,
        SATURATION,
        EITHER
    }

    private final EnumSetting<Mode> mode =
            new EnumSetting<>(
                    "Warning Mode",
                    Mode.FOOD,
                    Mode.class
            );

    private final NumberSetting foodThreshold =
            new NumberSetting(
                    "Food Threshold",
                    6.0,
                    0.0,
                    20.0,
                    1.0
            );

    private final NumberSetting saturationThreshold =
            new NumberSetting(
                    "Saturation Threshold",
                    2.0,
                    0.0,
                    20.0,
                    0.5
            );

    public FoodWarningModule() {
        super(
                "Food / Saturation Warning",
                "Warns when your food or saturation reaches a configured threshold.",
                0.46
        );

        addSetting(mode);
        addSetting(foodThreshold);
        addSetting(saturationThreshold);
    }

    public EnumSetting<Mode> getMode() {
        return mode;
    }

    public NumberSetting getFoodThreshold() {
        return foodThreshold;
    }

    public NumberSetting getSaturationThreshold() {
        return saturationThreshold;
    }
}
