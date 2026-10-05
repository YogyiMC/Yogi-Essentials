package dev.yogi.yogiessentials.client.module.fixes;

import dev.yogi.yogiessentials.client.module.Category;
import dev.yogi.yogiessentials.client.module.Module;

public final class WorldUnloadCleanupModule extends Module {
    public WorldUnloadCleanupModule() {
        super(
                "World-Unload Cleanup",
                "Clears Yogi Essentials' world/session-only caches when leaving or replacing a world without forcing garbage collection during gameplay.",
                Category.FIXES
        );
        setEnabled(true);
    }
}
