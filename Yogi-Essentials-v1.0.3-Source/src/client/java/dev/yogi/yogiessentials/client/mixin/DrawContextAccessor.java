package dev.yogi.yogiessentials.client.mixin;

import net.minecraft.class_11246;
import net.minecraft.class_332;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(class_332.class)
public interface DrawContextAccessor {

    @Accessor("state")
    class_11246 yogi$getGuiRenderState();
}
