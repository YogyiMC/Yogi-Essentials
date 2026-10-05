package dev.yogi.yogiessentials.client.module.fixes;

import dev.yogi.yogiessentials.client.module.Category;
import dev.yogi.yogiessentials.client.module.Module;

public final class FocusRecoveryFixModule extends Module {
    public FocusRecoveryFixModule() {
        super(
                "Focus Recovery Fix",
                "Restores normal mouse capture after returning to a world when Minecraft has focus and no screen is open.",
                Category.FIXES
        );
        setEnabled(true);
    }
}
