package dev.yogi.yogiessentials.client.module.performance;

import dev.yogi.yogiessentials.client.module.Category;
import dev.yogi.yogiessentials.client.module.Module;
import dev.yogi.yogiessentials.client.setting.BooleanSetting;

/**
 * Keeps chat-search normalization/index maintenance off the render thread.
 * Actual Minecraft HUD mutation remains on the client thread for correctness.
 */
public final class AsyncChatIndexModule extends Module {
    private final BooleanSetting backgroundIndexing = new BooleanSetting("Background Search Indexing", true);

    public AsyncChatIndexModule() {
        super(
                "Async Chat Index",
                "Builds the chat-search index on a dedicated lightweight worker so large chat histories add less work to the render thread.",
                Category.PERFORMANCE
        );
        addSetting(backgroundIndexing);
    }

    public boolean backgroundIndexingEnabled() {
        return isEnabled() && backgroundIndexing.get();
    }
}
