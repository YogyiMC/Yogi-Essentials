package dev.yogi.yogiessentials.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;
import net.minecraft.class_279;
import net.minecraft.class_283;

@Mixin(class_279.class)
public interface PostEffectProcessorAccessor {
    @Accessor("passes")
    List<class_283> yogi$getPasses();
}
