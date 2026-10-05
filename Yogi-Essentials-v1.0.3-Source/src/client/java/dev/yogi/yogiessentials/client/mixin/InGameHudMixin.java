package dev.yogi.yogiessentials.client.mixin;

import dev.yogi.yogiessentials.client.YogiEssentialsClient;
import dev.yogi.yogiessentials.client.module.hud.SaturationHudModule;
import dev.yogi.yogiessentials.client.module.fixes.CenteredCrosshairFixModule;
import dev.yogi.yogiessentials.client.module.smp.BossbarModule;
import dev.yogi.yogiessentials.client.render.CrosshairRenderer;
import dev.yogi.yogiessentials.client.util.CombatHitboxColorManager;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.class_10799;
import net.minecraft.class_1657;
import net.minecraft.class_2960;
import net.minecraft.class_329;
import net.minecraft.class_332;
import net.minecraft.class_9779;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import org.joml.Matrix3x2fStack;

@Mixin(class_329.class)
public abstract class InGameHudMixin {

    private static final class_2960 YOGI_SATURATION_OUTLINE =
            class_2960.method_60655("yogiessentials", "hud/saturation_outline");

    @Unique
    private boolean yogiessentials$centerShiftApplied;

    @Unique
    private float yogiessentials$centerShiftX;

    @Unique
    private float yogiessentials$centerShiftY;

    @Unique
    private boolean yogiessentials$bossbarTransformApplied;

    @Inject(
            method = "renderCrosshair",
            at = @At("HEAD"),
            cancellable = true
    )
    private void yogiessentials$crosshairHead(
            class_332 context,
            class_9779 tickCounter,
            CallbackInfo ci
    ) {
        yogiessentials$centerShiftApplied = false;
        yogiessentials$centerShiftX = 0.0F;
        yogiessentials$centerShiftY = 0.0F;

        if (CrosshairRenderer.shouldOverrideVanilla()) {
            CrosshairRenderer.render(context);
            ci.cancel();
            return;
        }


        if (YogiEssentialsClient.getModuleManager() == null) {
            return;
        }

        CenteredCrosshairFixModule centered =
                YogiEssentialsClient.getModuleManager().getModule(CenteredCrosshairFixModule.class);

        if (centered == null || !centered.isEnabled()
                || FabricLoader.getInstance().isModLoaded("centered-crosshair")) {
            return;
        }

        
        
        
        
        
        int w = context.method_51421();
        int h = context.method_51443();
        float vanillaX = w / 2 - 7.0F;
        float vanillaY = h / 2 - 7.0F;
        float exactX = (w - 15.0F) / 2.0F;
        float exactY = (h - 15.0F) / 2.0F;

        yogiessentials$centerShiftX = exactX - vanillaX;
        yogiessentials$centerShiftY = exactY - vanillaY;

        if (yogiessentials$centerShiftX != 0.0F || yogiessentials$centerShiftY != 0.0F) {
            context.method_51448().translate(
                    yogiessentials$centerShiftX,
                    yogiessentials$centerShiftY
            );
            yogiessentials$centerShiftApplied = true;
        }
    }

    @Inject(
            method = "renderCrosshair",
            at = @At("RETURN")
    )
    private void yogiessentials$crosshairTail(
            class_332 context,
            class_9779 tickCounter,
            CallbackInfo ci
    ) {
        if (yogiessentials$centerShiftApplied) {
            context.method_51448().translate(
                    -yogiessentials$centerShiftX,
                    -yogiessentials$centerShiftY
            );
            yogiessentials$centerShiftApplied = false;
        }

        Integer reachColor = CombatHitboxColorManager.activeReachCrosshairColor();
        if (reachColor != null && !CrosshairRenderer.shouldOverrideVanilla()) {
            yogiessentials$drawReachOverlayAccent(context, reachColor);
        }
    }

    @Unique
    private static void yogiessentials$drawReachOverlayAccent(class_332 context, int reachColor) {
        int centerX = context.method_51421() / 2;
        int centerY = context.method_51443() / 2;

        int outlineColor = 0xFF000000;
        context.method_25294(centerX - 2, centerY - 2, centerX + 3, centerY + 3, outlineColor);
        context.method_25294(centerX - 1, centerY - 1, centerX + 2, centerY + 2, reachColor | 0xFF000000);
    }

    




