package dev.yogi.yogiessentials.client.module.chat;

import dev.yogi.yogiessentials.client.module.Category;
import dev.yogi.yogiessentials.client.module.Module;
import dev.yogi.yogiessentials.client.setting.BooleanSetting;
import dev.yogi.yogiessentials.client.setting.NumberSetting;

public final class ChatSearchBarModule extends Module {
    private final NumberSetting maxResults = new NumberSetting("Maximum Results", 8.0, 3.0, 20.0, 1.0);
    private final BooleanSetting caseSensitive = new BooleanSetting("Case Sensitive", false);

    public ChatSearchBarModule() {
        super("Chat Search Bar", "Press Ctrl+F in chat to search recent messages by text or player name.", Category.CHAT);
        addSetting(maxResults);
        addSetting(caseSensitive);
    }

    public NumberSetting getMaxResults() { return maxResults; }
    public BooleanSetting getCaseSensitive() { return caseSensitive; }
}
