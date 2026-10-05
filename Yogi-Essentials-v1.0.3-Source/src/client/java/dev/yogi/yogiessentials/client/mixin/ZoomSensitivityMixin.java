package dev.yogi.yogiessentials.client.mixin;

import dev.yogi.yogiessentials.client.util.ZoomManager;
import net.minecraft.class_1297;
import net.minecraft.class_310;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(class_1297.class)
public abstract class ZoomSensitivityMixin {
    @ModifyVariable(method = "changeLookDirection(DD)V", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private double yogiessentials$scaleLookX(double delta) {
        return (Object) this == class_310.method_1551().field_1724
                ? ZoomManager.scaleLookDelta(delta) : delta;
    }

    @ModifyVariable(method = "changeLookDirection(DD)V", at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private double yogiessentials$scaleLookY(double delta) {
        return (Object) this == class_310.method_1551().field_1724
                ? ZoomManager.scaleLookDelta(delta) : delta;
    }
}
