package dev.yogi.yogiessentials.client.module.chat;

import dev.yogi.yogiessentials.client.module.Category;
import dev.yogi.yogiessentials.client.module.Module;

public final class RepeatedMessageStackingModule extends Module {
    public RepeatedMessageStackingModule() {
        super("Repeated Message Stacking", "Collapse consecutive duplicate chat lines and show a repeat counter.", Category.CHAT);
    }
}
