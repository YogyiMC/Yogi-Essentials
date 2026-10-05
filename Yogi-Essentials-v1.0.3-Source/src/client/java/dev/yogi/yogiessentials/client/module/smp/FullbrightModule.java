package dev.yogi.yogiessentials.client.module.smp;

import dev.yogi.yogiessentials.client.mixin.LightmapTextureManagerAccessor;
import dev.yogi.yogiessentials.client.module.Category;
import dev.yogi.yogiessentials.client.module.Module;
import dev.yogi.yogiessentials.client.setting.BooleanSetting;
import dev.yogi.yogiessentials.client.setting.PercentSetting;
import net.minecraft.class_1937;
import net.minecraft.class_310;
import net.minecraft.class_638;
import net.minecraft.class_765;
import dev.yogi.yogiessentials.client.setting.KeybindSetting;

import org.lwjgl.glfw.GLFW;






public final class FullbrightModule extends Module {

    private final KeybindSetting toggleKey =
            new KeybindSetting("Toggle Keybind", GLFW.GLFW_KEY_B);

    private final PercentSetting brightnessStrength =
            new RefreshingPercentSetting(
                    "Brightness Strength",
                    1.0,
                    0.0,
                    1.0,
                    0.01
            );

    private final BooleanSetting overworld =
            new RefreshingBooleanSetting("Overworld", true);

    private final BooleanSetting nether =
            new RefreshingBooleanSetting("Nether", true);

    private final BooleanSetting end =
            new RefreshingBooleanSetting("End", true);

    private final BooleanSetting customDimensions =
            new RefreshingBooleanSetting("Custom Dimensions", true);

    public FullbrightModule() {
        super(
                "Fullbright",
                "Brightens dark areas through the client lightmap without changing Minecraft's gamma setting.",
                Category.SMP
        );

        addSetting(toggleKey);
        addSetting(brightnessStrength);
        addSetting(overworld);
        addSetting(nether);
        addSetting(end);
        addSetting(customDimensions);
    }

    





    public float applyLightmapGamma(float vanillaGamma) {
        if (!isEnabled() || !isEnabledForCurrentDimension()) {
            return vanillaGamma;
        }

        float strength = Math.max(0.0F, Math.min(1.0F, brightnessStrength.get().floatValue()));
        float fullbrightGamma = 16.0F;
        return vanillaGamma + (fullbrightGamma - vanillaGamma) * strength;
    }

    public KeybindSetting getToggleKey() { return toggleKey; }
    public PercentSetting getBrightnessStrength() { return brightnessStrength; }

    @Override
    protected void onEnable() {
        refreshLightmap();
    }

    @Override
    protected void onDisable() {
        refreshLightmap();
    }

    @Override
    protected void onSettingsReset() {
        refreshLightmap();
    }

    private boolean isEnabledForCurrentDimension() {
        class_310 client = class_310.method_1551();
        class_638 world = client == null ? null : client.field_1687;

        if (world == null) {
            return true;
        }

        if (class_1937.field_25179.equals(world.method_27983())) {
            return overworld.get();
        }
        if (class_1937.field_25180.equals(world.method_27983())) {
            return nether.get();
        }
        if (class_1937.field_25181.equals(world.method_27983())) {
            return end.get();
        }

        return customDimensions.get();
    }

    private static void refreshLightmap() {
        try {
            class_310 client = class_310.method_1551();
            if (client == null || client.field_1773 == null) {
                return;
            }

            class_765 lightmap =
                    client.field_1773.method_22974();

            if (lightmap instanceof LightmapTextureManagerAccessor accessor) {
                accessor.yogiessentials$setDirty(true);
            }
        } catch (Throwable ignored) {
            
        }
    }

    private static final class RefreshingPercentSetting extends PercentSetting {
        private RefreshingPercentSetting(
                String name,
                double defaultValue,
                double min,
                double max,
                double step
        ) {
            super(name, defaultValue, min, max, step);
        }

        @Override
        public void set(Double value) {
            super.set(value);
            refreshLightmap();
        }
    }

    private static final class RefreshingBooleanSetting extends BooleanSetting {
        private RefreshingBooleanSetting(String name, boolean defaultValue) {
            super(name, defaultValue);
        }

        @Override
        public void set(Boolean value) {
            super.set(value);
            refreshLightmap();
        }
    }
}
