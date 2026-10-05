package dev.yogi.yogiessentials.client.util;

import dev.yogi.yogiessentials.client.YogiEssentialsClient;
import dev.yogi.yogiessentials.client.module.pvp.CombatHitboxesModule;
import dev.yogi.yogiessentials.client.module.pvp.ReachDisplayModule;
import net.minecraft.class_1296;
import net.minecraft.class_1297;
import net.minecraft.class_1303;
import net.minecraft.class_1308;
import net.minecraft.class_1542;
import net.minecraft.class_1588;
import net.minecraft.class_1657;
import net.minecraft.class_1676;
import net.minecraft.class_310;
import net.minecraft.class_3966;
import net.minecraft.class_8836;

/** Shared resolver for Minecraft's native F3+B hitbox/debug gizmos. */
public final class CombatHitboxColorManager {
    private static final ThreadLocal<Integer> NATIVE_HITBOX_DEPTH = ThreadLocal.withInitial(() -> 0);
    private static final ThreadLocal<Integer> NATIVE_BOX_INDEX = ThreadLocal.withInitial(() -> 0);

    private CombatHitboxColorManager() {}

    public static void beginNativeHitboxDraw() {
        int depth = NATIVE_HITBOX_DEPTH.get();
        if (depth == 0) NATIVE_BOX_INDEX.set(0);
        NATIVE_HITBOX_DEPTH.set(depth + 1);
    }

    public static void endNativeHitboxDraw() {
        int next = Math.max(0, NATIVE_HITBOX_DEPTH.get() - 1);
        if (next == 0) {
            NATIVE_HITBOX_DEPTH.remove();
            NATIVE_BOX_INDEX.remove();
        } else {
            NATIVE_HITBOX_DEPTH.set(next);
        }
    }

    public static boolean isInsideNativeHitboxDraw() {
        return NATIVE_HITBOX_DEPTH.get() > 0;
    }

    /** Returns 0 for the native entity box, then 1+ for helper boxes in the same draw pass. */
    public static int nextNativeBoxIndex() {
        int index = NATIVE_BOX_INDEX.get();
        NATIVE_BOX_INDEX.set(index + 1);
        return index;
    }

    public static boolean shouldRenderHitbox(class_1297 entity) {
        if (entity == null || YogiEssentialsClient.getModuleManager() == null) return true;
        CombatHitboxesModule module = YogiEssentialsClient.getModuleManager().getModule(CombatHitboxesModule.class);
        if (module == null || !module.isEnabled()) return true;

        if (entity instanceof class_1657) return module.showPlayers();
        if (entity instanceof class_1542) return module.showItemDrops();
        if (entity instanceof class_1303) return module.showExperienceOrbs();
        if (entity instanceof class_1676) return module.showProjectiles();
        if (entity instanceof class_8836) return module.showVehicles();
        if (entity instanceof class_1588) return module.showHostileMobs();
        if (entity instanceof class_1296) return module.showPassiveMobs();
        if (entity instanceof class_1308) return module.showOtherMobs();
        return module.showOtherEntities();
    }

    public static int colorFor(class_1297 entity, int fallbackArgb) {
        if (entity == null || YogiEssentialsClient.getModuleManager() == null) return fallbackArgb;

        ReachDisplayModule reach = YogiEssentialsClient.getModuleManager().getModule(ReachDisplayModule.class);
        if (reach != null && reach.isEnabled() && reach.usesHitbox() && isCurrentTarget(entity, reach)) {
            return reach.getHighlightColor().getArgb();
        }

        CombatHitboxesModule hitboxes = YogiEssentialsClient.getModuleManager().getModule(CombatHitboxesModule.class);
        if (hitboxes == null || !hitboxes.isEnabled()) return fallbackArgb;
        return entity instanceof class_1657 ? hitboxes.playerColor() : hitboxes.otherEntityColor();
    }

    public static Integer activeReachCrosshairColor() {
        if (YogiEssentialsClient.getModuleManager() == null) return null;
        ReachDisplayModule reach = YogiEssentialsClient.getModuleManager().getModule(ReachDisplayModule.class);
        if (reach == null || !reach.isEnabled() || !reach.usesCrosshairColor()) return null;
        class_310 client = class_310.method_1551();
        if (client == null || !(client.field_1765 instanceof class_3966 hit)) return null;
        class_1297 target = hit.method_17782();
        if (target == client.field_1724 || !reach.accepts(target)) return null;
        return reach.getHighlightColor().getArgb();
    }

    public static boolean showHelper(HelperPart part) {
        if (YogiEssentialsClient.getModuleManager() == null) return true;
        CombatHitboxesModule hitboxes = YogiEssentialsClient.getModuleManager().getModule(CombatHitboxesModule.class);
        if (hitboxes == null || !hitboxes.isEnabled()) return true;
        return switch (part) {
            case EYE_HEIGHT -> hitboxes.showEyeHeight();
            case LOOK_DIRECTION -> hitboxes.showLookDirection();
            case SERVER_POSITION -> hitboxes.showServerPosition();
            case EXTRA_DIRECTION_LINES -> hitboxes.showExtraDirectionLines();
            case DEBUG_CIRCLES -> hitboxes.showDebugCircles();
            case DEBUG_POINTS -> hitboxes.showDebugPoints();
        };
    }

    public static boolean onlyPrimaryHitboxEnabled() {
        if (YogiEssentialsClient.getModuleManager() == null) return false;
        CombatHitboxesModule hitboxes = YogiEssentialsClient.getModuleManager().getModule(CombatHitboxesModule.class);
        if (hitboxes == null || !hitboxes.isEnabled()) return false;
        return !hitboxes.showEyeHeight()
                && !hitboxes.showLookDirection()
                && !hitboxes.showServerPosition()
                && !hitboxes.showExtraDirectionLines()
                && !hitboxes.showDebugCircles()
                && !hitboxes.showDebugPoints();
    }

    public enum HelperPart { EYE_HEIGHT, LOOK_DIRECTION, SERVER_POSITION, EXTRA_DIRECTION_LINES, DEBUG_CIRCLES, DEBUG_POINTS }

    private static boolean isCurrentTarget(class_1297 entity, ReachDisplayModule reach) {
        class_310 client = class_310.method_1551();
        if (client == null || !(client.field_1765 instanceof class_3966 hit)) return false;
        class_1297 target = hit.method_17782();
        return target == entity && target != client.field_1724 && reach.accepts(target);
    }
}
