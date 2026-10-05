package dev.yogi.yogiessentials.client.mixin;

import net.minecraft.class_4185;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(class_4185.class)
public interface ButtonWidgetAccessor {
    @Accessor("onPress")
    class_4185.class_4241 yogiessentials$getPressAction();
}
