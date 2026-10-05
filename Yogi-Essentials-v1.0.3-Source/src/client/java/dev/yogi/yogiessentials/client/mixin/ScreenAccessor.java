package dev.yogi.yogiessentials.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;
import net.minecraft.class_364;
import net.minecraft.class_437;

@Mixin(class_437.class)
public interface ScreenAccessor {
    @Accessor("children")
    List<class_364> yogiessentials$getChildren();
}
