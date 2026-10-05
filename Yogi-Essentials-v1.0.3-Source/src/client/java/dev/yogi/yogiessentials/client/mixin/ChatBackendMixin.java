package dev.yogi.yogiessentials.client.mixin;

import dev.yogi.yogiessentials.client.YogiEssentialsClient;
import dev.yogi.yogiessentials.client.module.chat.ChatBackgroundOpacityModule;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(targets = {
        "net.minecraft.client.gui.hud.ChatHud$Hud",
        "net.minecraft.client.gui.hud.ChatHud$Interactable",
        "net.minecraft.client.gui.hud.ChatHud$Forwarder"
})
public abstract class ChatBackendMixin {
    @ModifyVariable(method = "fill", at = @At("HEAD"), argsOnly = true, ordinal = 4)
    private int yogiessentials$chatBackgroundOpacity(int color) {
        if (YogiEssentialsClient.getModuleManager() == null) {
            return color;
        }

        ChatBackgroundOpacityModule module =
                YogiEssentialsClient.getModuleManager().getModule(ChatBackgroundOpacityModule.class);
        if (module == null || !module.isEnabled()) {
            return color;
        }

        if ((color & 0x00FFFFFF) != 0) {
            return color;
        }

        int alpha = Math.max(0, Math.min(255,
                (int) Math.round(module.getOpacity().get() * 2.55)));
        return (alpha << 24) | (color & 0x00FFFFFF);
    }
}
