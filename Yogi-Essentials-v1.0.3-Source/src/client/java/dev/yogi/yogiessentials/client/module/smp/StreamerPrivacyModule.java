package dev.yogi.yogiessentials.client.module.smp;

import dev.yogi.yogiessentials.client.module.Category;
import dev.yogi.yogiessentials.client.module.Module;





public final class StreamerPrivacyModule extends Module {
    public StreamerPrivacyModule() {
        super(
                "Streamer Privacy Mode",
                "Hides privacy-sensitive information from Yogi Essentials HUD/screens while streaming or screensharing without changing the underlying HUD configuration.",
                Category.SMP
        );
    }
}
