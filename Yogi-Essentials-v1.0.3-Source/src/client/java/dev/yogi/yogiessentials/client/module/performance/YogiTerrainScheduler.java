package dev.yogi.yogiessentials.client.module.performance;

import dev.yogi.yogiessentials.client.YogiEssentialsClient;
import dev.yogi.yogiessentials.client.module.fixes.WorldLoadStutterFixModule;
import dev.yogi.yogiessentials.client.util.FrameTelemetry;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.class_310;

/**
 * Allocation-light, frame-budgeted terrain rebuild scheduler. It coalesces duplicate
 * section rebuild requests and releases only a small amount of useful work per frame.
 */
public final class YogiTerrainScheduler {
    @FunctionalInterface
    public interface RebuildSubmitter {
        void submit(int x, int y, int z, boolean important);
    }

    private record SectionKey(int x, int y, int z) {}

    private static final class Request {
        final SectionKey key;
        boolean important;
        long sequence;

        Request(SectionKey key, boolean important, long sequence) {
            this.key = key;
            this.important = important;
            this.sequence = sequence;
        }
    }

    private static final Map<SectionKey, Request> PENDING = new LinkedHashMap<>();
    private static final ThreadLocal<Boolean> FLUSHING = ThreadLocal.withInitial(() -> false);
    private static long sequence;
    private static long submitted;
    private static long coalesced;
    private static long dropped;
    private static long budgetFrame = Long.MIN_VALUE;
    private static long flushedFrame = Long.MIN_VALUE;
    private static int submittedThisFrame;
    private static long spentNanosThisFrame;

    private static final int MAX_SELECTION = 40;
    private static final Request[] FRONT_CANDIDATES = new Request[MAX_SELECTION];
    private static final double[] FRONT_SCORES = new double[MAX_SELECTION];
    private static final Request[] BEHIND_CANDIDATES = new Request[MAX_SELECTION];
    private static final double[] BEHIND_SCORES = new double[MAX_SELECTION];

    private YogiTerrainScheduler() {}

    public static boolean shouldIntercept() {
        CustomTerrainRendererModule module = module();
        if (module == null || !module.isEnabled()) return false;
        return module.getBackendStatus().backend() == TerrainBackendManager.EffectiveBackend.YOGI_OPENGL;
    }

    public static boolean isFlushing() {
        return FLUSHING.get();
    }

    public static void enqueue(int x, int y, int z, boolean important) {
        CustomTerrainRendererModule module = module();
        if (module == null || !shouldIntercept()) return;

        SectionKey key = new SectionKey(x, y, z);
        synchronized (PENDING) {
            Request existing = PENDING.get(key);
            if (existing != null && module.coalesceRebuilds()) {
                existing.important |= important;
                existing.sequence = ++sequence;
                coalesced++;
                return;
            }

            int maxQueued = module.maxQueuedRebuilds();
            if (PENDING.size() >= maxQueued) {
                Request worst = findWorstCandidate();
                if (worst != null && (!worst.important || important)) {
                    PENDING.remove(worst.key);
                    dropped++;
                } else if (!important) {
                    dropped++;
                    return;
                }
            }
            PENDING.put(key, new Request(key, important, ++sequence));
        }
    }

    private static void prepareFrameBudget() {
        long frame = FrameTelemetry.frameId();
        if (frame != budgetFrame) {
            budgetFrame = frame;
            submittedThisFrame = 0;
            spentNanosThisFrame = 0L;
        }
    }

