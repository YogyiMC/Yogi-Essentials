package dev.yogi.yogiessentials.client.mixin;

import net.minecraft.class_1735;
import net.minecraft.class_465;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(class_465.class)
public interface HandledScreenAccessor {
    @Invoker("getSlotAt")
    class_1735 yogiessentials$getSlotAt(double mouseX, double mouseY);
}
