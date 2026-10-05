package dev.yogi.yogiessentials.client.mixin;

import net.minecraft.class_1058;
import net.minecraft.class_4587;
import net.minecraft.class_4597;
import net.minecraft.class_4603;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Invokes Minecraft's real first-person fire renderer for the live preview. */
@Mixin(class_4603.class)
public interface InGameOverlayRendererInvoker {
    @Invoker("renderFireOverlay")
    static void yogi$renderFireOverlay(
            class_4587 matrices,
            class_4597 vertexConsumers,
            class_1058 sprite
    ) {
        throw new AssertionError();
    }
}
