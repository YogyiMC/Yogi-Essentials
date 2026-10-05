package dev.yogi.yogiessentials.client.util;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.class_1297;
import net.minecraft.class_1531;
import net.minecraft.class_1657;
import net.minecraft.class_310;
import net.minecraft.class_8113;

/**
 * Lightweight tracker for server-owned player nametag carrier entities.
 *
 * Instead of rescanning every entity in the world every tick, carriers are
 * observed lazily as Minecraft reaches them in the normal entity-render path.
 * A short expiry keeps the association alive across render ordering differences
 * while avoiding a permanent cache.
 */
public final class ServerNametagDetector {
    private static final long EXPIRY_TICKS = 40L;
    private static Object worldRef;
    private static long lastCleanupTick = Long.MIN_VALUE;
    private static final Map<Integer, Long> playerExpiry = new HashMap<>();
    private static final Map<Integer, Long> carrierExpiry = new HashMap<>();

    private ServerNametagDetector() {
    }

    public static boolean observeCarrier(class_1297 entity, class_310 client) {
        if (entity == null || client == null || client.field_1687 == null || !isPotentialCarrier(entity)) {
            return false;
        }
        prepare(client);

        class_1657 nearest = null;
        double nearestSq = Double.MAX_VALUE;
        for (class_1657 player : client.field_1687.method_18456()) {
            double dx = entity.method_23317() - player.method_23317();
            double dz = entity.method_23321() - player.method_23321();
            double horizontalSq = dx * dx + dz * dz;
            if (horizontalSq > 0.85D * 0.85D) continue;

            double dy = entity.method_23318() - player.method_23318();
            if (dy < -0.35D || dy > 4.25D) continue;

            double distanceSq = horizontalSq + dy * dy;
            if (distanceSq < nearestSq) {
                nearestSq = distanceSq;
                nearest = player;
            }
        }

        if (nearest == null) return false;
        long expiry = client.field_1687.method_75260() + EXPIRY_TICKS;
        playerExpiry.put(nearest.method_5628(), expiry);
        carrierExpiry.put(entity.method_5628(), expiry);
        return true;
    }

    public static boolean playerHasServerNametag(class_1657 player) {
        if (player == null) return false;
        class_310 client = class_310.method_1551();
        if (client == null || client.field_1687 == null) return false;
        prepare(client);
        Long expiry = playerExpiry.get(player.method_5628());
        return expiry != null && expiry >= client.field_1687.method_75260();
    }

    public static boolean isServerNametagCarrier(class_1297 entity) {
        if (entity == null) return false;
        class_310 client = class_310.method_1551();
        if (client == null || client.field_1687 == null) return false;
        prepare(client);
        Long expiry = carrierExpiry.get(entity.method_5628());
        return expiry != null && expiry >= client.field_1687.method_75260();
    }

    private static void prepare(class_310 client) {
        if (client.field_1687 != worldRef) {
            worldRef = client.field_1687;
            playerExpiry.clear();
            carrierExpiry.clear();
            lastCleanupTick = Long.MIN_VALUE;
        }

        long tick = client.field_1687.method_75260();
        if (lastCleanupTick == Long.MIN_VALUE || tick - lastCleanupTick >= 20L) {
            lastCleanupTick = tick;
            playerExpiry.entrySet().removeIf(entry -> entry.getValue() < tick);
            carrierExpiry.entrySet().removeIf(entry -> entry.getValue() < tick);
        }
    }

    private static boolean isPotentialCarrier(class_1297 entity) {
        return entity instanceof class_8113.class_8123
                || (entity instanceof class_1531 armorStand
                && armorStand.method_5767()
                && armorStand.method_16914());
    }
}
