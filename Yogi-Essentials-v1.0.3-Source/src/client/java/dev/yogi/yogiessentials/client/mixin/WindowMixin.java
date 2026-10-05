package dev.yogi.yogiessentials.client.mixin;

import dev.yogi.yogiessentials.client.module.performance.BorderlessFullscreenModule;
import net.minecraft.class_1041;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Restored to the published v1.0.2 interception path. */
@Mixin(class_1041.class)
public abstract class WindowMixin {
    @Inject(method = "toggleFullscreen", at = @At("HEAD"), cancellable = true)
    private void yogiessentials$borderlessF11(CallbackInfo ci) {
        class_1041 window = (class_1041) (Object) this;
        if (BorderlessFullscreenModule.interceptVanillaFullscreenToggle(window)) {
            ci.cancel();
        }
    }
}
