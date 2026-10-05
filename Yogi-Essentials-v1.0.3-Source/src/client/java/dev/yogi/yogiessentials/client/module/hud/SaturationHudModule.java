package dev.yogi.yogiessentials.client.module.hud;

import dev.yogi.yogiessentials.client.module.Category;
import dev.yogi.yogiessentials.client.module.Module;
import dev.yogi.yogiessentials.client.setting.BooleanSetting;
import dev.yogi.yogiessentials.client.setting.ColorSetting;
import dev.yogi.yogiessentials.client.setting.NumberSetting;





public final class SaturationHudModule extends Module {

    private final ColorSetting overlayColor =
            new ColorSetting(
                    "Overlay Color",
                    0xFFFFC928
            );

    private final NumberSetting opacity =
            new NumberSetting(
                    "Opacity",
                    0.78,
                    0.10,
                    1.00,
                    0.05
            );

    private final BooleanSetting avoidAppleSkinDuplicate =
            new BooleanSetting(
                    "Hide When AppleSkin Is Installed",
                    true
            );

    public SaturationHudModule() {
        super(
                "Saturation",
                "Shows your real saturation over the vanilla hunger bar.",
                Category.HUD
        );

        addSetting(overlayColor);
        addSetting(opacity);
        addSetting(avoidAppleSkinDuplicate);
    }

    public ColorSetting getOverlayColor() {
        return overlayColor;
    }

    public NumberSetting getOpacity() {
        return opacity;
    }

    public BooleanSetting getAvoidAppleSkinDuplicate() {
        return avoidAppleSkinDuplicate;
    }
}
