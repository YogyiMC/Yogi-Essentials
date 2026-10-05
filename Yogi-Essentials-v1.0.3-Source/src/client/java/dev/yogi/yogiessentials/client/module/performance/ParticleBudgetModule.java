package dev.yogi.yogiessentials.client.module.performance;

import dev.yogi.yogiessentials.client.module.Category;
import dev.yogi.yogiessentials.client.module.Module;
import dev.yogi.yogiessentials.client.setting.BooleanSetting;
import dev.yogi.yogiessentials.client.setting.NumberSetting;
import dev.yogi.yogiessentials.client.util.FrameTelemetry;
import dev.yogi.yogiessentials.client.util.ViewportCulling;
import net.minecraft.class_310;

/**
 * Particle allocation/render-pressure controller inspired by the useful ideas
 * behind dedicated particle optimization mods: reject work that cannot be seen,
 * reduce distant bursts before allocation, and keep an adaptive per-tick budget.
 */
public final class ParticleBudgetModule extends Module {
    private final NumberSetting maxDistance = new NumberSetting("Particle Distance", 56, 16, 160, 4);
    private final NumberSetting alwaysKeepRadius = new NumberSetting("Always Keep Radius", 12, 4, 32, 2);
    private final BooleanSetting cullBehindCamera = new BooleanSetting("Cull Off-Screen Particles", true);
    private final NumberSetting offscreenStart = new NumberSetting("Off-Screen Cull Start", 16, 8, 64, 2);
    private final NumberSetting farReductionStart = new NumberSetting("Far Reduction Start", 24, 8, 96, 4);
    private final NumberSetting farSpawnPercent = new NumberSetting("Far Particle Spawn (%)", 65, 20, 100, 5);
    private final NumberSetting maxPerTick = new NumberSetting("Particle Spawn Budget", 700, 100, 3000, 50);
    private final BooleanSetting adaptive = new BooleanSetting("Adaptive Budget", true);

    private int spawnedThisTick;
    private int adaptiveBudget = 700;
    private long lastAdapt;
    private long sampleSequence;

    public ParticleBudgetModule() {
        super("Particle Spawn Budget",
                "Culls unseen/distant particles before allocation, reduces far particle bursts, and adapts spawn work to frame pressure.",
                Category.PERFORMANCE);
        addSetting(maxDistance);
        addSetting(alwaysKeepRadius);
        addSetting(cullBehindCamera);
        addSetting(offscreenStart);
        addSetting(farReductionStart);
        addSetting(farSpawnPercent);
        addSetting(maxPerTick);
        addSetting(adaptive);
    }

    public void beginTick(class_310 client) {
        spawnedThisTick = 0;
        int configured = Math.max(100, maxPerTick.get().intValue());
        adaptiveBudget = Math.min(adaptiveBudget <= 0 ? configured : adaptiveBudget, configured);
        if (!isEnabled() || !adaptive.get() || client == null || client.field_1687 == null) {
            adaptiveBudget = configured;
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastAdapt < 750L) return;
        lastAdapt = now;
        FrameTelemetry.Sample sample = FrameTelemetry.snapshot();
        if (!sample.available()) return;
        double lowRatio = sample.onePercentLow() / Math.max(1.0, sample.averageFps());
        boolean stressed = sample.stutters() >= 2 || lowRatio < 0.62 || sample.varianceMsSquared() > 18.0;
        if (stressed) {
            adaptiveBudget = Math.max(100, Math.min(configured, adaptiveBudget - Math.max(50, configured / 8)));
        } else {
            adaptiveBudget = Math.min(configured, adaptiveBudget + Math.max(25, configured / 16));
        }
    }

    public boolean shouldCull(double x, double y, double z) {
        if (!isEnabled()) return false;
        class_310 client = class_310.method_1551();
        if (client == null || client.field_1724 == null) return false;

        double dx = client.field_1724.method_23317() - x;
        double dy = client.field_1724.method_23318() - y;
        double dz = client.field_1724.method_23321() - z;
        double distanceSq = dx * dx + dy * dy + dz * dz;

        double max = maxDistance.get();
        if (distanceSq > max * max) return true;

        double keepRadius = alwaysKeepRadius.get();
        if (distanceSq > keepRadius * keepRadius) {
            double offscreen = offscreenStart.get();
            if (cullBehindCamera.get() && distanceSq >= offscreen * offscreen
                    && !ViewportCulling.pointVisible(x, y, z, 0.55, 8.0)) {
                return true;
            }

            double farStart = farReductionStart.get();
            if (distanceSq >= farStart * farStart
                    && !sampleKeep(x, y, z, farSpawnPercent.get() / 100.0)) {
                return true;
            }
        }

        int budget = adaptive.get() ? adaptiveBudget : Math.max(100, maxPerTick.get().intValue());
        budget = Math.max(80, (int) Math.round(budget * MicrostutterGuardModule.activeScale()));
        if (spawnedThisTick >= budget) return true;
        spawnedThisTick++;
        return false;
    }

    private boolean sampleKeep(double x, double y, double z, double keepFraction) {
        if (keepFraction >= 0.999) return true;
        if (keepFraction <= 0.0) return false;
        long bits = Double.doubleToLongBits(x * 0.754877666)
                ^ Long.rotateLeft(Double.doubleToLongBits(y * 0.569840296), 17)
                ^ Long.rotateLeft(Double.doubleToLongBits(z * 0.438289921), 31)
                ^ (++sampleSequence * 0x9E3779B97F4A7C15L);
        bits ^= bits >>> 30;
        bits *= 0xBF58476D1CE4E5B9L;
        bits ^= bits >>> 27;
        bits *= 0x94D049BB133111EBL;
        bits ^= bits >>> 31;
        long bucket = bits & 1023L;
        return bucket < Math.round(keepFraction * 1024.0);
    }
}
