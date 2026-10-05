package dev.yogi.yogiessentials.client.mixin;

import dev.yogi.yogiessentials.client.YogiEssentialsClient;
import dev.yogi.yogiessentials.client.util.ChatEnhancementManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import net.minecraft.class_2561;
import net.minecraft.class_303;
import net.minecraft.class_338;
import net.minecraft.class_7469;
import net.minecraft.class_7591;
import net.minecraft.class_8623;

@Mixin(class_338.class)
public abstract class ChatHudMixin {
    @Shadow private List<class_303> messages;
    @Shadow private class_8623<String> messageHistory;
    @Invoker("refresh")
    protected abstract void yogiessentials$invokeRefresh();

    @Unique
    private class_2561 yogiessentials$preparedMessage;

    @Inject(
            method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V",
            at = @At("HEAD")
    )
    private void yogiessentials$prepareMessage(
            class_2561 message,
            class_7469 signatureData,
            class_7591 indicator,
            CallbackInfo ci
    ) {
        ChatEnhancementManager.ProcessedMessage processed =
                ChatEnhancementManager.process(message, signatureData != null);
        yogiessentials$preparedMessage = processed.text();

        if (processed.replacePrevious() && !messages.isEmpty()) {
            messages.remove(0);
            yogiessentials$invokeRefresh();
        }
    }

    @org.spongepowered.asm.mixin.injection.ModifyVariable(
            method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0
    )
    private class_2561 yogiessentials$usePreparedMessage(class_2561 original) {
        return yogiessentials$preparedMessage == null ? original : yogiessentials$preparedMessage;
    }

    @Inject(
            method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V",
            at = @At("TAIL")
    )
    private void yogiessentials$trimStoredMessages(
            class_2561 message,
            class_7469 signatureData,
            class_7591 indicator,
            CallbackInfo ci
    ) {
        int limit = ChatEnhancementManager.configuredHistoryLimit();
        boolean changed = false;
        while (messages.size() > limit) {
            messages.remove(messages.size() - 1);
            changed = true;
        }
        if (changed) {
            yogiessentials$invokeRefresh();
        }
        yogiessentials$preparedMessage = null;
    }

    @ModifyConstant(
            method = "addMessage(Lnet/minecraft/client/gui/hud/ChatHudLine;)V",
            constant = @Constant(intValue = 100),
            require = 0
    )
    private int yogiessentials$storedMessageLimit(int vanillaLimit) {
        return ChatEnhancementManager.configuredHistoryLimit();
    }

    @ModifyConstant(
            method = "addVisibleMessage(Lnet/minecraft/client/gui/hud/ChatHudLine;)V",
            constant = @Constant(intValue = 100),
            require = 0
    )
    private int yogiessentials$visibleMessageLimit(int vanillaLimit) {
        return ChatEnhancementManager.configuredHistoryLimit();
    }

    @ModifyConstant(
            method = "addToMessageHistory",
            constant = @Constant(intValue = 100),
            require = 0
    )
    private int yogiessentials$sentMessageLimit(int vanillaLimit) {
        return ChatEnhancementManager.configuredHistoryLimit();
    }

    @Inject(method = "addToMessageHistory", at = @At("TAIL"))
    private void yogiessentials$trimSentHistory(String message, CallbackInfo ci) {
        int limit = ChatEnhancementManager.configuredHistoryLimit();
        while (messageHistory.size() > limit) {
            messageHistory.removeFirst();
        }
    }

    @Inject(method = "clear", at = @At("TAIL"))
    private void yogiessentials$resetRepeatState(boolean clearHistory, CallbackInfo ci) {
        ChatEnhancementManager.resetRepeatState();
    }

}
