package dev.yogi.yogiessentials.client.mixin;

import dev.yogi.yogiessentials.client.YogiEssentialsClient;
import dev.yogi.yogiessentials.client.module.chat.ChatSearchBarModule;
import dev.yogi.yogiessentials.client.util.ChatEnhancementManager;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import net.minecraft.class_11908;
import net.minecraft.class_310;
import net.minecraft.class_332;
import net.minecraft.class_338;
import net.minecraft.class_342;
import net.minecraft.class_408;

@Mixin(class_408.class)
public abstract class ChatScreenMixin {
    @Shadow protected class_342 chatField;

    @Unique private boolean yogiessentials$searchActive;
    @Unique private String yogiessentials$savedChatText = "";

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void yogiessentials$searchKeys(class_11908 input, CallbackInfoReturnable<Boolean> cir) {
        ChatSearchBarModule module = yogiessentials$getSearchModule();
        if (module == null || !module.isEnabled()) {
            if (yogiessentials$searchActive) {
                yogiessentials$closeSearch(true);
            }
            return;
        }

        if (input.comp_4795() == GLFW.GLFW_KEY_F && input.method_75377()) {
            if (yogiessentials$searchActive) {
                yogiessentials$closeSearch(true);
            } else {
                yogiessentials$savedChatText = chatField == null ? "" : chatField.method_1882();
                yogiessentials$searchActive = true;
                if (chatField != null) {
                    chatField.method_1852("");
                    chatField.method_25365(true);
                }
            }
            cir.setReturnValue(true);
            return;
        }

        if (!yogiessentials$searchActive) {
            return;
        }

        if (input.method_74231() || input.method_74230()) {
            yogiessentials$closeSearch(true);
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void yogiessentials$renderSearch(
            class_332 context,
            int mouseX,
            int mouseY,
            float deltaTicks,
            CallbackInfo ci
    ) {
        if (!yogiessentials$searchActive || chatField == null) {
            return;
        }

        ChatSearchBarModule module = yogiessentials$getSearchModule();
        if (module == null || !module.isEnabled()) {
            return;
        }

        String query = chatField.method_1882();
        int maxResults = module.getMaxResults().get().intValue();
        List<String> results = ChatEnhancementManager.search(
                query,
                maxResults,
                module.getCaseSensitive().get()
        );

        int screenWidth = context.method_51421();
        int screenHeight = context.method_51443();
        class_310 client = class_310.method_1551();

        double chatScale = Math.max(0.0, Math.min(1.0,
                client.field_1690.method_42554().method_41753()));

        int chatWidth = (int) Math.ceil(class_338.method_1806(client.field_1690.method_42556().method_41753()) * chatScale);
        int chatHeight = (int) Math.ceil(class_338.method_1818(client.field_1690.method_41803().method_41753()) * chatScale);

        int panelWidth = Math.min(430, Math.max(190, chatWidth));
        panelWidth = Math.min(panelWidth, Math.max(1, screenWidth - 16));
        int resultCount = Math.max(1, results.size());
        int panelHeight = 28 + resultCount * 12 + 8;
        int x = 4;
        int y = screenHeight - 40 - chatHeight - panelHeight - 8;
        if (y < 8) {
            x = Math.max(8, (screenWidth - panelWidth) / 2);
            y = Math.max(8, (screenHeight - panelHeight) / 2);
        }
        int right = Math.min(screenWidth - 4, x + panelWidth);

        context.method_25294(x, y, right, y + panelHeight, 0xE8101014);
        yogiessentials$outline(context, x, y, right - x, panelHeight, 0xFFFF6A00);

        String heading = query.isBlank()
                ? "SEARCH CHAT — type to find messages • Ctrl+F to close"
                : "SEARCH CHAT — " + results.size() + " result" + (results.size() == 1 ? "" : "s");
        heading = client.field_1772.method_27523(heading, Math.max(20, right - x - 16));
        context.method_25303(
                client.field_1772,
                heading,
                x + 8,
                y + 7,
                0xFFFF6A00
        );

        int rowY = y + 23;
        if (query.isBlank()) {
            context.method_25303(
                    client.field_1772,
                    "Search by message text or player name.",
                    x + 8,
                    rowY,
                    0xFFAAAAAF
            );
            return;
        }

        if (results.isEmpty()) {
            context.method_25303(
                    client.field_1772,
                    "No matching chat messages.",
                    x + 8,
                    rowY,
                    0xFFAAAAAF
            );
            return;
        }

        for (String result : results) {
            String trimmed = client.field_1772
                    .method_27523(result, Math.max(20, right - x - 16));
            context.method_25303(
                    client.field_1772,
                    trimmed,
                    x + 8,
                    rowY,
                    0xFFF4F4F5
            );
            rowY += 12;
        }
    }

    @Inject(method = "removed", at = @At("HEAD"))
    private void yogiessentials$restoreDraftOnClose(CallbackInfo ci) {
        if (yogiessentials$searchActive) {
            yogiessentials$closeSearch(true);
        }
    }

    @Unique
    private void yogiessentials$closeSearch(boolean restoreChatText) {
        yogiessentials$searchActive = false;
        if (restoreChatText && chatField != null) {
            chatField.method_1852(yogiessentials$savedChatText == null ? "" : yogiessentials$savedChatText);
        }
        yogiessentials$savedChatText = "";
    }

    @Unique
    private ChatSearchBarModule yogiessentials$getSearchModule() {
        if (YogiEssentialsClient.getModuleManager() == null) {
            return null;
        }
        return YogiEssentialsClient.getModuleManager().getModule(ChatSearchBarModule.class);
    }

    @Unique
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
