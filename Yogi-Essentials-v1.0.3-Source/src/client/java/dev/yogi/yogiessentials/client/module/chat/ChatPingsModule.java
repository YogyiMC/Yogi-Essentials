package dev.yogi.yogiessentials.client.module.chat;

import dev.yogi.yogiessentials.client.module.Category;
import dev.yogi.yogiessentials.client.module.Module;
import dev.yogi.yogiessentials.client.setting.BooleanSetting;
import dev.yogi.yogiessentials.client.setting.EnumSetting;
import dev.yogi.yogiessentials.client.setting.NumberSetting;

public final class ChatPingsModule extends Module {
    public enum PingSound {
        EXPERIENCE,
        LEVEL_UP,
        AMETHYST,
        BELL
    }

    private final EnumSetting<PingSound> sound =
            new EnumSetting<>("Ping Sound", PingSound.EXPERIENCE, PingSound.class);
    private final NumberSetting volume =
            new NumberSetting("Volume (%)", 70.0, 0.0, 100.0, 5.0);
    private final BooleanSetting includeSystemMessages =
            new BooleanSetting("Include Server Messages", false);

    public ChatPingsModule() {
        super("Chat Pings", "Play a configurable alert when your username is mentioned in chat.", Category.CHAT);
        addSetting(sound);
        addSetting(volume);
        addSetting(includeSystemMessages);
    }

    public EnumSetting<PingSound> getSound() { return sound; }
    public NumberSetting getVolume() { return volume; }
    public BooleanSetting getIncludeSystemMessages() { return includeSystemMessages; }
}
