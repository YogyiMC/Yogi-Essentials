package dev.yogi.yogiessentials.client.mixin;

import com.mojang.blaze3d.buffers.GpuBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;
import net.minecraft.class_283;

@Mixin(class_283.class)
public interface PostEffectPassAccessor {
    @Accessor("uniformBuffers")
    Map<String, GpuBuffer> yogi$getUniformBuffers();
}
