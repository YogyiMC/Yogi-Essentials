package dev.yogi.yogiessentials.client.module.fixes;

import dev.yogi.yogiessentials.client.module.Category;
import dev.yogi.yogiessentials.client.module.Module;
import dev.yogi.yogiessentials.client.setting.BooleanSetting;
import dev.yogi.yogiessentials.client.setting.NumberSetting;











public final class FpsRecoveryFixModule extends Module {

    private final BooleanSetting autoRecovery =
            new BooleanSetting("Auto Recovery", true);

    private final NumberSetting minimumHealthyFps =
            new NumberSetting("Minimum Healthy FPS", 160.0, 60.0, 1000.0, 10.0);

    private final NumberSetting dropThresholdPercent =
            new NumberSetting("Drop Threshold %", 55.0, 25.0, 90.0, 5.0);

    private final NumberSetting detectionSeconds =
            new NumberSetting("Detection Time", 2.5, 0.5, 10.0, 0.5);

    private final NumberSetting cooldownSeconds =
            new NumberSetting("Recovery Cooldown", 12.0, 5.0, 60.0, 1.0);

    private final BooleanSetting refreshSwapInterval =
            new BooleanSetting("Refresh VSync State", false);

    public FpsRecoveryFixModule() {
        super(
                "FPS Recovery Fix",
                "Learns your healthy FPS and resets safe pacing/presentation state after sustained abnormal drops without cycling exclusive fullscreen.",
                Category.FIXES
        );

        addSetting(autoRecovery);
        addSetting(minimumHealthyFps);
        addSetting(dropThresholdPercent);
        addSetting(detectionSeconds);
        addSetting(cooldownSeconds);
        addSetting(refreshSwapInterval);
    }

    @Override
    protected void onDisable() {
        dev.yogi.yogiessentials.client.util.FpsRecoveryManager.reset();
    }

    public BooleanSetting getAutoRecovery() { return autoRecovery; }
    public NumberSetting getMinimumHealthyFps() { return minimumHealthyFps; }
    public NumberSetting getDropThresholdPercent() { return dropThresholdPercent; }
    public NumberSetting getDetectionSeconds() { return detectionSeconds; }
    public NumberSetting getCooldownSeconds() { return cooldownSeconds; }
    public BooleanSetting getRefreshSwapInterval() { return refreshSwapInterval; }
}
