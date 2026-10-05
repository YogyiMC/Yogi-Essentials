package dev.yogi.yogiessentials.client.module.chat;

import dev.yogi.yogiessentials.client.module.Category;
import dev.yogi.yogiessentials.client.module.Module;
import dev.yogi.yogiessentials.client.setting.NumberSetting;

public final class ChatHistoryLimitModule extends Module {
    private final NumberSetting limit = new NumberSetting("Message Limit", 1000.0, 25.0, 1000.0, 25.0);

    public ChatHistoryLimitModule() {
        super("Chat History Limit", "Choose how many chat messages and sent-history entries the client keeps.", Category.CHAT);
        addSetting(limit);
    }

    public NumberSetting getLimit() { return limit; }
}
