package dev.yogi.yogiessentials.client.mixin;

import dev.yogi.yogiessentials.client.YogiEssentialsClient;
import dev.yogi.yogiessentials.client.module.smp.FogCustomizerModule;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.nio.ByteBuffer;
import net.minecraft.class_310;
import net.minecraft.class_758;

/**
 * Intercepts the final 1.21.11 fog UBO write. This is intentionally later than
 * all vanilla FogModifier calculations, so water/lava/snow/status/dimension
 * fog cannot overwrite the user's setting afterward.
 */
@Mixin(class_758.class)
public abstract class FogRendererMixin {

    @Shadow
    private void applyFog(
            ByteBuffer buffer, int bufPos, Vector4f fogColor,
            float environmentalStart, float environmentalEnd,
            float renderDistanceStart, float renderDistanceEnd,
            float skyEnd, float cloudEnd
    ) {
        throw new AssertionError();
    }

    @Redirect(
            method = "applyFog(Lnet/minecraft/client/render/Camera;ILnet/minecraft/client/render/RenderTickCounter;FLnet/minecraft/client/world/ClientWorld;)Lorg/joml/Vector4f;",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/fog/FogRenderer;applyFog(Ljava/nio/ByteBuffer;ILorg/joml/Vector4f;FFFFFF)V"
            )
    )
    private void yogiessentials$writeCustomizedFog(
            class_758 instance,
            ByteBuffer buffer, int bufPos, Vector4f fogColor,
            float environmentalStart, float environmentalEnd,
            float renderDistanceStart, float renderDistanceEnd,
            float skyEnd, float cloudEnd
    ) {
        FogCustomizerModule module = module();
        class_310 client = class_310.method_1551();

        if (module != null && module.isEnabled() && client.field_1687 != null) {
            FogCustomizerModule.FogSettings settings =
                    module.getSettings(client.field_1773.method_19418(), client.field_1687);

            float clarity = switch (settings.profile()) {
                case OVERWORLD, END -> clamp01(settings.dimensionMultiplier());
                case NETHER, WATER, LAVA, POWDER_SNOW, BLINDNESS, DARKNESS ->
                        clamp01(settings.fogOffset());
                default -> 0.0F;
            };

            if (clarity > 0.0001F) {
                final float clearEnd = 1_000_000.0F;
                final float clearStart = 999_000.0F;
                environmentalStart = lerp(environmentalStart, clearStart, clarity);
                environmentalEnd = lerp(environmentalEnd, clearEnd, clarity);
                renderDistanceStart = lerp(renderDistanceStart, clearStart, clarity);
                renderDistanceEnd = lerp(renderDistanceEnd, clearEnd, clarity);
                skyEnd = lerp(skyEnd, clearEnd, clarity);
                cloudEnd = lerp(cloudEnd, clearEnd, clarity);
            }
        }

        applyFog(buffer, bufPos, fogColor,
                environmentalStart, environmentalEnd,
                renderDistanceStart, renderDistanceEnd, skyEnd, cloudEnd);
    }

    private static float lerp(float vanilla, float clear, float amount) {
        return vanilla + (clear - vanilla) * amount;
    }

    private static float clamp01(float value) {
        return Math.max(0.0F, Math.min(1.0F, value));
    }

    private static FogCustomizerModule module() {
        if (YogiEssentialsClient.getModuleManager() == null) return null;
        return YogiEssentialsClient.getModuleManager().getModule(FogCustomizerModule.class);
    }
}
