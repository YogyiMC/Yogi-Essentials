package dev.yogi.yogiessentials.client.mixin;

import net.minecraft.class_1306;
import net.minecraft.class_4587;
import net.minecraft.class_759;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;











@Mixin(class_759.class)
public interface HeldItemRendererInvoker {

    @Invoker("applyEquipOffset")
    void yogi$applyEquipOffset(class_4587 matrices, class_1306 arm, float equipProgress);

    @Invoker("applySwingOffset")
    void yogi$applySwingOffset(class_4587 matrices, class_1306 arm, float swingProgress);
}
