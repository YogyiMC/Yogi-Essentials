package dev.yogi.yogiessentials.client.module.smp;

import dev.yogi.yogiessentials.client.YogiEssentialsClient;
import dev.yogi.yogiessentials.client.module.Category;
import dev.yogi.yogiessentials.client.module.Module;
import dev.yogi.yogiessentials.client.setting.PercentSetting;
import dev.yogi.yogiessentials.client.setting.SectionSetting;
import net.minecraft.class_1294;
import net.minecraft.class_1297;
import net.minecraft.class_1309;
import net.minecraft.class_1937;
import net.minecraft.class_4184;
import net.minecraft.class_5636;
import net.minecraft.class_638;
import net.minecraft.class_7285;







public final class FogCustomizerModule extends Module {

    public enum FogProfile {
        OVERWORLD,
        NETHER,
        END,
        WATER,
        LAVA,
        POWDER_SNOW,
        BLINDNESS,
        DARKNESS,
        OTHER
    }

    



    public record FogSettings(
            FogProfile profile,
            float dimensionMultiplier,
            float fogOffset
    ) {
    }

    private final PercentSetting overworldFogMultiplier =
            percent("Overworld Fog Multiplier", 1.0);

    
    private final PercentSetting netherFogOffset =
            percent("Nether Fog Offset", 1.0);

    private final PercentSetting endFogMultiplier =
            percent("End Fog Multiplier", 1.0);

    private final PercentSetting waterFogOffset =
            percent("Water Fog Offset", 0.0);

    private final PercentSetting lavaFogOffset =
            percent("Lava Fog Offset", 1.0);

    private final PercentSetting powderSnowFogOffset =
            percent("Powder Snow Fog Offset", 1.0);

    private final PercentSetting blindnessFogOffset =
            percent("Blindness Fog Offset", 0.0);

    private final PercentSetting darknessFogOffset =
            percent("Darkness Fog Offset", 0.0);

    public FogCustomizerModule() {
        super(
                "Fog Customizer",
                "Adjust dimension, fluid, and status-effect fog with simple percentage controls for clearer visibility.",
                Category.SMP
        );

        addSetting(new SectionSetting("Dimension Fog Multiplier"));
        addSetting(overworldFogMultiplier);
        addSetting(netherFogOffset);
        addSetting(endFogMultiplier);

        addSetting(new SectionSetting("Fluid Fog Offset"));
        addSetting(waterFogOffset);
        addSetting(lavaFogOffset);
        addSetting(powderSnowFogOffset);

        addSetting(new SectionSetting("Status Effect Fog Offset"));
        addSetting(blindnessFogOffset);
        addSetting(darknessFogOffset);
    }

    public FogSettings getSettings(class_4184 camera, class_638 world) {
        FogProfile profile = resolveProfile(camera, world);

        return switch (profile) {
            case OVERWORLD -> new FogSettings(
                    profile,
                    overworldFogMultiplier.get().floatValue(),
                    0.0F
            );
            case NETHER -> new FogSettings(
                    profile,
                    1.0F,
                    netherFogOffset.get().floatValue()
            );
            case END -> new FogSettings(
                    profile,
                    endFogMultiplier.get().floatValue(),
                    0.0F
            );
            case WATER -> new FogSettings(
                    profile,
                    1.0F,
                    waterFogOffset.get().floatValue()
            );
            case LAVA -> new FogSettings(
                    profile,
                    1.0F,
                    lavaFogOffset.get().floatValue()
            );
            case POWDER_SNOW -> new FogSettings(
                    profile,
                    1.0F,
                    powderSnowFogOffset.get().floatValue()
            );
            case BLINDNESS -> new FogSettings(
                    profile,
                    1.0F,
                    blindnessFogOffset.get().floatValue()
            );
            case DARKNESS -> new FogSettings(
                    profile,
                    1.0F,
                    darknessFogOffset.get().floatValue()
            );
            case OTHER -> new FogSettings(profile, 1.0F, 0.0F);
        };
    }

    





