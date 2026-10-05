package dev.yogi.yogiessentials.client.mixin;

import net.minecraft.class_310;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Exposes the vanilla attack entry point so Spear Optimizer performs one real,
 * ordinary attack (identical to a normal left-click) after the deterministic
 * same-tick swap + slot sync. No extra packets, no custom damage.
 */
@Mixin(class_310.class)
public interface MinecraftClientInvoker {

    @Invoker("doAttack")
    boolean yogi$doAttack();
}
