package dev.yogi.yogiessentials.client.mixin;

import net.minecraft.class_757;
import net.minecraft.class_759;
import net.minecraft.class_9920;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;









@SuppressWarnings("ALL")
@Mixin(class_757.class)
public interface GameRendererAccessor {

    @Accessor("firstPersonRenderer")
    class_759 yogi$getHeldItemRenderer();

    @Accessor("pool")
    class_9920 yogi$getPostEffectPool();
}
