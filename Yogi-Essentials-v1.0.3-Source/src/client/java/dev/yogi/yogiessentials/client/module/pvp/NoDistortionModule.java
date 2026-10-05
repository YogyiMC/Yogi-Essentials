package dev.yogi.yogiessentials.client.module.pvp;

import dev.yogi.yogiessentials.client.module.Category;
import dev.yogi.yogiessentials.client.module.Module;
import net.minecraft.class_310;
import net.minecraft.class_315;

/**
 * Owns Minecraft's built-in distortion-effect strength while enabled.
 * This uses the native option instead of a fragile render-method injection, so
 * portal/nausea-style distortion is removed consistently and the user's prior
 * value is restored when the module is disabled.
 */
public class NoDistortionModule extends Module {

    private Double previousStrength;

    public NoDistortionModule() {
        super(
                "Distortion Effects",
                "Removes first-person distortion effects such as portal-style screen warping.",
                Category.PVP
        );
    }

    @Override
    protected void onEnable() {
        class_315 options = options();
        if (options == null) return;
        if (previousStrength == null) {
            previousStrength = options.method_42453().method_41753();
        }
        options.method_42453().method_41748(0.0);
    }

    @Override
    protected void onDisable() {
        class_315 options = options();
        if (options != null && previousStrength != null) {
            options.method_42453().method_41748(previousStrength);
        }
        previousStrength = null;
    }

    private static class_315 options() {
        class_310 client = class_310.method_1551();
        return client == null ? null : client.field_1690;
    }
}
