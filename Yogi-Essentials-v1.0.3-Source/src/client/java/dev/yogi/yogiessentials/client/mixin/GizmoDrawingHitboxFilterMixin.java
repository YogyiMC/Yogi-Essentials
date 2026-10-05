package dev.yogi.yogiessentials.client.mixin;

import dev.yogi.yogiessentials.client.util.CombatHitboxColorManager;
import net.minecraft.class_12172;
import net.minecraft.class_12173;
import net.minecraft.class_12174;
import net.minecraft.class_12175;
import net.minecraft.class_12176;
import net.minecraft.class_12178;
import net.minecraft.class_12180;
import net.minecraft.class_12182;
import net.minecraft.class_12183;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Filters the gizmos produced by Minecraft's own F3+B entity hitbox renderer.
 * The native BoxGizmo is never replaced, so the box keeps vanilla interpolation.
 */
@Mixin(class_12180.class)
public abstract class GizmoDrawingHitboxFilterMixin {
    @Inject(method = "collect", at = @At("HEAD"), cancellable = true)
    private static void yogiessentials$filterNativeHitboxHelper(
            class_12175 gizmo,
            CallbackInfoReturnable<class_12178> cir
    ) {
        if (!CombatHitboxColorManager.isInsideNativeHitboxDraw()) return;

        if (gizmo instanceof class_12174) {
            int boxIndex = CombatHitboxColorManager.nextNativeBoxIndex();
            if (boxIndex == 0) return; // Minecraft's real entity AABB.

            if (CombatHitboxColorManager.onlyPrimaryHitboxEnabled()
                    || !CombatHitboxColorManager.showHelper(CombatHitboxColorManager.HelperPart.EYE_HEIGHT)) {
                yogiessentials$suppress(cir);
            }
            return;
        }

        if (CombatHitboxColorManager.onlyPrimaryHitboxEnabled()) {
            yogiessentials$suppress(cir);
            return;
        }

        if (gizmo instanceof class_12172
                && !CombatHitboxColorManager.showHelper(CombatHitboxColorManager.HelperPart.LOOK_DIRECTION)) {
            yogiessentials$suppress(cir);
            return;
        }

        if (gizmo instanceof class_12182 line) {
            CombatHitboxColorManager.HelperPart part = yogiessentials$classifyLine(line.comp_5105());
            if (!CombatHitboxColorManager.showHelper(part)) {
                yogiessentials$suppress(cir);
                return;
            }
        }

        if (gizmo instanceof class_12173
                && !CombatHitboxColorManager.showHelper(CombatHitboxColorManager.HelperPart.DEBUG_CIRCLES)) {
            yogiessentials$suppress(cir);
            return;
        }

        if (gizmo instanceof class_12183
                && !CombatHitboxColorManager.showHelper(CombatHitboxColorManager.HelperPart.DEBUG_POINTS)) {
            yogiessentials$suppress(cir);
        }
    }

    @Unique
    private static CombatHitboxColorManager.HelperPart yogiessentials$classifyLine(int argb) {
        int rgb = argb & 0x00FFFFFF;
        int red = (rgb >>> 16) & 0xFF;
        int green = (rgb >>> 8) & 0xFF;
        int blue = rgb & 0xFF;

        if (red > green + 32 && red > blue + 32) {
            return CombatHitboxColorManager.HelperPart.EYE_HEIGHT;
        }

        if (blue > red + 32 && blue > green + 32) {
            return CombatHitboxColorManager.HelperPart.LOOK_DIRECTION;
        }

        return CombatHitboxColorManager.HelperPart.EXTRA_DIRECTION_LINES;
    }

    @Unique
    private static void yogiessentials$suppress(CallbackInfoReturnable<class_12178> cir) {
        cir.setReturnValue(class_12176.field_64085);
    }
}
