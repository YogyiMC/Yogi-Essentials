package dev.yogi.yogiessentials.client.util;

import dev.yogi.yogiessentials.client.YogiEssentialsClient;
import dev.yogi.yogiessentials.client.module.performance.MicrostutterGuardModule;
import dev.yogi.yogiessentials.client.module.performance.ViewportCullingModule;
import dev.yogi.yogiessentials.client.module.fixes.WorldLoadStutterFixModule;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.class_1297;
import net.minecraft.class_2338;
import net.minecraft.class_238;
import net.minecraft.class_239;
import net.minecraft.class_243;
import net.minecraft.class_310;
import net.minecraft.class_3959;
import net.minecraft.class_3965;
import net.minecraft.class_4184;
import net.minecraft.class_4604;

/**
 * Renderer-side visibility helpers.
 *
 * Off-screen/frustum decisions remain per-frame and distance-independent.
 * Terrain occlusion is deliberately cached and budgeted because doing several
 * world raycasts for every renderable every frame can itself create the exact
 * microstutters this feature is supposed to remove. When the budget is
 * exhausted we fail open (render the object) rather than hitch or hide it.
 */
public final class ViewportCulling {
    private static final Map<Integer, OcclusionEntry> ENTITY_OCCLUSION = new HashMap<>();
    private static final Map<Long, OcclusionEntry> BLOCK_ENTITY_OCCLUSION = new HashMap<>();

    private static final long CACHE_TTL_TICKS = 4L;
    private static final double CAMERA_CACHE_MOVE_SQ = 0.20 * 0.20;
    private static final double TARGET_CACHE_MOVE_SQ = 0.20 * 0.20;

    private static Object cachedWorld;
    private static long budgetFrame = Long.MIN_VALUE;
    private static int entityChecksThisFrame;
    private static int blockEntityChecksThisFrame;
    private static long occlusionNanosThisFrame;
    private static long offscreenEntityRejects;
    private static long offscreenBlockEntityRejects;
    private static long occludedEntityRejects;
    private static long occludedBlockEntityRejects;
    private static long lastCachePruneTick = Long.MIN_VALUE;

    private static long coneFrame = Long.MIN_VALUE;
    private static class_243 coneCameraPos = class_243.field_1353;
    private static double coneFx, coneFy, coneFz;
    private static double coneRx, coneRz;
    private static double coneUx, coneUy, coneUz;
    private static double coneVerticalHalf, coneHorizontalHalf;
    private static double coneMarginDegrees = Double.NaN;
    private static double coneVerticalTanWithMargin;
    private static double coneHorizontalTanWithMargin;

    private ViewportCulling() {
    }

    private record OcclusionEntry(long tick, class_243 cameraPos, class_243 targetPos, boolean visible) {}

    public static ViewportCullingModule module() {
        if (YogiEssentialsClient.getModuleManager() == null) return null;
        return YogiEssentialsClient.getModuleManager().getModule(ViewportCullingModule.class);
    }

    public static boolean boxVisible(class_4604 frustum, class_238 box, double safetyMarginBlocks) {
        if (frustum == null || box == null) return true;
        double margin = Math.max(0.0, safetyMarginBlocks);
        return frustum.method_23093(margin > 0.0 ? box.method_1014(margin) : box);
    }

    /**
     * Camera-cone test independent of Minecraft's entity-distance rules. It has
     * no close/far exemption. Bounding radius is included so an object remains
     * visible while any meaningful part of it intersects the screen edge.
     */
    public static boolean pointVisible(class_243 center, double boundingRadius, double edgeMarginDegrees) {
        if (center == null) return true;
        return pointVisible(center.field_1352, center.field_1351, center.field_1350, boundingRadius, edgeMarginDegrees);
    }

