package dev.yogi.yogiessentials.client.module.pvp;

import dev.yogi.yogiessentials.client.module.Category;
import dev.yogi.yogiessentials.client.module.Module;
import dev.yogi.yogiessentials.client.setting.NumberSetting;

public class NoFovEffectsModule extends Module {

    private final NumberSetting effectStrength =
            new NumberSetting(
                    "Effect Strength",
                    0.0,
                    0.0,
                    1.0,
                    0.05
            );

    public NoFovEffectsModule() {
        super(
                "FOV Effects",
                "Controls how strongly Minecraft's dynamic movement/item FOV effects are applied.",
                Category.PVP
        );

        addSetting(
                effectStrength
        );
    }

    public NumberSetting getEffectStrength() {
        return effectStrength;
    }
}
