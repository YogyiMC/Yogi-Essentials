package dev.yogi.yogiessentials.client.mixin;

import dev.yogi.yogiessentials.client.YogiEssentialsClient;
import dev.yogi.yogiessentials.client.module.pvp.NoFovEffectsModule;
import dev.yogi.yogiessentials.client.module.visual.ExtendedFovModule;
import dev.yogi.yogiessentials.client.util.ZoomManager;
import net.minecraft.class_310;
import net.minecraft.class_4184;
import net.minecraft.class_757;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(
        value = class_757.class,
        priority = 900
)
public abstract class FovEffectsStrengthMixin {

    @Inject(
            method = "getFov",
            at = @At("RETURN"),
            cancellable = true
    )
    private void yogi$scaleDynamicFovEffects(
            class_4184 camera,
            float tickProgress,
            boolean changingFov,
            CallbackInfoReturnable<Float> cir
    ) {
        if (!changingFov) {
            return;
        }

        float result = cir.getReturnValue();

        NoFovEffectsModule module =
                YogiEssentialsClient
                        .getModuleManager()
                        .getModule(
                                NoFovEffectsModule.class
                        );

        if (
                module != null
                        &&
                module.isEnabled()
        ) {
            class_310 client =
                    class_310.method_1551();

            float baseFov =
                    client
                            .field_1690
                            .method_41808()
                            .method_41753();

            ExtendedFovModule extended =
                    YogiEssentialsClient
                            .getModuleManager()
                            .getModule(
                                    ExtendedFovModule.class
                            );

            if (
                    extended != null
                            &&
                    extended.isEnabled()
            ) {
                baseFov =
                        extended
                                .getFov()
                                .get()
                                .floatValue();
            }

            float strength =
                    module
                            .getEffectStrength()
                            .get()
                            .floatValue();

            result =
                    baseFov
                            + (
                            result
                                    - baseFov
                    )
                            * strength;
        }

        result = ZoomManager.applyZoom(
                result,
                tickProgress
        );

        cir.setReturnValue(result);
    }
}
