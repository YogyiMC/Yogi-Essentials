package dev.yogi.yogiessentials.client.module.pvp;

import dev.yogi.yogiessentials.client.module.Category;
import dev.yogi.yogiessentials.client.module.Module;
import dev.yogi.yogiessentials.client.setting.BooleanSetting;
import dev.yogi.yogiessentials.client.setting.NumberSetting;
import dev.yogi.yogiessentials.client.util.MotionBlurManager;

/** Temporal frame blending for smoother-looking camera motion. */
public final class MotionBlurModule extends Module {
    private final NumberSetting strength =
            new NumberSetting("Strength (%)", 35.0, 0.0, 99.0, 1.0);
    private final BooleanSetting inMenus =
            new BooleanSetting("Blur In Menus", false);

    public MotionBlurModule() {
        super(
                "Motion Blur",
                "Blends the previous rendered frame into the current frame.",
                Category.PVP
        );
        addSetting(strength);
        addSetting(inMenus);
    }

    public NumberSetting getStrength() {
        return strength;
    }

    public BooleanSetting getInMenus() {
        return inMenus;
    }

    @Override
    protected void onEnable() {
        MotionBlurManager.resetHistory();
    }

    @Override
    protected void onDisable() {
        MotionBlurManager.resetHistory();
    }
}
