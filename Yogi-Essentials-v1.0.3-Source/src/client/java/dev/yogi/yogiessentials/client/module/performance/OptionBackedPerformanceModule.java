package dev.yogi.yogiessentials.client.module.performance;

import dev.yogi.yogiessentials.client.module.Category;
import dev.yogi.yogiessentials.client.module.Module;
import net.minecraft.class_310;
import net.minecraft.class_315;











public abstract class OptionBackedPerformanceModule extends Module {

    private boolean applied;

    protected OptionBackedPerformanceModule(
            String name,
            String description
    ) {
        super(
                name,
                description,
                Category.PERFORMANCE
        );
    }

    @Override
    protected final void onEnable() {
        reconcileWithMinecraftOptions();
    }

    @Override
    protected final void onDisable() {
        reconcileWithMinecraftOptions();
    }

    




    public final void reconcileWithMinecraftOptions() {
        class_315 options = getOptionsIfReady();

        if (options == null) {
            return;
        }

        if (isEnabled()) {
            if (!applied) {
                capturePrevious(options);
                applied = true;
            }

            applyEnabledValue(options);
            return;
        }

        if (!applied) {
            return;
        }

        restorePrevious(options);
        clearPrevious();
        applied = false;
    }

    public final boolean isAppliedToMinecraftOptions() {
        return applied;
    }

    private static class_315 getOptionsIfReady() {
        class_310 client = class_310.method_1551();

        if (
                client == null
                        || client.field_1690 == null
        ) {
            return null;
        }

        return client.field_1690;
    }

    protected abstract void capturePrevious(class_315 options);

    protected abstract void applyEnabledValue(class_315 options);

    protected abstract void restorePrevious(class_315 options);

    protected abstract void clearPrevious();
}
