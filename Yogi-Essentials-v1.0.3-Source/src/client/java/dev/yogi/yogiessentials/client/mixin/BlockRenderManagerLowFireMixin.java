package dev.yogi.yogiessentials.client.mixin;

import net.minecraft.class_776;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Legacy compatibility stub.
 *
 * <p>Small Ground Fire no longer modifies {@link class_776} matrices.
 * The active implementation wraps the final baked fire models in
 * {@code BlockModelsLowFireMixin}, after resource-pack model resolution, then
 * translates the complete quads downward without changing their proportions or
 * UVs. Keeping this harmless class prevents stale source overlays from
 * reintroducing the old scaling implementation while still compiling cleanly.</p>
 */
@Mixin(class_776.class)
public abstract class BlockRenderManagerLowFireMixin {
}