    public static boolean pointVisible(double x, double y, double z, double boundingRadius, double edgeMarginDegrees) {
        class_310 client = class_310.method_1551();
        if (client == null || client.field_1690 == null || client.field_1773 == null) return true;

        class_4184 camera = client.field_1773.method_19418();
        if (camera == null || !camera.method_19332()) return true;
        prepareCone(client, camera);

        double dx = x - coneCameraPos.field_1352;
        double dy = y - coneCameraPos.field_1351;
        double dz = z - coneCameraPos.field_1350;
        if (dx * dx + dy * dy + dz * dz < 1.0e-10) return true;

        double forward = dx * coneFx + dy * coneFy + dz * coneFz;
        double radius = Math.max(0.0, boundingRadius);
        if (forward < -radius) return false;

        prepareConeMargin(edgeMarginDegrees);
        double right = dx * coneRx + dz * coneRz;
        double up = dx * coneUx + dy * coneUy + dz * coneUz;
        double safeForward = Math.max(0.0, forward);

        double horizontalLimit = safeForward * coneHorizontalTanWithMargin + radius;
        double verticalLimit = safeForward * coneVerticalTanWithMargin + radius;
        return Math.abs(right) <= horizontalLimit && Math.abs(up) <= verticalLimit;
    }

    private static void prepareCone(class_310 client, class_4184 camera) {
        long frame = FrameTelemetry.frameId();
        if (frame == coneFrame) return;
        coneFrame = frame;
        coneCameraPos = camera.method_71156();
        coneMarginDegrees = Double.NaN;

        double yaw = Math.toRadians(camera.method_19330());
        double pitch = Math.toRadians(camera.method_19329());
        double cosPitch = Math.cos(pitch);
        coneFx = -Math.sin(yaw) * cosPitch;
        coneFy = -Math.sin(pitch);
        coneFz = Math.cos(yaw) * cosPitch;
        coneRx = Math.cos(yaw);
        coneRz = Math.sin(yaw);
        coneUx = -Math.sin(yaw) * Math.sin(pitch);
        coneUy = Math.cos(pitch);
        coneUz = Math.cos(yaw) * Math.sin(pitch);

        double configuredFov = client.field_1690.method_41808().method_41753();
        coneVerticalHalf = Math.toRadians(Math.max(30.0, Math.min(120.0, configuredFov)) * 0.5);
        double aspect = 16.0 / 9.0;
        if (client.method_22683() != null && client.method_22683().method_4506() > 0) {
            aspect = (double) client.method_22683().method_4489()
                    / (double) client.method_22683().method_4506();
        }
        coneHorizontalHalf = Math.atan(Math.tan(coneVerticalHalf) * Math.max(0.5, aspect));
    }

    private static void prepareConeMargin(double edgeMarginDegrees) {
        double margin = Math.max(0.0, Math.min(20.0, edgeMarginDegrees));
        if (Double.compare(margin, coneMarginDegrees) == 0) return;
        coneMarginDegrees = margin;
        double radians = Math.toRadians(margin);
        coneVerticalTanWithMargin = Math.tan(Math.min(Math.toRadians(89.0), coneVerticalHalf + radians));
        coneHorizontalTanWithMargin = Math.tan(Math.min(Math.toRadians(89.0), coneHorizontalHalf + radians));
    }

    public static double boundingRadius(class_238 box) {
        if (box == null) return 0.75;
        double x = box.method_17939() * 0.5;
        double y = box.method_17940() * 0.5;
        double z = box.method_17941() * 0.5;
        return Math.sqrt(x * x + y * y + z * z);
    }