    public void applyToFogData(FogProfile profile, class_7285 data) {
        if (!isEnabled() || data == null || profile == null) return;

        float amount = getAmount(profile);
        if (amount <= 0.0001F) return;

        switch (profile) {
            case OVERWORLD, END -> {
                
                
                float multiplier = 1.0F + (float) Math.pow(amount, 4.0) * 4.0F;
                data.field_60582 *= multiplier;
                data.field_60584 *= multiplier;
                data.field_60583 *= multiplier;
                data.field_60585 *= multiplier;
            }
            case NETHER -> {
                
                float offset = squaredOffset(amount, 800.0F);
                data.field_60582 += offset * 0.5F;
                data.field_60584 += offset;
                data.field_60583 += offset * 0.5F;
                data.field_60585 += offset;
            }
            case WATER -> pushEnd(data, squaredOffset(amount, 1000.0F), 1000.0F);
            case LAVA -> pushEnd(data, squaredOffset(amount, 200.0F), 200.0F);
            case POWDER_SNOW -> pushEnd(data, squaredOffset(amount, 120.0F), 120.0F);
            case BLINDNESS, DARKNESS -> pushEnd(data, squaredOffset(amount, 120.0F), 120.0F);
            case OTHER -> {
            }
        }
    }

    public float getAmount(FogProfile profile) {
        if (profile == null) return 0.0F;
        double value = switch (profile) {
            case OVERWORLD -> overworldFogMultiplier.get();
            case NETHER -> netherFogOffset.get();
            case END -> endFogMultiplier.get();
            case WATER -> waterFogOffset.get();
            case LAVA -> lavaFogOffset.get();
            case POWDER_SNOW -> powderSnowFogOffset.get();
            case BLINDNESS -> blindnessFogOffset.get();
            case DARKNESS -> darknessFogOffset.get();
            case OTHER -> 0.0;
        };
        return (float) Math.max(0.0, Math.min(1.0, value));
    }

    public static FogCustomizerModule enabledInstance() {
        if (YogiEssentialsClient.getModuleManager() == null) return null;
        FogCustomizerModule module = YogiEssentialsClient.getModuleManager().getModule(FogCustomizerModule.class);
        return module != null && module.isEnabled() ? module : null;
    }

    private static float squaredOffset(float amount, float maxDistance) {
        return amount * amount * maxDistance;
    }

    private static void pushEnd(class_7285 data, float offset, float maxDistance) {
        data.field_60584 = Math.min(data.field_60584 + offset, maxDistance);
        data.field_60585 = Math.min(data.field_60585 + offset, maxDistance);
    }

    public FogProfile resolveProfile(class_4184 camera, class_638 world) {
        class_5636 submersion = camera == null
                ? class_5636.field_27888
                : camera.method_19334();

        
        if (submersion == class_5636.field_27886) {
            return FogProfile.WATER;
        }
        if (submersion == class_5636.field_27885) {
            return FogProfile.LAVA;
        }
        if (submersion == class_5636.field_27887) {
            return FogProfile.POWDER_SNOW;
        }

        class_1297 focused = camera == null ? null : camera.method_19331();
        if (focused instanceof class_1309 living) {
            if (living.method_6059(class_1294.field_5919)) {
                return FogProfile.BLINDNESS;
            }
            if (living.method_6059(class_1294.field_38092)) {
                return FogProfile.DARKNESS;
            }
        }

        if (world != null) {
            if (class_1937.field_25180.equals(world.method_27983())) {
                return FogProfile.NETHER;
            }
            if (class_1937.field_25181.equals(world.method_27983())) {
                return FogProfile.END;
            }
            if (class_1937.field_25179.equals(world.method_27983())) {
                return FogProfile.OVERWORLD;
            }
        }

        return FogProfile.OTHER;
    }

    private static PercentSetting percent(String name, double defaultValue) {
        return new PercentSetting(
                name,
                defaultValue,
                0.0,
                1.0,
                0.01
        );
    }
}
