package dev.yogi.yogiessentials.client.module.hud;

public class CoordinatesHudModule extends StyledHudModule {

    public CoordinatesHudModule() {
        super(
                "Coordinates",
                "Shows your current XYZ coordinates. Streamer Privacy replaces the live values with HIDDEN.",
                0.01,
                0.10
        );
    }
}
