package dev.yogi.yogiessentials.client.mixin;

import dev.yogi.yogiessentials.client.YogiEssentialsClient;
import dev.yogi.yogiessentials.client.module.pvp.NoHurtCameraModule;
import net.minecraft.class_4587;
import net.minecraft.class_757;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(class_757.class)
public abstract class GameRendererMixin {

    @Inject(
            method = "tiltViewWhenHurt",
            at = @At("HEAD"),
            cancellable = true
    )
    private void yogiessentials$disableHurtCamera(
            class_4587 matrices,
            float tickProgress,
            CallbackInfo ci
    ) {

        NoHurtCameraModule module =
                YogiEssentialsClient
                        .getModuleManager()
                        .getModule(
                                NoHurtCameraModule.class
                        );

        if (
                module != null
                        && module.isEnabled()
        ) {

            ci.cancel();
        }
    }
}