package dev.yogi.yogiessentials.client.module.performance;

import dev.yogi.yogiessentials.client.module.Category;
import dev.yogi.yogiessentials.client.module.Module;
import dev.yogi.yogiessentials.client.setting.BooleanSetting;
import dev.yogi.yogiessentials.client.setting.NumberSetting;
import dev.yogi.yogiessentials.client.util.FrameTelemetry;
import net.minecraft.class_1297;
import net.minecraft.class_2338;
import net.minecraft.class_238;
import net.minecraft.class_243;
import net.minecraft.class_310;
import net.minecraft.class_4184;

/**
 * Caps only the expensive, distant portion of entity/block-entity render
 * submission when a scene becomes unusually dense. Nearby objects are never
 * budget-cull candidates. Counters reset per rendered frame, not per game tick.
 */
public final class RenderSubmissionBudgetModule extends Module {
    private final BooleanSetting entities = new BooleanSetting("Budget Distant Entities", true);
    private final BooleanSetting blockEntities = new BooleanSetting("Budget Distant Block Entities", true);
    private final NumberSetting nearbyRadius = new NumberSetting("Always Render Radius", 24, 8, 64, 2);
    private final NumberSetting entityBudget = new NumberSetting("Distant Entity Budget / Frame", 650, 100, 3000, 50);
    private final NumberSetting blockEntityBudget = new NumberSetting("Distant Block Entity Budget / Frame", 280, 50, 1500, 25);

    private long frameId = Long.MIN_VALUE;
    private int submittedEntities;
    private int submittedBlockEntities;

    public RenderSubmissionBudgetModule() {
        super(
                "Render Submission Budget",
                "Caps only very dense distant entity/block-entity render submissions while always keeping nearby objects visible.",
                Category.PERFORMANCE
        );
        addSetting(entities);
        addSetting(blockEntities);
        addSetting(nearbyRadius);
        addSetting(entityBudget);
        addSetting(blockEntityBudget);
    }

    public boolean allowEntity(class_1297 entity) {
        if (!isEnabled() || !entities.get() || entity == null) return true;
        class_310 client = class_310.method_1551();
        class_4184 camera = client == null || client.field_1773 == null ? null : client.field_1773.method_19418();
        if (camera == null || !camera.method_19332()) return true;
        class_238 box = entity.method_5829();
        class_243 cameraPos = camera.method_71156();
        double dx = cameraPos.field_1352 - (box.field_1323 + box.field_1320) * 0.5;
        double dy = cameraPos.field_1351 - (box.field_1322 + box.field_1325) * 0.5;
        double dz = cameraPos.field_1350 - (box.field_1321 + box.field_1324) * 0.5;
        if (dx * dx + dy * dy + dz * dz <= square(nearbyRadius.get())) return true;
        resetIfNewFrame();
        int effectiveBudget = Math.max(80, (int) Math.round(entityBudget.get() * MicrostutterGuardModule.activeScale()));
        return submittedEntities++ < effectiveBudget;
    }

    public boolean allowBlockEntity(class_2338 pos) {
        if (!isEnabled() || !blockEntities.get() || pos == null) return true;
        class_310 client = class_310.method_1551();
        class_4184 camera = client == null || client.field_1773 == null ? null : client.field_1773.method_19418();
        if (camera == null || !camera.method_19332()) return true;
        class_243 cameraPos = camera.method_71156();
        double dx = cameraPos.field_1352 - (pos.method_10263() + 0.5);
        double dy = cameraPos.field_1351 - (pos.method_10264() + 0.5);
        double dz = cameraPos.field_1350 - (pos.method_10260() + 0.5);
        if (dx * dx + dy * dy + dz * dz <= square(nearbyRadius.get())) return true;
        resetIfNewFrame();
        int effectiveBudget = Math.max(40, (int) Math.round(blockEntityBudget.get() * MicrostutterGuardModule.activeScale()));
        return submittedBlockEntities++ < effectiveBudget;
    }

    private void resetIfNewFrame() {
        long current = FrameTelemetry.frameId();
        if (current != frameId) {
            frameId = current;
            submittedEntities = 0;
            submittedBlockEntities = 0;
        }
    }

    private static double square(double value) {
        return value * value;
    }
}
