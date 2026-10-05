package dev.yogi.yogiessentials.client.mixin;

import net.minecraft.class_636;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;






@Mixin(class_636.class)
public interface ClientPlayerInteractionManagerInvoker {

    @Accessor("lastSelectedSlot")
    void yogi$setLastSelectedSlot(int slot);
}