    @Inject(
            method = "renderFood",
            at = @At("HEAD")
    )
    private void yogiessentials$renderExhaustionUnderlay(
            class_332 context,
            class_1657 player,
            int top,
            int right,
            CallbackInfo ci
    ) {
        SaturationHudModule module = yogiessentials$getSaturationModule();
        if (!yogiessentials$shouldRenderSaturation(module)) {
            return;
        }

        float exhaustion = Math.max(
                0.0F,
                Math.min(
                        4.0F,
                        ((HungerManagerAccessor) (Object) player.method_7344())
                                .yogiessentials$getExhaustion()
                )
        );
        if (exhaustion <= 0.0F) {
            return;
        }

        int progress = Math.max(0, Math.min(81, Math.round((exhaustion / 4.0F) * 81.0F)));
        int alpha = Math.max(
                0,
                Math.min(
                        255,
                        Math.round(module.getOpacity().get().floatValue() * 0.40F * 255.0F)
                )
        );
        int color = (alpha << 24) | module.getOverlayColor().getRgb();

        for (int i = 0; i < progress; i++) {
            int px = right - i - 1;
            int py = top + 7 + (i & 1);
            context.method_25294(px, py, px + 1, py + 1, color);
        }
    }

    @Inject(
            method = "renderFood",
            at = @At("RETURN")
    )
    private void yogiessentials$renderSaturation(
            class_332 context,
            class_1657 player,
            int top,
            int right,
            CallbackInfo ci
    ) {
        SaturationHudModule module = yogiessentials$getSaturationModule();
        if (!yogiessentials$shouldRenderSaturation(module)) {
            return;
        }

        float saturation = Math.max(
                0.0F,
                Math.min(20.0F, player.method_7344().method_7589())
        );
        if (saturation <= 0.0F) {
            return;
        }

        int alpha = Math.max(
                0,
                Math.min(
                        255,
                        Math.round(module.getOpacity().get().floatValue() * 255.0F)
                )
        );
        int color = (alpha << 24) | module.getOverlayColor().getRgb();

        for (int slot = 0; slot < 10; slot++) {
            float amount = saturation - (slot * 2.0F);
            if (amount <= 0.0F) {
                break;
            }

            float fraction = Math.max(0.0F, Math.min(1.0F, amount / 2.0F));
            int quarter = Math.max(1, Math.min(4, (int) Math.ceil(fraction * 4.0F - 0.0001F)));
            int visibleWidth = switch (quarter) {
                case 1 -> 3;
                case 2 -> 5;
                case 3 -> 7;
                default -> 9;
            };

            int x = right - slot * 8 - 9;

            context.method_44379(x, top, x + visibleWidth, top + 9);
            context.method_52707(
                    class_10799.field_56883,
                    YOGI_SATURATION_OUTLINE,
                    x,
                    top,
                    9,
                    9,
                    color
            );
            context.method_44380();
        }
    }

    @Unique
    private static SaturationHudModule yogiessentials$getSaturationModule() {
        if (YogiEssentialsClient.getModuleManager() == null) {
            return null;
        }
        return YogiEssentialsClient
                .getModuleManager()
                .getModule(SaturationHudModule.class);
    }

    @Unique
    private static boolean yogiessentials$shouldRenderSaturation(SaturationHudModule module) {
        if (module == null || !module.isEnabled()) {
            return false;
        }
        return !module.getAvoidAppleSkinDuplicate().get()
                || !FabricLoader.getInstance().isModLoaded("appleskin");
    }

    @Inject(method = "renderBossBarHud", at = @At("HEAD"), cancellable = true)
    private void yogiessentials$bossbarHead(class_332 context, class_9779 tickCounter, CallbackInfo ci) {
        yogiessentials$bossbarTransformApplied = false;
        if (YogiEssentialsClient.getModuleManager() == null) return;
        BossbarModule module = YogiEssentialsClient.getModuleManager().getModule(BossbarModule.class);
        if (module == null || !module.isEnabled()) return;
        if (!module.getShowBars().get()) {
            ci.cancel();
            return;
        }

        float scale = module.getScale().get().floatValue();
        float x = module.getXOffset().get().floatValue();
        float y = module.getYOffset().get().floatValue();
        if (Math.abs(scale - 1.0F) < 0.0001F && Math.abs(x) < 0.0001F && Math.abs(y) < 0.0001F) return;

        Matrix3x2fStack matrices = context.method_51448();
        matrices.pushMatrix();
        float centerX = context.method_51421() / 2.0F;
        matrices.translate(centerX + x, y);
        matrices.scale(scale, scale);
        matrices.translate(-centerX, 0.0F);
        yogiessentials$bossbarTransformApplied = true;
    }

    @Inject(method = "renderBossBarHud", at = @At("RETURN"))
    private void yogiessentials$bossbarTail(class_332 context, class_9779 tickCounter, CallbackInfo ci) {
        if (yogiessentials$bossbarTransformApplied) {
            context.method_51448().popMatrix();
            yogiessentials$bossbarTransformApplied = false;
        }
    }
}
