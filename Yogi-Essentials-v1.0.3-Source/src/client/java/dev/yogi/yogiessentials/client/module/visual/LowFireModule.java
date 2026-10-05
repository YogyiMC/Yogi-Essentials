package dev.yogi.yogiessentials.client.module.visual;

import dev.yogi.yogiessentials.client.module.Category;
import dev.yogi.yogiessentials.client.module.Module;
import dev.yogi.yogiessentials.client.setting.BooleanSetting;
import dev.yogi.yogiessentials.client.render.YogiGroundFireRenderer;

/**
 * Fixed low-fire visual module.
 *
 * <p>There is intentionally no height slider. The module has two independent
 * effects: the fixed first-person low-fire overlay and optional small fire
 * blocks in the world. The module master toggle gates both effects.</p>
 */
public class LowFireModule extends Module {


    private final BooleanSetting firstPersonLowFire =
            new BooleanSetting("Low First-Person Fire", true);

    private final BooleanSetting smallGroundFire =
            new GroundFireSetting();

    public LowFireModule() {
        super(
                "Low Fire",
                "Fixed PvP-friendly first-person fire with an optional separate small ground-fire effect.",
                Category.VISUAL
        );

        addSetting(firstPersonLowFire);
        addSetting(smallGroundFire);
    }

    /**
     * True only when the first-person low-fire effect itself is enabled.
     * Turning this off restores vanilla first-person fire while still allowing
     * Small Ground Fire to remain enabled.
     */
    public boolean isFirstPersonLowFire() {
        return firstPersonLowFire.get();
    }

    public boolean isSmallGroundFire() {
        return smallGroundFire.get();
    }

    @Override
    protected void onEnable() {
        YogiGroundFireRenderer.onModuleStateChanged();
    }

    @Override
    protected void onDisable() {
        YogiGroundFireRenderer.onModuleStateChanged();
    }

    @Override
    protected void onSettingsReset() {
        YogiGroundFireRenderer.onModuleStateChanged();
    }

    private final class GroundFireSetting extends BooleanSetting {
        private GroundFireSetting() {
            super("Small Ground Fire", false);
        }

        @Override
        public void set(Boolean value) {
            boolean old = get();
            super.set(value);
            if (old != get()) {
                YogiGroundFireRenderer.onModuleStateChanged();
            }
        }
    }

}

