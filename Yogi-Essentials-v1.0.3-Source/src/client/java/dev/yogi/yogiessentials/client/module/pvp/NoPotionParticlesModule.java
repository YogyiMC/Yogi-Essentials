package dev.yogi.yogiessentials.client.module.pvp;

import dev.yogi.yogiessentials.client.module.Category;
import dev.yogi.yogiessentials.client.module.Module;
import dev.yogi.yogiessentials.client.setting.BooleanSetting;

public final class NoPotionParticlesModule extends Module {

    private final BooleanSetting hideOwnEffects =
            new BooleanSetting("Hide Own Effect Particles", true);

    private final BooleanSetting hideOtherEntityEffects =
            new BooleanSetting("Hide Other Entity Effects", false);

    private final BooleanSetting hideSplashEffects =
            new BooleanSetting("Hide Splash Potion Effects", false);

    public NoPotionParticlesModule() {
        super(
                "No Potion Particles",
                "Hides potion-effect swirl particles without removing the effects. Own, other-entity, and splash-potion particles are separately configurable.",
                Category.PVP
        );

        addSetting(hideOwnEffects);
        addSetting(hideOtherEntityEffects);
        addSetting(hideSplashEffects);
    }

    public BooleanSetting getHideOwnEffects() {
        return hideOwnEffects;
    }

    public BooleanSetting getHideOtherEntityEffects() {
        return hideOtherEntityEffects;
    }

    public BooleanSetting getHideSplashEffects() {
        return hideSplashEffects;
    }
}
