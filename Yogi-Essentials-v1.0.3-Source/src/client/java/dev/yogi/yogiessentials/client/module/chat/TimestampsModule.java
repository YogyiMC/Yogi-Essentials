package dev.yogi.yogiessentials.client.module.chat;

import dev.yogi.yogiessentials.client.module.Category;
import dev.yogi.yogiessentials.client.module.Module;
import dev.yogi.yogiessentials.client.setting.EnumSetting;

public final class TimestampsModule extends Module {
    public enum Format { H24, H12 }

    private final EnumSetting<Format> format = new EnumSetting<>("Time Format", Format.H24, Format.class);

    public TimestampsModule() {
        super("Timestamps", "Prefix displayed chat messages with the local time they arrived.", Category.CHAT);
        addSetting(format);
    }

    public EnumSetting<Format> getFormat() { return format; }
}
