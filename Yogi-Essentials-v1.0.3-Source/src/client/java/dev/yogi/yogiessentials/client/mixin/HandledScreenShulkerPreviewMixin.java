package dev.yogi.yogiessentials.client.mixin;

import dev.yogi.yogiessentials.client.YogiEssentialsClient;
import dev.yogi.yogiessentials.client.module.smp.ShulkerBoxPreviewModule;
import net.minecraft.class_1735;
import net.minecraft.class_1747;
import net.minecraft.class_1799;
import net.minecraft.class_2371;
import net.minecraft.class_2480;
import net.minecraft.class_310;
import net.minecraft.class_332;
import net.minecraft.class_465;
import net.minecraft.class_9288;
import net.minecraft.class_9334;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Replaces the vanilla shulker hover tooltip with the Yogi Essentials grid preview.
 *
 * Hooking drawMouseoverTooltip is intentional: this is the common handled-screen path
 * used by normal survival inventories as well as container screens. The previous render
 * tail hook depended on hover state that is not reliable across every handled screen.
 */
@Mixin(class_465.class)
public abstract class HandledScreenShulkerPreviewMixin {
    @Shadow protected class_1735 focusedSlot;

    @Inject(method = "drawMouseoverTooltip", at = @At("HEAD"), cancellable = true)
    private void yogiessentials$replaceShulkerTooltip(
            class_332 context,
            int mouseX,
            int mouseY,
            CallbackInfo ci
    ) {
        ShulkerBoxPreviewModule module = yogiessentials$getModule();
        if (module == null || !module.isEnabled()) {
            return;
        }

        class_1735 slot = focusedSlot;
        if (slot == null || slot.method_7677().method_7960()) {
            return;
        }

        class_1799 shulker = slot.method_7677();
        if (!yogiessentials$isShulker(shulker)) {
            return;
        }

        ci.cancel();

        class_310 client = class_310.method_1551();
        if (client == null || client.field_1772 == null) {
            return;
        }

        if (!yogiessentials$activationSatisfied(module, client)) {
            context.method_51438(client.field_1772, shulker.method_7964(), mouseX, mouseY);
            return;
        }

        class_2371<class_1799> contents = class_2371.method_10213(27, class_1799.field_8037);
        class_9288 component = shulker.method_58694(class_9334.field_49622);
        if (component != null) {
            component.method_57492(contents);
        }

        boolean anyItems = contents.stream().anyMatch(stack -> !stack.method_7960());
        if (!module.getShowEmptySlots().get() && !anyItems) {
            context.method_51438(client.field_1772, shulker.method_7964(), mouseX, mouseY);
            return;
        }

        yogiessentials$renderPreview(context, client, module, shulker, contents, mouseX, mouseY);
    }

    private static ShulkerBoxPreviewModule yogiessentials$getModule() {
        if (YogiEssentialsClient.getModuleManager() == null) {
            return null;
        }
        return YogiEssentialsClient.getModuleManager().getModule(ShulkerBoxPreviewModule.class);
    }

    private static boolean yogiessentials$isShulker(class_1799 stack) {
        return !stack.method_7960()
                && stack.method_7909() instanceof class_1747 blockItem
                && blockItem.method_7711() instanceof class_2480;
    }

    private static boolean yogiessentials$activationSatisfied(
            ShulkerBoxPreviewModule module,
            class_310 client
    ) {
        if (module.getActivationMode().get() != ShulkerBoxPreviewModule.ActivationMode.SHIFT_HOVER) {
            return true;
        }
        if (client.method_22683() == null) {
            return false;
        }
        long window = client.method_22683().method_4490();
        return GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
    }

    private static void yogiessentials$renderPreview(
            class_332 context,
            class_310 client,
            ShulkerBoxPreviewModule module,
            class_1799 shulker,
            class_2371<class_1799> contents,
            int mouseX,
            int mouseY
    ) {
        float scale = module.getPreviewScale().get().floatValue() / 100.0F;
        if (scale <= 0.0F) {
            return;
        }

        int logicalWidth = 9 * 18 + 12;
        int logicalHeight = 3 * 18 + 30;
        int pixelWidth = Math.round(logicalWidth * scale);
        int pixelHeight = Math.round(logicalHeight * scale);

        int px = mouseX + 14;
        int py = mouseY + 14;
        if (px + pixelWidth > context.method_51421() - 4) {
            px = mouseX - pixelWidth - 14;
        }
        if (py + pixelHeight > context.method_51443() - 4) {
            py = mouseY - pixelHeight - 14;
        }
        px = Math.max(4, px);
        py = Math.max(4, py);

        var matrices = context.method_51448();
        matrices.pushMatrix();
        matrices.translate(px, py);
        matrices.scale(scale, scale);

        context.method_25294(-3, -3, logicalWidth + 3, logicalHeight + 3, 0xB0000000);
        context.method_25294(0, 0, logicalWidth, logicalHeight, 0xF0121216);
        yogiessentials$outline(context, 0, 0, logicalWidth, logicalHeight, 0xFFFF6A00);

        String title = client.field_1772.method_27523(shulker.method_7964().getString(), logicalWidth - 12);
        context.method_25303(client.field_1772, title, 6, 7, 0xFFF4F4F5);

        int startX = 6;
        int startY = 23;
        for (int i = 0; i < 27; i++) {
            int sx = startX + (i % 9) * 18;
            int sy = startY + (i / 9) * 18;
            class_1799 stack = contents.get(i);

            if (module.getShowEmptySlots().get() || !stack.method_7960()) {
                context.method_25294(sx, sy, sx + 16, sy + 16, 0xAA24242A);
                yogiessentials$outline(context, sx, sy, 16, 16, 0xFF3B3B42);
            }
            if (!stack.method_7960()) {
                context.method_51427(stack, sx, sy);
                context.method_51431(client.field_1772, stack, sx, sy);
            }
        }

        matrices.popMatrix();
    }

    private static void yogiessentials$outline(
            class_332 context,
            int x,
            int y,
            int width,
            int height,
            int color
    ) {
        context.method_25294(x, y, x + width, y + 1, color);
        context.method_25294(x, y + height - 1, x + width, y + height, color);
        context.method_25294(x, y, x + 1, y + height, color);
        context.method_25294(x + width - 1, y, x + width, y + height, color);
    }
}