    /**
     * Flush at most once per rendered frame. The old implementation built a
     * PriorityQueue + ArrayList of scored requests on both updateChunks HEAD and
     * TAIL. At very high FPS that generated thousands of short-lived objects per
     * second and could show up as periodic GC/frame-time dips. A tiny number of
     * rebuilds is selected by direct scans instead, producing no per-frame queue.
     */
    public static int flush(RebuildSubmitter submitter) {
        CustomTerrainRendererModule module = module();
        if (module == null || !shouldIntercept() || submitter == null) return 0;

        prepareFrameBudget();
        if (flushedFrame == budgetFrame) return 0;
        flushedFrame = budgetFrame;

        int maxCount = scaledRebuildCount(module);
        int remainingCount = Math.max(0, maxCount - submittedThisFrame);
        long totalBudgetNanos = Math.max(100_000L, Math.round(frameBudgetMs(module) * 1_000_000.0));
        long remainingBudgetNanos = Math.max(0L, totalBudgetNanos - spentNanosThisFrame);
        if (remainingCount <= 0 || remainingBudgetNanos <= 0L) return 0;

        class_310 client = class_310.method_1551();
        double px = 0.0, py = 0.0, pz = 0.0;
        double lookX = 0.0, lookZ = 1.0;
        if (client != null && client.field_1724 != null) {
            px = client.field_1724.method_23317() / 16.0;
            py = client.field_1724.method_23318() / 16.0;
            pz = client.field_1724.method_23321() / 16.0;
            double yaw = Math.toRadians(client.field_1724.method_36454());
            lookX = -Math.sin(yaw);
            lookZ = Math.cos(yaw);
            if (module.prioritizeTravelDirection()) {
                double vx = client.field_1724.method_18798().field_1352;
                double vz = client.field_1724.method_18798().field_1350;
                double speedSq = vx * vx + vz * vz;
                if (speedSq > 0.0025) {
                    double inv = 1.0 / Math.sqrt(speedSq);
                    vx *= inv;
                    vz *= inv;
                    lookX = lookX * 0.55 + vx * 0.45;
                    lookZ = lookZ * 0.55 + vz * 0.45;
                    double lenSq = lookX * lookX + lookZ * lookZ;
                    if (lenSq > 0.0001) {
                        double lookInv = 1.0 / Math.sqrt(lenSq);
                        lookX *= lookInv;
                        lookZ *= lookInv;
                    }
                }
            }
        }

        long start = System.nanoTime();
        int count = 0;
        int behindCount = 0;
        int behindLimit = module.deferBehindCamera()
                ? Math.max(0, (int) Math.floor(remainingCount * module.behindCameraSharePercent() / 100.0))
                : 0;
        int frontCount = 0;
        int behindCandidateCount = 0;

        synchronized (PENDING) {
            if (PENDING.isEmpty()) return 0;
            int frontLimit = Math.min(remainingCount, MAX_SELECTION);
            int backLimit = Math.min(behindLimit, MAX_SELECTION);
            int scanned = 0;
            long selectionDeadline = start + Math.min(250_000L, Math.max(75_000L, remainingBudgetNanos / 3L));
            for (Request request : PENDING.values()) {
                scanned++;
                double dx = request.key.x - px;
                double dy = request.key.y - py;
                double dz = request.key.z - pz;
                double horizontalSq = dx * dx + dz * dz;
                double dot = 1.0;
                if (horizontalSq > 0.000001) {
                    dot = (dx * lookX + dz * lookZ) / Math.sqrt(horizontalSq);
                }
                boolean behind = module.deferBehindCamera()
                        && !request.important
                        && horizontalSq > 9.0
                        && dot < -0.10;

                double candidateScore = scoreFromComponents(
                        request, module, dx, dy, dz, horizontalSq, dot);
                if (behind) {
                    behindCandidateCount = insertCandidate(request, candidateScore,
                            BEHIND_CANDIDATES, BEHIND_SCORES, behindCandidateCount, backLimit);
                } else {
                    frontCount = insertCandidate(request, candidateScore,
                            FRONT_CANDIDATES, FRONT_SCORES, frontCount, frontLimit);
                }
                if (scanned >= 96 && frontCount + behindCandidateCount >= Math.min(remainingCount, 4)
                        && System.nanoTime() >= selectionDeadline) {
                    break;
                }
            }
        }

        FLUSHING.set(true);
        try {
            int frontIndex = 0;
            int behindIndex = 0;
            while (count < remainingCount) {
                if (count > 0 && System.nanoTime() - start >= remainingBudgetNanos) break;

                boolean haveFront = frontIndex < frontCount;
                boolean haveBehind = behindIndex < behindCandidateCount && behindCount < behindLimit;
                if (!haveFront && !haveBehind) break;

                Request best;
                boolean useBehind;
                if (!haveFront) {
                    useBehind = true;
                } else if (!haveBehind) {
                    useBehind = false;
                } else {
                    useBehind = BEHIND_SCORES[behindIndex] < FRONT_SCORES[frontIndex];
                }

                if (useBehind) {
                    best = BEHIND_CANDIDATES[behindIndex++];
                    behindCount++;
                } else {
                    best = FRONT_CANDIDATES[frontIndex++];
                }

                boolean removed;
                synchronized (PENDING) {
                    removed = PENDING.remove(best.key) != null;
                }
                if (!removed) continue;

                submitter.submit(best.key.x, best.key.y, best.key.z, best.important);
                count++;
                submitted++;
            }
        } finally {
            FLUSHING.set(false);
            clearCandidateArrays(frontCount, behindCandidateCount);
            long spent = Math.max(0L, System.nanoTime() - start);
            submittedThisFrame += count;
            spentNanosThisFrame += spent;
        }
        return count;
    }

