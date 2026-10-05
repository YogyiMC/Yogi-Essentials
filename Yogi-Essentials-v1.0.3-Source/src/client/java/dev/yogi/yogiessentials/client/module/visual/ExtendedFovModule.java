package dev.yogi.yogiessentials.client.module.visual;

import dev.yogi.yogiessentials.client.module.Category;
import dev.yogi.yogiessentials.client.module.Module;
import dev.yogi.yogiessentials.client.setting.BooleanSetting;
import dev.yogi.yogiessentials.client.setting.NumberSetting;

public class ExtendedFovModule extends Module {

    private final NumberSetting fov =
            new NumberSetting(
                    "FOV",
                    110.0,
                    30.0,
                    170.0,
                    1.0
            );

    private final BooleanSetting preserveDynamicEffects =
            new BooleanSetting(
                    "Preserve Dynamic FOV",
                    true
            );

    public ExtendedFovModule() {
        super(
                "Extended FOV",
                "Allows a wider configurable first-person field of view than the vanilla slider.",
                Category.VISUAL
        );

        addSetting(
                fov
        );

        addSetting(
                preserveDynamicEffects
        );
    }

    public NumberSetting getFov() {
        return fov;
    }

    public BooleanSetting getPreserveDynamicEffects() {
        return preserveDynamicEffects;
    }
}
