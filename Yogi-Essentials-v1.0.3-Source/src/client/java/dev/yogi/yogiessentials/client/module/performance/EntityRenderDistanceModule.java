package dev.yogi.yogiessentials.client.module.performance;

import dev.yogi.yogiessentials.client.module.Category;
import dev.yogi.yogiessentials.client.module.Module;
import dev.yogi.yogiessentials.client.setting.BooleanSetting;
import dev.yogi.yogiessentials.client.setting.NumberSetting;
import dev.yogi.yogiessentials.client.util.FrameTelemetry;
import net.minecraft.class_1297;
import net.minecraft.class_1303;
import net.minecraft.class_1308;
import net.minecraft.class_1311;
import net.minecraft.class_1542;
import net.minecraft.class_1657;
import net.minecraft.class_1676;
import net.minecraft.class_238;
import net.minecraft.class_243;
import net.minecraft.class_310;
import net.minecraft.class_4184;
import net.minecraft.class_8836;

/**
 * Per-category client render-distance control inspired by the general idea of
 * entity-specific view distances. This is a Yogi Essentials implementation and
 * only decides whether an already-tracked entity should be submitted to the renderer.
 */
public final class EntityRenderDistanceModule extends Module {
    private final NumberSetting alwaysRenderRadius =
            new NumberSetting("Always Render Radius", 24, 8, 64, 2);
    private final NumberSetting players =
            new NumberSetting("Players Distance", 192, 48, 320, 8);
    private final NumberSetting hostile =
            new NumberSetting("Hostile Mobs Distance", 128, 32, 256, 8);
    private final NumberSetting passive =
            new NumberSetting("Passive Mobs Distance", 96, 32, 256, 8);
    private final NumberSetting otherMobs =
            new NumberSetting("Other Mobs Distance", 96, 32, 256, 8);
    private final NumberSetting itemDrops =
            new NumberSetting("Item Drops Distance", 64, 16, 192, 8);
    private final NumberSetting xpOrbs =
            new NumberSetting("XP Orbs Distance", 48, 16, 160, 8);
    private final NumberSetting projectiles =
            new NumberSetting("Projectiles Distance", 128, 32, 256, 8);
    private final NumberSetting vehicles =
            new NumberSetting("Vehicles Distance", 128, 32, 256, 8);
    private final NumberSetting miscellaneous =
            new NumberSetting("Misc Entities Distance", 96, 24, 256, 8);
    private final BooleanSetting adaptive =
            new BooleanSetting("Adaptive Distance Scaling", true);
    private final NumberSetting minimumAdaptivePercent =
            new NumberSetting("Minimum Adaptive Distance (%)", 75, 50, 100, 5);

    private volatile double adaptiveScale = 1.0;
    private long lastEvaluation;

    public EntityRenderDistanceModule() {
        super(
                "Entity Render Distance",
                "Uses separate client render distances for entity categories and can reduce only distant entity work when frame pacing is under pressure.",
                Category.PERFORMANCE
        );
        addSetting(alwaysRenderRadius);
        addSetting(players);
        addSetting(hostile);
        addSetting(passive);
        addSetting(otherMobs);
        addSetting(itemDrops);
        addSetting(xpOrbs);
        addSetting(projectiles);
        addSetting(vehicles);
        addSetting(miscellaneous);
        addSetting(adaptive);
        addSetting(minimumAdaptivePercent);
    }

    @Override
    protected void onDisable() {
        adaptiveScale = 1.0;
        lastEvaluation = 0L;
    }

    public void tick(class_310 client) {
        if (!isEnabled() || !adaptive.get() || client == null || client.field_1687 == null) {
            adaptiveScale = 1.0;
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastEvaluation < 500L) return;
        lastEvaluation = now;

        FrameTelemetry.Sample sample = FrameTelemetry.snapshot();
        if (!sample.available()) return;

        double floor = minimumAdaptivePercent.get() / 100.0;
        double lowRatio = sample.onePercentLow() / Math.max(1.0, sample.averageFps());
        double target;
        if (sample.stutters() >= 3 || lowRatio < 0.58 || sample.varianceMsSquared() > 24.0) {
            target = floor;
        } else if (sample.stutters() >= 1 || lowRatio < 0.72 || sample.varianceMsSquared() > 12.0) {
            target = Math.max(floor, 0.85);
        } else {
            target = 1.0;
        }

        if (target < adaptiveScale) {
            adaptiveScale = Math.max(target, adaptiveScale - 0.08);
        } else {
            adaptiveScale = Math.min(target, adaptiveScale + 0.04);
        }
    }

    public boolean allow(class_1297 entity) {
        if (!isEnabled() || entity == null) return true;

        class_310 client = class_310.method_1551();
        if (client == null || client.field_1773 == null) return true;
        class_4184 camera = client.field_1773.method_19418();
        if (camera == null || !camera.method_19332()) return true;

        if (client.field_1724 != null && entity == client.field_1724) return true;

        class_238 box = entity.method_5829();
        class_243 cameraPos = camera.method_71156();
        double dx = cameraPos.field_1352 - (box.field_1323 + box.field_1320) * 0.5;
        double dy = cameraPos.field_1351 - (box.field_1322 + box.field_1325) * 0.5;
        double dz = cameraPos.field_1350 - (box.field_1321 + box.field_1324) * 0.5;
        double distanceSq = dx * dx + dy * dy + dz * dz;
        double safe = alwaysRenderRadius.get();
        if (distanceSq <= safe * safe) return true;

        double limit = categoryDistance(entity);
        if (adaptive.get()) limit *= adaptiveScale;
        return distanceSq <= limit * limit;
    }

    private double categoryDistance(class_1297 entity) {
        if (entity instanceof class_1657) return players.get();
        if (entity instanceof class_1542) return itemDrops.get();
        if (entity instanceof class_1303) return xpOrbs.get();
        if (entity instanceof class_1676) return projectiles.get();
        if (entity instanceof class_8836) return vehicles.get();

        if (entity instanceof class_1308) {
            class_1311 group = entity.method_5864().method_5891();
            if (group == class_1311.field_6302) return hostile.get();
            if (group.method_6136()) return passive.get();
            return otherMobs.get();
        }

        return miscellaneous.get();
    }
}
