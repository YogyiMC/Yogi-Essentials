package dev.yogi.yogiessentials.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.class_2960;
import net.minecraft.class_3298;
import net.minecraft.class_3304;

/**
 * Protects only Yogi Essentials' private Low Fire sprite bytes.
 *
 * <p>The previous hard-override attempt replaced minecraft:block/fire_0 and
 * fire_1 globally. Besides being broader than the module contract, that still
 * left the world-fire geometry dependent on the resource-pack baked model.
 * This version does not touch vanilla Minecraft resources at all. Instead it
 * guarantees that the one private sprite used by both Yogi render paths is
 * always read from the mod jar's classpath, even if a resource pack supplies a
 * file with the same yogiessentials identifier.</p>
 */
@Mixin(class_3304.class)
public abstract class LowFireResourceOverrideMixin {

    private static final class_2960 YOGI_INTERNAL_FIRE_RESOURCE =
            class_2960.method_60655("yogiessentials", "textures/block/low_fire_internal.png");

    private static final String YOGI_INTERNAL_FIRE_CLASSPATH =
            "/assets/yogiessentials/textures/block/low_fire_internal.png";

    @Inject(method = "getResource", at = @At("RETURN"), cancellable = true)
    private void yogiessentials$protectPrivateFireResource(
            class_2960 id,
            CallbackInfoReturnable<Optional<class_3298>> cir
    ) {
        if (!YOGI_INTERNAL_FIRE_RESOURCE.equals(id)) {
            return;
        }

        Optional<class_3298> current = cir.getReturnValue();
        if (current == null || current.isEmpty()) {
            return;
        }

        cir.setReturnValue(Optional.of(yogiessentials$wrap(current.get())));
    }

    @Inject(method = "getAllResources", at = @At("RETURN"), cancellable = true)
    private void yogiessentials$protectPrivateFireResourceStack(
            class_2960 id,
            CallbackInfoReturnable<List<class_3298>> cir
    ) {
        if (!YOGI_INTERNAL_FIRE_RESOURCE.equals(id)) {
            return;
        }

        List<class_3298> current = cir.getReturnValue();
        if (current == null || current.isEmpty()) {
            return;
        }

        List<class_3298> forced = new ArrayList<>(current.size());
        for (class_3298 original : current) {
            forced.add(yogiessentials$wrap(original));
        }
        cir.setReturnValue(List.copyOf(forced));
    }

    @Unique
    private static class_3298 yogiessentials$wrap(class_3298 original) {
        return new class_3298(
                original.method_45304(),
                LowFireResourceOverrideMixin::yogiessentials$openPrivateFire
        );
    }

    @Unique
    private static InputStream yogiessentials$openPrivateFire() throws IOException {
        InputStream stream = LowFireResourceOverrideMixin.class.getResourceAsStream(
                YOGI_INTERNAL_FIRE_CLASSPATH
        );
        if (stream == null) {
            throw new IOException(
                    "Missing bundled Yogi Essentials fire texture: "
                            + YOGI_INTERNAL_FIRE_CLASSPATH
            );
        }
        return stream;
    }
}
