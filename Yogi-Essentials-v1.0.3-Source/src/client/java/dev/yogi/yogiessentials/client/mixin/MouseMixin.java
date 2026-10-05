package dev.yogi.yogiessentials.client.mixin;

import dev.yogi.yogiessentials.client.util.ZoomManager;
import net.minecraft.class_312;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(class_312.class)
public abstract class MouseMixin {

    @Inject(
            method = "onMouseScroll",
            at = @At("HEAD"),
            cancellable = true
    )
    private void yogiessentials$scrollZoom(
            long window,
            double horizontal,
            double vertical,
            CallbackInfo ci
    ) {
        if (ZoomManager.handleScroll(vertical)) {
            ci.cancel();
        }
    }
}
