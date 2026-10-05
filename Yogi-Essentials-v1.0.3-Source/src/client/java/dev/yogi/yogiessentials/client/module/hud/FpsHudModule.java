package dev.yogi.yogiessentials.client.module.hud;

public class FpsHudModule extends StyledHudModule {

    public FpsHudModule() {
        super(
                "FPS",
                "Shows your current frames per second.",
                0.01,
                0.02
        );
    }
}
