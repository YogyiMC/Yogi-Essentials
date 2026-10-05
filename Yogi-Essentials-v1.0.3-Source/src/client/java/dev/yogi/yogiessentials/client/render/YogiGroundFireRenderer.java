package dev.yogi.yogiessentials.client.render;

import dev.yogi.yogiessentials.client.YogiEssentialsClient;
import dev.yogi.yogiessentials.client.module.visual.LowFireModule;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.class_2246;
import net.minecraft.class_2338;
import net.minecraft.class_243;
import net.minecraft.class_2680;
import net.minecraft.class_310;
import net.minecraft.class_4184;
import net.minecraft.class_4587;
import net.minecraft.class_4597;
import net.minecraft.class_4770;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Resource-pack-independent Small Ground Fire renderer.
 *
 * <p>New/removed fire is tracked from ClientWorld block updates so flint-and-
 * steel/server block changes are visible on the next render without waiting for
 * a polling interval. A slower nearby reconciliation scan only catches chunk
 * loads/missed updates and is not used for normal per-fire latency.</p>
 */
public final class YogiGroundFireRenderer {

    private static final int HORIZONTAL_SCAN_RADIUS = 24;
    private static final int VERTICAL_SCAN_RADIUS = 20;
    private static final int RECONCILE_TICKS = 20;

    private static final Set<class_2338> visibleFire = new LinkedHashSet<>();
    private static int ticksUntilReconcile;
    private static boolean wasActive;
    private static boolean initialized;

    private YogiGroundFireRenderer() {
    }

    public static void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;

        ClientTickEvents.END_CLIENT_TICK.register(YogiGroundFireRenderer::tick);
        WorldRenderEvents.BEFORE_DEBUG_RENDER.register(YogiGroundFireRenderer::render);
    }

    private static LowFireModule module() {
        if (YogiEssentialsClient.getModuleManager() == null) {
            return null;
        }
        return YogiEssentialsClient.getModuleManager().getModule(LowFireModule.class);
    }

    private static boolean active() {
        LowFireModule module = module();
        return module != null && module.isEnabled() && module.isSmallGroundFire();
    }

    /**
     * Called directly by the Low Fire master toggle and Small Ground Fire
     * setting so the visual swap is scheduled immediately instead of waiting
     * for the next client tick.
     */
    public static void onModuleStateChanged() {
        class_310 client = class_310.method_1551();
        if (client == null || client.field_1687 == null || client.field_1724 == null) {
            return;
        }

        boolean activeNow = active();
        if (activeNow) {
            scanNearbyFire(client);
            refreshFireSections(client, visibleFire);
        } else {
            refreshFireSections(client, visibleFire);
            visibleFire.clear();
        }
        wasActive = activeNow;
        ticksUntilReconcile = RECONCILE_TICKS;
    }

    /** Called at the tail of ClientWorld.handleBlockUpdate. */
    public static void onBlockStateChanged(class_2338 pos, class_2680 state) {
        if (pos == null || state == null || !active()) {
            return;
        }

        class_2338 immutable = pos.method_10062();
        if (isFireState(state)) {
            visibleFire.add(immutable);
        } else {
            visibleFire.remove(immutable);
        }
    }

    private static void tick(class_310 client) {
        YogiFireVisuals.tickAnimation();

        boolean activeNow = active();
        if (client.field_1687 == null || client.field_1724 == null) {
            visibleFire.clear();
            ticksUntilReconcile = 0;
            wasActive = activeNow;
            return;
        }

        if (activeNow != wasActive) {
            onModuleStateChanged();
            activeNow = active();
        }

        if (!activeNow) {
            return;
        }

        if (--ticksUntilReconcile <= 0) {
            ticksUntilReconcile = RECONCILE_TICKS;
            scanNearbyFire(client);
        }
    }

    private static void scanNearbyFire(class_310 client) {
        class_2338 center = client.field_1724.method_24515();
        int minX = center.method_10263() - HORIZONTAL_SCAN_RADIUS;
        int maxX = center.method_10263() + HORIZONTAL_SCAN_RADIUS;
        int minY = Math.max(client.field_1687.method_31607(), center.method_10264() - VERTICAL_SCAN_RADIUS);
        int maxY = Math.min(client.field_1687.method_31600(), center.method_10264() + VERTICAL_SCAN_RADIUS);
        int minZ = center.method_10260() - HORIZONTAL_SCAN_RADIUS;
        int maxZ = center.method_10260() + HORIZONTAL_SCAN_RADIUS;

        Set<class_2338> next = new LinkedHashSet<>();
        class_2338.class_2339 cursor = new class_2338.class_2339();

        for (int y = minY; y <= maxY; y++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int x = minX; x <= maxX; x++) {
                    cursor.method_10103(x, y, z);
                    if (isFireState(client.field_1687.method_8320(cursor))) {
                        next.add(cursor.method_10062());
                    }
                }
            }
        }

        visibleFire.clear();
        visibleFire.addAll(next);
    }

    private static boolean isFireState(class_2680 state) {
        return state != null && (
                state.method_26204() instanceof class_4770
                        || state.method_27852(class_2246.field_10036)
                        || state.method_27852(class_2246.field_22089)
        );
    }

    private static void refreshFireSections(class_310 client, Iterable<class_2338> positions) {
        if (client.field_1769 == null || positions == null) {
            return;
        }

        for (class_2338 pos : positions) {
            client.field_1687.method_18113(pos.method_10263(), pos.method_10264(), pos.method_10260());
            client.field_1769.method_18146(
                    pos.method_10263(), pos.method_10264(), pos.method_10260(),
                    pos.method_10263(), pos.method_10264(), pos.method_10260()
            );
        }
    }

    private static void render(WorldRenderContext context) {
        if (!active() || visibleFire.isEmpty()) {
            return;
        }

        class_310 client = class_310.method_1551();
        if (client.field_1687 == null || client.field_1724 == null) {
            return;
        }

        class_4587 matrices = context.matrices();
        class_4597 consumers = context.consumers();
        if (matrices == null || consumers == null) {
            return;
        }

        class_4184 camera = client.field_1773.method_19418();
        class_243 cameraPos = camera.method_71156();

        List<class_2338> snapshot = new ArrayList<>(visibleFire);
        for (class_2338 pos : snapshot) {
            class_2680 state = client.field_1687.method_8320(pos);
            if (!isFireState(state)) {
                visibleFire.remove(pos);
                continue;
            }

            YogiDirectFireRenderer.renderGroundFire(
                    matrices,
                    consumers,
                    pos.method_10263() - cameraPos.field_1352,
                    pos.method_10264() - cameraPos.field_1351,
                    pos.method_10260() - cameraPos.field_1350,
                    state.method_27852(class_2246.field_22089)
            );
        }
    }
}
