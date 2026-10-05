package dev.yogi.yogiessentials.client.mixin;

import dev.yogi.yogiessentials.client.module.performance.PerformanceOptimizer;
import net.minecraft.class_310;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(class_310.class)
public abstract class MinecraftClientFrameLimiterMixin {
    @Inject(method = "render(Z)V", at = @At("RETURN"))
    private void yogiessentials$limitCompletedFrame(boolean tick, CallbackInfo ci) {
        PerformanceOptimizer.paceFrame();
    }
}
