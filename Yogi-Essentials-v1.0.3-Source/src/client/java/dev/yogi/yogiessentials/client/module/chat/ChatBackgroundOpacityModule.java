package dev.yogi.yogiessentials.client.module.chat;

import dev.yogi.yogiessentials.client.module.Category;
import dev.yogi.yogiessentials.client.module.Module;
import dev.yogi.yogiessentials.client.setting.NumberSetting;

public final class ChatBackgroundOpacityModule extends Module {
    private final NumberSetting opacity = new NumberSetting("Opacity (%)", 50.0, 0.0, 100.0, 5.0);

    public ChatBackgroundOpacityModule() {
        super("Chat Background Opacity", "Control only the chat message background opacity.", Category.CHAT);
        addSetting(opacity);
    }

    public NumberSetting getOpacity() { return opacity; }
}