    public static boolean entityTerrainVisible(class_1297 entity, double safetyRadius) {
        if (WorldLoadStutterFixModule.pauseTerrainOcclusionNow()) return true;
        class_310 client = class_310.method_1551();
        if (client == null || client.field_1687 == null || client.field_1773 == null || entity == null) return true;
        class_4184 camera = client.field_1773.method_19418();
        if (camera == null || !camera.method_19332()) return true;
        class_243 start = camera.method_71156();
        class_238 box = entity.method_5829();
        class_243 center = box.method_1005();
        if (start.method_1025(center) <= safetyRadius * safetyRadius) return true;

        prepareWorldAndFrame(client);
        long tick = client.field_1687.method_75260();
        OcclusionEntry cached = ENTITY_OCCLUSION.get(entity.method_5628());
        if (cacheValid(cached, tick, start, center, true)) return cached.visible;

        if (!claimEntityCheck()) return true;

        double inset = Math.max(0.05, Math.min(0.25, box.method_17940() * 0.2));
        class_243 high = new class_243(center.field_1352, box.field_1325 - inset, center.field_1350);
        long checkStart = System.nanoTime();
        boolean visible = clearLine(client, start, center, entity, null);
        if (!visible) {
            visible = clearLine(client, start, high, entity, null);
        }
        occlusionNanosThisFrame += Math.max(0L, System.nanoTime() - checkStart);
        ENTITY_OCCLUSION.put(entity.method_5628(), new OcclusionEntry(tick, start, center, visible));
        return visible;
    }

    public static boolean blockEntityTerrainVisible(class_2338 pos, double safetyRadius) {
        if (WorldLoadStutterFixModule.pauseTerrainOcclusionNow()) return true;
        class_310 client = class_310.method_1551();
        if (client == null || client.field_1687 == null || client.field_1773 == null || pos == null) return true;
        class_4184 camera = client.field_1773.method_19418();
        if (camera == null || !camera.method_19332()) return true;
        class_243 start = camera.method_71156();
        class_243 center = class_243.method_24953(pos);
        if (start.method_1025(center) <= safetyRadius * safetyRadius) return true;

        prepareWorldAndFrame(client);
        long tick = client.field_1687.method_75260();
        long key = pos.method_10063();
        OcclusionEntry cached = BLOCK_ENTITY_OCCLUSION.get(key);
        if (cacheValid(cached, tick, start, center, false)) return cached.visible;

        if (!claimBlockEntityCheck()) return true;

        class_1297 contextEntity = camera.method_19331();
        long checkStart = System.nanoTime();
        boolean visible = clearLine(client, start, center, contextEntity, pos);
        if (!visible) {
            visible = clearLine(client, start, center.method_1031(0.0, 0.40, 0.0), contextEntity, pos);
        }
        occlusionNanosThisFrame += Math.max(0L, System.nanoTime() - checkStart);
        BLOCK_ENTITY_OCCLUSION.put(key, new OcclusionEntry(tick, start, center, visible));
        return visible;
    }

    private static boolean cacheValid(OcclusionEntry entry, long tick, class_243 cameraPos,
                                      class_243 targetPos, boolean movingTarget) {
        if (entry == null) return false;
        if (tick < entry.tick || tick - entry.tick > CACHE_TTL_TICKS) return false;
        if (entry.cameraPos.method_1025(cameraPos) > CAMERA_CACHE_MOVE_SQ) return false;
        return !movingTarget || entry.targetPos.method_1025(targetPos) <= TARGET_CACHE_MOVE_SQ;
    }

    private static void prepareWorldAndFrame(class_310 client) {
        if (client.field_1687 != cachedWorld) {
            cachedWorld = client.field_1687;
            ENTITY_OCCLUSION.clear();
            BLOCK_ENTITY_OCCLUSION.clear();
            lastCachePruneTick = Long.MIN_VALUE;
        }

        long tick = client.field_1687 == null ? 0L : client.field_1687.method_75260();
        if (lastCachePruneTick == Long.MIN_VALUE || tick - lastCachePruneTick >= 40L) {
            lastCachePruneTick = tick;
            long cutoff = tick - 40L;
            if (ENTITY_OCCLUSION.size() > 512) {
                ENTITY_OCCLUSION.values().removeIf(entry -> entry.tick < cutoff);
            }
            if (BLOCK_ENTITY_OCCLUSION.size() > 256) {
                BLOCK_ENTITY_OCCLUSION.values().removeIf(entry -> entry.tick < cutoff);
            }
        }
        if (ENTITY_OCCLUSION.size() > 4096) trimEntries(ENTITY_OCCLUSION, 512);
        if (BLOCK_ENTITY_OCCLUSION.size() > 2048) trimEntries(BLOCK_ENTITY_OCCLUSION, 256);

        long frame = FrameTelemetry.frameId();
        if (frame != budgetFrame) {
            budgetFrame = frame;
            entityChecksThisFrame = 0;
            blockEntityChecksThisFrame = 0;
            occlusionNanosThisFrame = 0L;
        }
    }


