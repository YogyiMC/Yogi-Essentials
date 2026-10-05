package dev.yogi.yogiessentials.client.module.chat;

import dev.yogi.yogiessentials.client.module.Category;
import dev.yogi.yogiessentials.client.module.Module;
import dev.yogi.yogiessentials.client.setting.BooleanSetting;
import dev.yogi.yogiessentials.client.setting.ColorSetting;
import dev.yogi.yogiessentials.client.setting.StringSetting;

public final class MessageHighlightingModule extends Module {
    private final BooleanSetting mentions = new BooleanSetting("Highlight Mentions", true);
    private final StringSetting keywords = new StringSetting("Keywords (comma separated)", "", 180);
    private final ColorSetting color = new ColorSetting("Highlight Color", 0xFFFFA040);

    public MessageHighlightingModule() {
        super("Message Highlighting", "Highlight only mentions of your username and your chosen keywords.", Category.CHAT);
        addSetting(mentions);
        addSetting(keywords);
        addSetting(color);
    }

    public BooleanSetting getMentions() { return mentions; }
    public StringSetting getKeywords() { return keywords; }
    public ColorSetting getColor() { return color; }
}
