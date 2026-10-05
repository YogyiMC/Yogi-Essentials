package dev.yogi.yogiessentials.client.module.fixes;

import dev.yogi.yogiessentials.client.module.Category;
import dev.yogi.yogiessentials.client.module.Module;

public final class CenteredCrosshairFixModule extends Module {
    public CenteredCrosshairFixModule() {
        super(
                "Centered Crosshair Fix",
                "Keeps the vanilla crosshair on the exact scaled-screen center, including odd GUI dimensions and unusual GUI scales.",
                Category.FIXES
        );
        setEnabled(true);
    }
}
