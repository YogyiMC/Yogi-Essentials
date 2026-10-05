package dev.yogi.yogiessentials.client.module.chat;

import dev.yogi.yogiessentials.client.module.Category;
import dev.yogi.yogiessentials.client.module.Module;

public final class ClickToCopyModule extends Module {
    public ClickToCopyModule() {
        super("Click to Copy", "Click an individual message in open chat to copy its plain text to the clipboard.", Category.CHAT);
    }
}
