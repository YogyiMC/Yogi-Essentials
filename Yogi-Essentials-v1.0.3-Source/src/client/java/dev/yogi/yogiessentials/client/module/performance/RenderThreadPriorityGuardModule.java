package dev.yogi.yogiessentials.client.module.performance;

import dev.yogi.yogiessentials.client.module.Category;
import dev.yogi.yogiessentials.client.module.Module;
import dev.yogi.yogiessentials.client.setting.NumberSetting;
import net.minecraft.class_310;

/**
 * Conservative scheduler hint for CPU-contention scenarios. Only the actual
 * client/render thread is adjusted; worker-pool priorities and Thread.yield()
 * behavior are intentionally left untouched for compatibility with renderer
 * and chunk mods.
 */
public final class RenderThreadPriorityGuardModule extends Module {
    private final NumberSetting priority = new NumberSetting("Render Thread Priority", 5, 5, 7, 1);
    private Thread ownedThread;
    private Integer originalPriority;
    private Integer appliedPriority;

    public RenderThreadPriorityGuardModule() {
        super(
                "Render Thread Priority Guard",
                "Gives the client/render thread a small scheduler priority advantage during CPU contention without rewriting worker thread priorities.",
                Category.PERFORMANCE
        );
        addSetting(priority);
    }

    /** Auto Optimizer keeps Java's normal priority so a very fast render loop cannot starve integrated-server/chunk workers. */
    public boolean prepareForAutoOptimizer() {
        boolean changed = priority.get().intValue() != Thread.NORM_PRIORITY;
        priority.set((double) Thread.NORM_PRIORITY);
        return changed;
    }

    @Override
    protected void onDisable() {
        restore();
    }

    public void tick(class_310 client) {
        if (!isEnabled() || client == null || client.field_1687 == null) return;
        Thread current = Thread.currentThread();
        int wanted = Math.max(Thread.NORM_PRIORITY, Math.min(Thread.MAX_PRIORITY, priority.get().intValue()));
        try {
            if (ownedThread != current) {
                restore();
                ownedThread = current;
                originalPriority = current.getPriority();
            }
            if (current.getPriority() != wanted) {
                current.setPriority(wanted);
                appliedPriority = wanted;
            }
        } catch (SecurityException ignored) {
        }
    }

    private void restore() {
        if (ownedThread == null || originalPriority == null || appliedPriority == null) {
            ownedThread = null;
            originalPriority = null;
            appliedPriority = null;
            return;
        }
        try {
            if (ownedThread.isAlive() && ownedThread.getPriority() == appliedPriority) {
                ownedThread.setPriority(originalPriority);
            }
        } catch (SecurityException ignored) {
        }
        ownedThread = null;
        originalPriority = null;
        appliedPriority = null;
    }
}
