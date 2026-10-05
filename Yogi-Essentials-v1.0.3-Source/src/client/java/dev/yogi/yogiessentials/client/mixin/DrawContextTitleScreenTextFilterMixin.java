package dev.yogi.yogiessentials.client.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Locale;
import net.minecraft.class_2561;
import net.minecraft.class_310;
import net.minecraft.class_327;
import net.minecraft.class_332;
import net.minecraft.class_442;
import net.minecraft.class_5481;

@Mixin(class_332.class)
public abstract class DrawContextTitleScreenTextFilterMixin {
    @Inject(method = "drawText(Lnet/minecraft/client/font/TextRenderer;Ljava/lang/String;IIIZ)I", at = @At("HEAD"), cancellable = true)
    private void yogiessentials$hideTitleScreenAccountLabel(
            class_327 textRenderer,
            String text,
            int x,
            int y,
            int color,
            boolean shadow,
            CallbackInfoReturnable<Integer> cir
    ) {
        if (yogiessentials$shouldSuppress(text)) {
            cir.setReturnValue(x);
        }
    }

    @Inject(method = "drawText(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/Text;IIIZ)I", at = @At("HEAD"), cancellable = true)
    private void yogiessentials$hideTitleScreenAccountText(
            class_327 textRenderer,
            class_2561 text,
            int x,
            int y,
            int color,
            boolean shadow,
            CallbackInfoReturnable<Integer> cir
    ) {
        if (text != null && yogiessentials$shouldSuppress(text.getString())) {
            cir.setReturnValue(x);
        }
    }

    @Inject(method = "drawText(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/OrderedText;IIIZ)I", at = @At("HEAD"), cancellable = true)
    private void yogiessentials$hideTitleScreenAccountOrderedText(
            class_327 textRenderer,
            class_5481 text,
            int x,
            int y,
            int color,
            boolean shadow,
            CallbackInfoReturnable<Integer> cir
    ) {
        if (text != null && yogiessentials$shouldSuppress(text.toString())) {
            cir.setReturnValue(x);
        }
    }

    private static boolean yogiessentials$shouldSuppress(String text) {
        if (text == null || text.isBlank()) return false;
        class_310 client = class_310.method_1551();
        if (client == null || !(client.field_1755 instanceof class_442)) return false;

        String normalized = text.toLowerCase(Locale.ROOT);
        return normalized.contains("current account");
    }
}
