package dev.yogi.yogiessentials.client.module.pvp;

import dev.yogi.yogiessentials.client.module.Category;
import dev.yogi.yogiessentials.client.module.Module;
import dev.yogi.yogiessentials.client.setting.BooleanSetting;
import dev.yogi.yogiessentials.client.setting.NumberSetting;
import dev.yogi.yogiessentials.client.setting.SectionSetting;
import dev.yogi.yogiessentials.client.util.HitRegistrationManager;

/**
 * Client-side hit feedback inspired by the supplied BetterHitreg source.
 * Server damage, reach, cooldowns and outgoing attack packets remain vanilla.
 */
public final class HitRegistrationModule extends Module {
    private final NumberSetting feedbackDelay =
            new NumberSetting("Feedback Delay (ms)", 0.0, 0.0, 250.0, 5.0);
    private final NumberSetting duplicateWindow =
            new NumberSetting("Duplicate Suppression (ms)", 650.0, 100.0, 1500.0, 25.0);
    private final NumberSetting soundVolume =
            new NumberSetting("Feedback Sound Volume (%)", 100.0, 0.0, 150.0, 5.0);

    private final BooleanSetting hurtAnimation =
            new BooleanSetting("Immediate Hurt Animation", true);
    private final BooleanSetting hitSounds =
            new BooleanSetting("Immediate Hit Sounds", true);
    private final BooleanSetting particles =
            new BooleanSetting("Immediate Hit Particles", true);
    private final BooleanSetting suppressDuplicateAnimation =
            new BooleanSetting("Suppress Delayed Server Animation", true);
    private final BooleanSetting playersOnly =
            new BooleanSetting("Players Only", true);

    public HitRegistrationModule() {
        super(
                "Hit Registration",
                "Makes confirmed-looking client hit feedback appear immediately while leaving server combat unchanged.",
                Category.PVP
        );

        addSetting(new SectionSetting("Client feedback only; server damage and attack packets stay vanilla."));
        addSetting(feedbackDelay);
        addSetting(duplicateWindow);
        addSetting(hurtAnimation);
        addSetting(hitSounds);
        addSetting(soundVolume);
        addSetting(particles);
        addSetting(suppressDuplicateAnimation);
        addSetting(playersOnly);
    }

    public NumberSetting getFeedbackDelay() {
        return feedbackDelay;
    }

    public NumberSetting getDuplicateWindow() {
        return duplicateWindow;
    }

    public NumberSetting getSoundVolume() {
        return soundVolume;
    }

    public BooleanSetting getHurtAnimation() {
        return hurtAnimation;
    }

    public BooleanSetting getHitSounds() {
        return hitSounds;
    }

    public BooleanSetting getParticles() {
        return particles;
    }

    public BooleanSetting getSuppressDuplicateAnimation() {
        return suppressDuplicateAnimation;
    }

    public BooleanSetting getPlayersOnly() {
        return playersOnly;
    }


    @Override
    protected void onDisable() {
        HitRegistrationManager.reset();
    }
}
