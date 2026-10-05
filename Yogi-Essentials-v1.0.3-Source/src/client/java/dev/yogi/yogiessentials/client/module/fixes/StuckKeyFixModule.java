package dev.yogi.yogiessentials.client.module.fixes;

import dev.yogi.yogiessentials.client.module.Category;
import dev.yogi.yogiessentials.client.module.Module;

public final class StuckKeyFixModule extends Module {
    public StuckKeyFixModule() {
        super(
                "Stuck-Key Fix",
                "Clears stale logical key states after focus loss, GUI entry, and fullscreen/window transitions without polling or changing normal focused gameplay input.",
                Category.FIXES
        );
        setEnabled(true);
    }
}
