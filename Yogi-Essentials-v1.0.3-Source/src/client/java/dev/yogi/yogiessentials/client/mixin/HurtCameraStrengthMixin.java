package dev.yogi.yogiessentials.client.mixin;

import dev.yogi.yogiessentials.client.YogiEssentialsClient;
import dev.yogi.yogiessentials.client.module.pvp.NoHurtCameraModule;
import net.minecraft.class_4587;
import net.minecraft.class_757;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(class_757.class)
public abstract class HurtCameraStrengthMixin {

    @Redirect(
            method = "tiltViewWhenHurt",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/util/math/MatrixStack;multiply(Lorg/joml/Quaternionfc;)V"
            )
    )
    private void yogi$scaleHurtCameraRotation(
            class_4587 matrices,
            Quaternionfc original
    ) {
        NoHurtCameraModule module =
                YogiEssentialsClient
                        .getModuleManager()
                        .getModule(
                                NoHurtCameraModule.class
                        );

        if (
                module == null
                        ||
                !module.isEnabled()
        ) {
            matrices.method_22907(
                    original
            );

            return;
        }

        float strength =
                module
                        .getShakeStrength()
                        .get()
                        .floatValue();

        if (
                strength <= 0.001F
        ) {
            return;
        }

        if (
                strength >= 0.999F
        ) {
            matrices.method_22907(
                    original
            );

            return;
        }

        Quaternionf scaled =
                new Quaternionf()
                        .identity()
                        .slerp(
                                new Quaternionf(
                                        original
                                ),
                                strength
                        );

        matrices.method_22907(
                scaled
        );
    }
}
