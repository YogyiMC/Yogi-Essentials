package dev.yogi.yogiessentials.client.mixin;

import dev.yogi.yogiessentials.client.module.performance.AdaptiveOptimizer;
import net.minecraft.class_310;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Captures focus loss at the actual Minecraft window callback. */
@Mixin(class_310.class)
public abstract class MinecraftClientFocusMixin {
    @Inject(method = "onWindowFocusChanged", at = @At("HEAD"))
    private void yogiessentials$trackOptimizerFocusLoss(boolean focused, CallbackInfo ci) {
        AdaptiveOptimizer.onWindowFocusChanged(focused);
    }
}
