package dev.yogi.yogiessentials.client.mixin;

import net.minecraft.class_1041;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Non-destructive access to Minecraft's logical fullscreen/windowed state. */
@Mixin(class_1041.class)
public interface WindowStateAccessor {
    @Accessor("fullscreen")
    void yogiessentials$setFullscreen(boolean fullscreen);

    @Accessor("currentFullscreen")
    void yogiessentials$setCurrentFullscreen(boolean fullscreen);

    @Accessor("windowedX")
    int yogiessentials$getWindowedX();

    @Accessor("windowedY")
    int yogiessentials$getWindowedY();

    @Accessor("windowedWidth")
    int yogiessentials$getWindowedWidth();

    @Accessor("windowedHeight")
    int yogiessentials$getWindowedHeight();
}