    private static int insertCandidate(Request request, double score,
                                       Request[] requests, double[] scores, int count, int limit) {
        if (limit <= 0) return 0;
        int index;
        if (count < limit) {
            index = count++;
        } else {
            index = limit - 1;
            if (score >= scores[index]) return count;
        }
        requests[index] = request;
        scores[index] = score;
        while (index > 0 && scores[index] < scores[index - 1]) {
            double previousScore = scores[index - 1];
            scores[index - 1] = scores[index];
            scores[index] = previousScore;
            Request previousRequest = requests[index - 1];
            requests[index - 1] = requests[index];
            requests[index] = previousRequest;
            index--;
        }
        return count;
    }

    private static void clearCandidateArrays(int frontCount, int behindCount) {
        for (int i = 0; i < frontCount; i++) FRONT_CANDIDATES[i] = null;
        for (int i = 0; i < behindCount; i++) BEHIND_CANDIDATES[i] = null;
    }

    private static double scoreFromComponents(Request request, CustomTerrainRendererModule module,
                                               double dx, double dy, double dz,
                                               double horizontalSq, double dot) {
        double distanceSq = dx * dx + dy * dy + dz * dz;
        double result = module.prioritizeNearby() ? distanceSq : request.sequence * 0.0001;
        if (request.important) result -= 10_000.0;
        if (module.prioritizeCameraFacing() && horizontalSq > 0.000001) {
            result += (1.0 - dot) * 24.0;
        }
        result -= Math.min(200.0, Math.max(0L, sequence - request.sequence) * 0.02);
        return result;
    }

    private static int scaledRebuildCount(CustomTerrainRendererModule module) {
        int base = module.maxRebuildsPerFrame();
        double scale = MicrostutterGuardModule.activeScale() * WorldLoadStutterFixModule.activeWorkScale();
        FrameTelemetry.Sample sample = FrameTelemetry.snapshot();
        boolean healthy = false;
        if (sample.available()) {
            double lowRatio = sample.onePercentLow() / Math.max(1.0, sample.averageFps());
            if (sample.stutters() >= 2 || lowRatio < 0.65) scale = Math.min(scale, 0.35);
            else if (sample.stutters() >= 1 || lowRatio < 0.78) scale = Math.min(scale, 0.55);
            else if (lowRatio < 0.88) scale = Math.min(scale, 0.75);
            healthy = sample.stutters() == 0 && lowRatio >= 0.90 && sample.varianceMsSquared() < 6.0;
        }

        int result = Math.max(1, (int) Math.floor(base * Math.max(0.25, scale)));
        if (module.adaptiveQueueBackpressure() && healthy) {
            int pending;
            synchronized (PENDING) { pending = PENDING.size(); }
            int threshold = Math.max(1, module.maxQueuedRebuilds() * module.queuePressureThresholdPercent() / 100);
            if (pending >= threshold) {
                result = Math.min(base + module.healthyBurstRebuilds(), result + module.healthyBurstRebuilds());
            }
        }
        return result;
    }

    private static double frameBudgetMs(CustomTerrainRendererModule module) {
        int target = PerformanceOptimizer.selectedTargetFps();
        FrameTelemetry.Sample sample = FrameTelemetry.snapshot();
        double frameMs;
        if (target > 0) {
            frameMs = 1000.0 / target;
        } else {
            double fps = sample.available() ? Math.max(60.0, sample.averageFps()) : 144.0;
            frameMs = 1000.0 / fps;
        }

        double budget = Math.max(0.10, frameMs * module.chunkFrameBudgetPercent() / 100.0);
        if (module.adaptiveQueueBackpressure() && sample.available() && sample.stutters() == 0) {
            double lowRatio = sample.onePercentLow() / Math.max(1.0, sample.averageFps());
            int pending;
            synchronized (PENDING) { pending = PENDING.size(); }
            int threshold = Math.max(1, module.maxQueuedRebuilds() * module.queuePressureThresholdPercent() / 100);
            if (pending >= threshold && lowRatio >= 0.90) budget *= 1.25;
        }
        return budget;
    }

    private static Request findWorstCandidate() {
        Request oldest = null;
        for (Request request : PENDING.values()) {
            if (oldest == null) oldest = request;
            if (!request.important) return request;
        }
        return oldest;
    }

    public static int pendingCount() {
        synchronized (PENDING) { return PENDING.size(); }
    }

    public static long submittedCount() { return submitted; }
    public static long coalescedCount() { return coalesced; }
    public static long droppedCount() { return dropped; }

    public static void clear() {
        synchronized (PENDING) { PENDING.clear(); }
        budgetFrame = Long.MIN_VALUE;
        flushedFrame = Long.MIN_VALUE;
        submittedThisFrame = 0;
        spentNanosThisFrame = 0L;
    }

    private static CustomTerrainRendererModule module() {
        if (YogiEssentialsClient.getModuleManager() == null) return null;
        return YogiEssentialsClient.getModuleManager().getModule(CustomTerrainRendererModule.class);
    }
}
