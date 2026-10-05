package dev.yogi.yogiessentials.client.module.pvp;

import dev.yogi.yogiessentials.client.module.Category;
import dev.yogi.yogiessentials.client.module.Module;
import dev.yogi.yogiessentials.client.setting.BooleanSetting;
import dev.yogi.yogiessentials.client.setting.KeybindSetting;
import dev.yogi.yogiessentials.client.setting.Setting;
import dev.yogi.yogiessentials.client.util.ToggleSprintManager;

import org.lwjgl.glfw.GLFW;








public final class ToggleSprintModule extends Module {

    private final KeybindSetting toggleKey =
            new KeybindSetting(
                    "Toggle Sprint Keybind",
                    GLFW.GLFW_KEY_R
            );

    private final BooleanSetting alwaysSprint =
            new BooleanSetting(
                    "Always Sprint",
                    false
            );

    private final BooleanSetting keepSprintingAfterRestart =
            new BooleanSetting(
                    "Keep Sprinting After Game Restart",
                    false
            );

    



    private final BooleanSetting sprintToggled =
            new BooleanSetting(
                    "Sprint Toggled",
                    false
            );

    public ToggleSprintModule() {
        super(
                "Toggle Sprint",
                "Press the selected key once to start sprinting and press it again to stop. Always Sprint only chooses the initial on-state; the keybind remains authoritative.",
                Category.PVP
        );

        addSetting(toggleKey);
        addSetting(alwaysSprint);
        addSetting(keepSprintingAfterRestart);
        addSetting(sprintToggled);
    }

    @Override
    public boolean isSettingVisible(Setting<?> setting) {
        return setting != sprintToggled;
    }

    @Override
    protected void onDisable() {
        ToggleSprintManager.releaseForcedSprint();
    }

    @Override
    protected void onSettingsReset() {
        sprintToggled.set(false);
        ToggleSprintManager.releaseForcedSprint();
    }

    public KeybindSetting getToggleKey() {
        return toggleKey;
    }

    public BooleanSetting getAlwaysSprint() {
        return alwaysSprint;
    }

    public BooleanSetting getKeepSprintingAfterRestart() {
        return keepSprintingAfterRestart;
    }

    public boolean isSprintToggled() {
        return sprintToggled.get();
    }

    public boolean shouldForceSprint() {
        return isEnabled() && sprintToggled.get();
    }

    




    public boolean toggleSprintState() {
        sprintToggled.toggle();
        return true;
    }

    




    public void prepareForSession() {
        if (alwaysSprint.get()) {
            sprintToggled.set(true);
        } else if (!keepSprintingAfterRestart.get()) {
            sprintToggled.set(false);
        }
    }

    @Override
    protected void onEnable() {
        if (alwaysSprint.get()) {
            sprintToggled.set(true);
        }
    }
}
