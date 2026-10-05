package dev.yogi.yogiessentials.client.module.pvp;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.class_1297;
import net.minecraft.class_1511;
import net.minecraft.class_2338;
import net.minecraft.class_638;

public final class CombatSoundContext {
    public enum Source { CRYSTAL, ANCHOR, OTHER }

    private record Position(double x, double y, double z, long expires) {
        boolean near(double sx, double sy, double sz, double radius) {
            double dx = sx - x;
            double dy = sy - y;
            double dz = sz - z;
            return dx * dx + dy * dy + dz * dz <= radius * radius;
        }
    }

    private static class_638 world;
    private static final Map<Integer, Position> crystals = new HashMap<>();
    private static Position recentCrystal;
    private static Position recentAnchor;

    private CombatSoundContext() {
    }

    public static synchronized void onEntityAdded(class_638 current, class_1297 entity) {
        useWorld(current);
        if (entity instanceof class_1511) {
            crystals.put(entity.method_5628(), new Position(entity.method_23317(), entity.method_23318(), entity.method_23321(), 0));
        }
    }

    public static synchronized void onEntityRemoved(class_638 current, class_1297 entity) {
        useWorld(current);
        if (entity instanceof class_1511) {
            crystals.remove(entity.method_5628());
            recentCrystal = new Position(entity.method_23317(), entity.method_23318(), entity.method_23321(), System.nanoTime() + 800_000_000L);
        }
    }

    public static synchronized void onCrystalAttack(class_638 current, class_1297 entity) {
        useWorld(current);
        if (entity instanceof class_1511) {
            recentCrystal = new Position(entity.method_23317(), entity.method_23318(), entity.method_23321(), System.nanoTime() + 800_000_000L);
        }
    }

    public static synchronized void onAnchorActivated(class_638 current, class_2338 pos) {
        useWorld(current);
        recentAnchor = new Position(pos.method_10263() + 0.5, pos.method_10264() + 0.5, pos.method_10260() + 0.5,
                System.nanoTime() + 800_000_000L);
    }

    public static synchronized Source explosionAt(class_638 current, double x, double y, double z) {
        useWorld(current);
        long now = System.nanoTime();
        if (recentAnchor != null && now < recentAnchor.expires && recentAnchor.near(x, y, z, 2.0)) return Source.ANCHOR;
        if (recentCrystal != null && now < recentCrystal.expires && recentCrystal.near(x, y, z, 2.0)) return Source.CRYSTAL;
        for (Position crystal : crystals.values()) {
            if (crystal.near(x, y, z, 1.5)) return Source.CRYSTAL;
        }
        return Source.OTHER;
    }

    private static void useWorld(class_638 current) {
        if (world == current) return;
        world = current;
        crystals.clear();
        recentCrystal = null;
        recentAnchor = null;
    }
}
