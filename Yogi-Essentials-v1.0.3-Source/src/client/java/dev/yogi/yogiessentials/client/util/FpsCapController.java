package dev.yogi.yogiessentials.client.util;

import dev.yogi.yogiessentials.client.module.performance.PerformanceOptimizer;
import java.util.concurrent.locks.LockSupport;
import net.minecraft.class_310;

/**
 * Low-overhead FPS target controller.
 *
 * For normal targets (<= 260 FPS), Minecraft's native limiter owns pacing. That
 * path is already integrated with the renderer and avoids a second wait/spin loop
 * competing with capture/encoder threads. Yogi only uses its custom limiter for
 * targets above Minecraft's 260-FPS "Unlimited" sentinel.
 */
public final class FpsCapController {
    private static final long MIN_SPIN_MARGIN_NANOS = 75_000L;
    private static final long DEFAULT_PARK_SAFETY_NANOS = 250_000L;
    private static final long MAX_PARK_SAFETY_NANOS = 750_000L;

    private static long lastPresentedNanos;
    private static long lastOptionSyncNanos;
    private static long parkSafetyNanos = DEFAULT_PARK_SAFETY_NANOS;
    private static int lastTarget = -1;

    private FpsCapController() {}

    public static void reset() {
        lastPresentedNanos = 0L;
        lastTarget = -1;
        parkSafetyNanos = DEFAULT_PARK_SAFETY_NANOS;
    }

    public static void paceFrame() {
        int target = PerformanceOptimizer.selectedTargetFps();
        synchronizeVanillaLimiter(target);

        if (target <= 260) {
            lastPresentedNanos = 0L;
            lastTarget = target;
            return;
        }

        long period = Math.max(1L, 1_000_000_000L / target);
        long now = System.nanoTime();
        if (target != lastTarget || lastPresentedNanos == 0L) {
            lastTarget = target;
            lastPresentedNanos = now;
            parkSafetyNanos = DEFAULT_PARK_SAFETY_NANOS;
            return;
        }

        long deadline = lastPresentedNanos + period;
        long remaining = deadline - now;
        if (remaining <= 0L) {
            lastPresentedNanos = now;
            return;
        }

        long safety = Math.min(MAX_PARK_SAFETY_NANOS,
                Math.max(MIN_SPIN_MARGIN_NANOS, parkSafetyNanos));
        if (remaining > safety + 150_000L) {
            long requested = remaining - safety;
            long beforePark = System.nanoTime();
            LockSupport.parkNanos(requested);
            long actual = Math.max(0L, System.nanoTime() - beforePark);
            long overshoot = Math.max(0L, actual - requested);
            long desiredSafety = Math.min(MAX_PARK_SAFETY_NANOS,
                    Math.max(MIN_SPIN_MARGIN_NANOS, overshoot + 100_000L));
            if (desiredSafety > parkSafetyNanos) {
                parkSafetyNanos = desiredSafety;
            } else {
                parkSafetyNanos = Math.max(MIN_SPIN_MARGIN_NANOS,
                        (parkSafetyNanos * 31L + desiredSafety) / 32L);
            }
        }

        while (System.nanoTime() < deadline) {
            Thread.onSpinWait();
        }
        lastPresentedNanos = System.nanoTime();
    }

    private static void synchronizeVanillaLimiter(int target) {
        long now = System.nanoTime();
        if (now - lastOptionSyncNanos < 1_000_000_000L) return;
        lastOptionSyncNanos = now;

        class_310 client = class_310.method_1551();
        if (client == null || client.field_1690 == null) return;

        int desiredVanillaCap = target > 0 && target <= 260 ? target : 260;
        if (client.field_1690.method_42524().method_41753() != desiredVanillaCap) {
            client.field_1690.method_42524().method_41748(desiredVanillaCap);
        }

        if (target > 0 && client.field_1690.method_42433().method_41753()) {
            client.field_1690.method_42433().method_41748(false);
        }
    }
}
