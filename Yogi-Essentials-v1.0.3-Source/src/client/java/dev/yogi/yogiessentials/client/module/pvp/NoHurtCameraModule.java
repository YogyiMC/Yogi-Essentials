package dev.yogi.yogiessentials.client.module.pvp;

import dev.yogi.yogiessentials.client.module.Category;
import dev.yogi.yogiessentials.client.module.Module;
import dev.yogi.yogiessentials.client.setting.NumberSetting;

public class NoHurtCameraModule extends Module {

    private final NumberSetting shakeStrength =
            new NumberSetting(
                    "Shake Strength",
                    0.0,
                    0.0,
                    1.0,
                    0.05
            );

    public NoHurtCameraModule() {
        super(
                "Hurt Camera",
                "Controls how strong the normal Minecraft hurt-camera shake is.",
                Category.PVP
        );

        addSetting(
                shakeStrength
        );
    }

    public NumberSetting getShakeStrength() {
        return shakeStrength;
    }
}