    private static <K> void trimEntries(Map<K, OcclusionEntry> map, int limit) {
        var iterator = map.entrySet().iterator();
        int removed = 0;
        while (iterator.hasNext() && removed < limit) {
            iterator.next();
            iterator.remove();
            removed++;
        }
    }

    private static boolean claimEntityCheck() {
        ViewportCullingModule module = module();
        int configured = module == null ? 64 : module.entityOcclusionChecksPerFrame();
        double scale = MicrostutterGuardModule.activeScale() * WorldLoadStutterFixModule.activeWorkScale();
        int budget = Math.max(8, (int) Math.round(configured * scale));
        long timeBudget = Math.max(200_000L, Math.round(750_000L * Math.max(0.35, scale)));
        if (entityChecksThisFrame >= budget || occlusionNanosThisFrame >= timeBudget) return false;
        entityChecksThisFrame++;
        return true;
    }

    private static boolean claimBlockEntityCheck() {
        ViewportCullingModule module = module();
        int configured = module == null ? 32 : module.blockEntityOcclusionChecksPerFrame();
        double scale = MicrostutterGuardModule.activeScale() * WorldLoadStutterFixModule.activeWorkScale();
        int budget = Math.max(4, (int) Math.round(configured * scale));
        long timeBudget = Math.max(200_000L, Math.round(750_000L * Math.max(0.35, scale)));
        if (blockEntityChecksThisFrame >= budget || occlusionNanosThisFrame >= timeBudget) return false;
        blockEntityChecksThisFrame++;
        return true;
    }

    public static void recordOffscreenEntityReject() { offscreenEntityRejects++; }
    public static void recordOffscreenBlockEntityReject() { offscreenBlockEntityRejects++; }
    public static void recordOccludedEntityReject() { occludedEntityRejects++; }
    public static void recordOccludedBlockEntityReject() { occludedBlockEntityRejects++; }

    public static long offscreenEntityRejects() { return offscreenEntityRejects; }
    public static long offscreenBlockEntityRejects() { return offscreenBlockEntityRejects; }
    public static long occludedEntityRejects() { return occludedEntityRejects; }
    public static long occludedBlockEntityRejects() { return occludedBlockEntityRejects; }

    public static void clearCaches() {
        ENTITY_OCCLUSION.clear();
        BLOCK_ENTITY_OCCLUSION.clear();
        cachedWorld = null;
        budgetFrame = Long.MIN_VALUE;
        entityChecksThisFrame = 0;
        blockEntityChecksThisFrame = 0;
        occlusionNanosThisFrame = 0L;
        coneFrame = Long.MIN_VALUE;
        coneMarginDegrees = Double.NaN;
        lastCachePruneTick = Long.MIN_VALUE;
    }

    private static boolean clearLine(class_310 client, class_243 start, class_243 end,
                                     class_1297 contextEntity, class_2338 targetBlock) {
        if (client.field_1687 == null) return true;
        class_3959 context = new class_3959(
                start, end, class_3959.class_3960.field_17558,
                class_3959.class_242.field_1348, contextEntity
        );
        class_3965 hit = client.field_1687.method_17742(context);
        if (hit == null || hit.method_17783() == class_239.class_240.field_1333) return true;
        if (targetBlock != null && targetBlock.equals(hit.method_17777())) return true;

        double hitDistanceSq = start.method_1025(hit.method_17784());
        double targetDistanceSq = start.method_1025(end);
        return hitDistanceSq + 0.04 >= targetDistanceSq;
    }
}
