package dev.yogi.yogiessentials.client.module.performance;

import dev.yogi.yogiessentials.client.module.Category;
import dev.yogi.yogiessentials.client.module.Module;
import dev.yogi.yogiessentials.client.setting.BooleanSetting;
import dev.yogi.yogiessentials.client.setting.NumberSetting;
import dev.yogi.yogiessentials.client.util.FrameTelemetry;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.class_11954;
import net.minecraft.class_243;
import net.minecraft.class_310;
import net.minecraft.class_4184;

/**
 * Reduces the dynamic BlockEntityRenderer workload in dense bases/storage rooms.
 * Rather than copying another mod's static chest model path, Yogi Essentials keeps
 * vanilla rendering and simply avoids submitting distant dynamic renderers that are
 * disproportionately expensive compared with ordinary chunk geometry.
 */
public final class DynamicBlockEntityOptimizerModule extends Module {
    private final NumberSetting alwaysRenderRadius =
            new NumberSetting("Always Render Radius", 20, 8, 48, 2);
    private final NumberSetting chestDistance =
            new NumberSetting("Chest / Storage Distance", 64, 24, 192, 8);
    private final NumberSetting signDistance =
            new NumberSetting("Signs Distance", 72, 24, 192, 8);
    private final NumberSetting decorationDistance =
            new NumberSetting("Skulls / Banners Distance", 64, 24, 192, 8);
    private final NumberSetting otherDistance =
            new NumberSetting("Other Block Entities Distance", 96, 32, 256, 8);
    private final BooleanSetting adaptive =
            new BooleanSetting("Adaptive Block Entity Distance", true);
    private final NumberSetting minimumAdaptivePercent =
            new NumberSetting("Minimum Adaptive Distance (%)", 70, 50, 100, 5);

    private volatile double adaptiveScale = 1.0;
    private long lastEvaluation;
    private final Map<Class<?>, Kind> kindCache = new HashMap<>();

    private enum Kind {
        STORAGE,
        SIGN,
        DECORATION,
        OTHER
    }

    public DynamicBlockEntityOptimizerModule() {
        super(
                "Dynamic Block Entity Optimizer",
                "Cuts expensive distant chest, sign, skull, banner and other block-entity renderer submissions while preserving nearby detail.",
                Category.PERFORMANCE
        );
        addSetting(alwaysRenderRadius);
        addSetting(chestDistance);
        addSetting(signDistance);
        addSetting(decorationDistance);
        addSetting(otherDistance);
        addSetting(adaptive);
        addSetting(minimumAdaptivePercent);
    }

    @Override
    protected void onDisable() {
        adaptiveScale = 1.0;
        lastEvaluation = 0L;
        kindCache.clear();
    }

    public void tick(class_310 client) {
        if (!isEnabled() || !adaptive.get() || client == null || client.field_1687 == null) {
            adaptiveScale = 1.0;
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastEvaluation < 750L) return;
        lastEvaluation = now;

        FrameTelemetry.Sample sample = FrameTelemetry.snapshot();
        if (!sample.available()) return;
        double floor = minimumAdaptivePercent.get() / 100.0;
        double lowRatio = sample.onePercentLow() / Math.max(1.0, sample.averageFps());
        double target = 1.0;
        if (sample.stutters() >= 3 || lowRatio < 0.60) target = floor;
        else if (sample.stutters() >= 1 || lowRatio < 0.75) target = Math.max(floor, 0.85);

        adaptiveScale += Math.max(-0.08, Math.min(0.04, target - adaptiveScale));
        adaptiveScale = Math.max(floor, Math.min(1.0, adaptiveScale));
    }

    public boolean allow(class_11954 state) {
        if (!isEnabled() || state == null || state.field_62673 == null) return true;

        class_310 client = class_310.method_1551();
        if (client == null || client.field_1773 == null) return true;
        class_4184 camera = client.field_1773.method_19418();
        if (camera == null || !camera.method_19332()) return true;

        class_243 cameraPos = camera.method_71156();
        double dx = cameraPos.field_1352 - (state.field_62673.method_10263() + 0.5);
        double dy = cameraPos.field_1351 - (state.field_62673.method_10264() + 0.5);
        double dz = cameraPos.field_1350 - (state.field_62673.method_10260() + 0.5);
        double distanceSq = dx * dx + dy * dy + dz * dz;
        double safe = alwaysRenderRadius.get();
        if (distanceSq <= safe * safe) return true;

        double limit = distanceFor(state);
        if (adaptive.get()) limit *= adaptiveScale;
        return distanceSq <= limit * limit;
    }

    private double distanceFor(class_11954 state) {
        Kind kind = kindCache.computeIfAbsent(state.getClass(), DynamicBlockEntityOptimizerModule::classify);
        return switch (kind) {
            case STORAGE -> chestDistance.get();
            case SIGN -> signDistance.get();
            case DECORATION -> decorationDistance.get();
            case OTHER -> otherDistance.get();
        };
    }

    private static Kind classify(Class<?> type) {
        String name = type.getSimpleName();
        if (name.contains("Chest") || name.contains("Shulker") || name.contains("Container")) {
            return Kind.STORAGE;
        }
        if (name.contains("Sign")) {
            return Kind.SIGN;
        }
        if (name.contains("Skull") || name.contains("Banner") || name.contains("Bell")) {
            return Kind.DECORATION;
        }
        return Kind.OTHER;
    }
}
