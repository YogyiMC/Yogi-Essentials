package dev.yogi.yogiessentials.client.mixin;

import net.minecraft.class_765;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(class_765.class)
public interface LightmapTextureManagerAccessor {

    @Accessor("dirty")
    void yogiessentials$setDirty(boolean dirty);
}
