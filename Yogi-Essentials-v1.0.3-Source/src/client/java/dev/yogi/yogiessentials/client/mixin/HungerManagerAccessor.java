package dev.yogi.yogiessentials.client.mixin;

import net.minecraft.class_1702;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(class_1702.class)
public interface HungerManagerAccessor {
    @Accessor("exhaustion")
    float yogiessentials$getExhaustion();
}
