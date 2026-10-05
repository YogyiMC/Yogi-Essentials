package dev.yogi.yogiessentials.client.mixin;

import dev.yogi.yogiessentials.client.util.CombatHitboxColorManager;
import net.minecraft.class_12155;
import net.minecraft.class_1297;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Customizes Minecraft 1.21.11's own F3+B entity hitbox pass. */
@Mixin(class_12155.class)
public abstract class EntityRendererHitboxColorMixin {
    @Unique private class_1297 yogiessentials$currentHitboxEntity;

    @Inject(method = "drawHitbox", at = @At("HEAD"), cancellable = true)
    private void yogiessentials$captureOrFilterHitbox(
            class_1297 entity,
            float tickProgress,
            boolean inLocalServer,
            CallbackInfo ci
    ) {
        if ((inLocalServer && !CombatHitboxColorManager.showHelper(CombatHitboxColorManager.HelperPart.SERVER_POSITION))
                || !CombatHitboxColorManager.shouldRenderHitbox(entity)) {
            yogiessentials$currentHitboxEntity = null;
            ci.cancel();
            return;
        }

        yogiessentials$currentHitboxEntity = entity;
        CombatHitboxColorManager.beginNativeHitboxDraw();
    }

    @ModifyArg(
            method = "drawHitbox",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/render/DrawStyle;stroked(I)Lnet/minecraft/client/render/DrawStyle;",
                    ordinal = 0
            ),
            index = 0
    )
    private int yogiessentials$recolorPrimaryHitbox(int vanillaColor) {
        class_1297 entity = yogiessentials$currentHitboxEntity;
        return entity == null ? vanillaColor : CombatHitboxColorManager.colorFor(entity, vanillaColor);
    }

    @Inject(method = "drawHitbox", at = @At("RETURN"))
    private void yogiessentials$clearHitboxEntity(
            class_1297 entity,
            float tickProgress,
            boolean inLocalServer,
            CallbackInfo ci
    ) {
        yogiessentials$currentHitboxEntity = null;
        CombatHitboxColorManager.endNativeHitboxDraw();
    }
}
