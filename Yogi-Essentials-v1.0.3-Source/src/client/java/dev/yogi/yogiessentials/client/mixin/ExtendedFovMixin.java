package dev.yogi.yogiessentials.client.mixin;

import dev.yogi.yogiessentials.client.YogiEssentialsClient;
import dev.yogi.yogiessentials.client.module.visual.ExtendedFovModule;
import net.minecraft.class_310;
import net.minecraft.class_4184;
import net.minecraft.class_757;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(class_757.class)
public abstract class ExtendedFovMixin {

    @Shadow
    @Final
    private class_310 client;

    @Inject(
            method = "getFov",
            at = @At("RETURN"),
            cancellable = true
    )
    private void yogi$extendedFov(
            class_4184 camera,
            float tickProgress,
            boolean changingFov,
            CallbackInfoReturnable<Float> cir
    ) {
        if (!changingFov) {
            return;
        }

        if (
                YogiEssentialsClient
                        .getModuleManager()
                        == null
        ) {
            return;
        }

        ExtendedFovModule module =
                YogiEssentialsClient
                        .getModuleManager()
                        .getModule(
                                ExtendedFovModule.class
                        );

        if (
                module == null
                        ||
                !module.isEnabled()
        ) {
            return;
        }

        float target =
                module
                        .getFov()
                        .get()
                        .floatValue();

        if (
                !module
                        .getPreserveDynamicEffects()
                        .get()
        ) {
            cir.setReturnValue(
                    target
            );

            return;
        }

        int vanillaBase =
                client
                        .field_1690
                        .method_41808()
                        .method_41753();

        if (vanillaBase <= 0) {
            cir.setReturnValue(
                    target
            );

            return;
        }

        float vanillaResult =
                cir.getReturnValue();

        float dynamicRatio =
                vanillaResult
                        / vanillaBase;

        cir.setReturnValue(
                target
                        * dynamicRatio
        );
    }
}
