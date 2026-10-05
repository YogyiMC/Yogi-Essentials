package dev.yogi.yogiessentials.client.module.performance;

import dev.yogi.yogiessentials.client.module.Category;
import dev.yogi.yogiessentials.client.module.Module;
import dev.yogi.yogiessentials.client.setting.BooleanSetting;
import dev.yogi.yogiessentials.client.setting.NumberSetting;
import dev.yogi.yogiessentials.client.util.FrameTelemetry;
import net.minecraft.class_310;

/**
 * Adaptive chunk-work pressure controller inspired by the general idea of
 * budgeting chunk work around frame time. It never lowers terrain render
 * distance. Instead it temporarily lowers simulation pressure while frame
 * pacing is stressed, without flipping chunk-builder modes during gameplay.
 */
public final class AdaptiveChunkPacingModule extends Module {
    private final NumberSetting maxFrameTimePercent = new NumberSetting("Chunk Frame Budget (%)", 12, 5, 25, 1);
    private final BooleanSetting proactiveThrottling = new BooleanSetting("Proactive Throttling", true);
    private final BooleanSetting movementAware = new BooleanSetting("Movement-Aware Pacing", true);
    private final BooleanSetting teleportProtection = new BooleanSetting("Teleport Protection", true);
    private final NumberSetting minimumSimulation = new NumberSetting("Minimum Simulation Distance", 5, 5, 10, 1);
    private final NumberSetting recoverySeconds = new NumberSetting("Recovery Delay (seconds)", 4, 1, 15, 1);

    private Integer originalSimulation;
    private Integer appliedSimulation;
    private boolean hasLastPosition;
    private double lastX;
    private double lastY;
    private double lastZ;
    private long conservativeUntil;
    private long lastEvaluation;

    public AdaptiveChunkPacingModule() {
        super(
                "Adaptive Chunk Pacing",
                "Spreads chunk-related pressure across frames using frame-time, movement, and teleport-aware throttling without lowering render distance.",
                Category.PERFORMANCE
        );
        addSetting(maxFrameTimePercent);
        addSetting(proactiveThrottling);
        addSetting(movementAware);
        addSetting(teleportProtection);
        addSetting(minimumSimulation);
        addSetting(recoverySeconds);
    }

    @Override
    protected void onDisable() {
        restore(class_310.method_1551());
        hasLastPosition = false;
        conservativeUntil = 0L;
        lastEvaluation = 0L;
    }

    public void tick(class_310 client) {
        if (PerformanceOptimizer.optimizerOwnsRuntimeOptions()) return;
        if (!isEnabled() || client == null || client.field_1690 == null || client.field_1687 == null || client.field_1724 == null) {
            return;
        }

        long now = System.currentTimeMillis();
        double px = client.field_1724.method_23317();
        double py = client.field_1724.method_23318();
        double pz = client.field_1724.method_23321();
        if (teleportProtection.get() && hasLastPosition) {
            double dx = px - lastX;
            double dy = py - lastY;
            double dz = pz - lastZ;
            if (dx * dx + dy * dy + dz * dz > 64.0 * 64.0) {
                conservativeUntil = Math.max(conservativeUntil, now + 3500L);
            }
        }
        lastX = px;
        lastY = py;
        lastZ = pz;
        hasLastPosition = true;

        if (now - lastEvaluation < 250L) return;
        lastEvaluation = now;

        FrameTelemetry.Sample sample = FrameTelemetry.snapshot();
        if (!sample.available()) return;

        double targetFrameMs = targetFrameMs(client);
        double allowedChunkPressureMs = targetFrameMs * (maxFrameTimePercent.get() / 100.0);
        var velocity = client.field_1724.method_18798();
        double speedSq = velocity.field_1352 * velocity.field_1352 + velocity.field_1351 * velocity.field_1351 + velocity.field_1350 * velocity.field_1350;
        boolean movingFast = movementAware.get() && speedSq >= 0.35 * 0.35;
        boolean unstable = sample.stutters() >= 2
                || sample.varianceMsSquared() > Math.max(8.0, allowedChunkPressureMs * allowedChunkPressureMs)
                || sample.onePercentLow() < sample.averageFps() * 0.72;
        boolean proactive = proactiveThrottling.get() && sample.averageMs() > targetFrameMs * 0.88;
        boolean protect = now < conservativeUntil || unstable || proactive || movingFast;

        if (protect) {
            conservativeUntil = Math.max(conservativeUntil, now + Math.round(recoverySeconds.get() * 1000.0));
            applyPressure(client, movingFast || unstable);
        } else if (now >= conservativeUntil) {
            restore(client);
        }
    }

    private void applyPressure(class_310 client, boolean strong) {
        int current = client.field_1690.method_42510().method_41753();
        int minimum = minimumSimulation.get().intValue();
        if (originalSimulation == null) originalSimulation = current;

        int desired = Math.max(minimum, originalSimulation - (strong ? 2 : 1));
        desired = Math.min(desired, client.field_1690.method_42503().method_41753());

        if (current > desired && (appliedSimulation == null || desired < appliedSimulation)) {
            client.field_1690.method_42510().method_41748(desired);
            appliedSimulation = desired;
        }
    }

    private void restore(class_310 client) {
        if (client == null || client.field_1690 == null) return;
        if (originalSimulation != null && appliedSimulation != null
                && client.field_1690.method_42510().method_41753().equals(appliedSimulation)) {
            client.field_1690.method_42510().method_41748(originalSimulation);
        }
        originalSimulation = null;
        appliedSimulation = null;
    }

    private static double targetFrameMs(class_310 client) {
        int cap = PerformanceOptimizer.selectedTargetFps();
        if (cap > 0) return 1000.0 / cap;
        FrameTelemetry.Sample sample = FrameTelemetry.snapshot();
        double fps = sample.available() ? Math.max(60.0, sample.averageFps()) : 120.0;
        return 1000.0 / fps;
    }
}
