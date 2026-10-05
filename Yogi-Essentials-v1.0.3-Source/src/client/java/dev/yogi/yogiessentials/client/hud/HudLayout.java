package dev.yogi.yogiessentials.client.hud;

import dev.yogi.yogiessentials.client.module.hud.HudModule;

public final class HudLayout {

    private HudLayout() {
    }

    public static int pixelX(
            HudModule module,
            int screenWidth,
            int renderedWidth
    ) {
        int available =
                Math.max(
                        0,
                        screenWidth - renderedWidth
                );

        return (int) Math.round(
                module.getHudX() * available
        );
    }

    public static int pixelY(
            HudModule module,
            int screenHeight,
            int renderedHeight
    ) {
        int available =
                Math.max(
                        0,
                        screenHeight - renderedHeight
                );

        return (int) Math.round(
                module.getHudY() * available
        );
    }

    public static double normalizedX(
            int pixelX,
            int screenWidth,
            int renderedWidth
    ) {
        int available =
                Math.max(
                        0,
                        screenWidth - renderedWidth
                );

        if (available == 0) {
            return 0.0;
        }

        return pixelX / (double) available;
    }

    public static double normalizedY(
            int pixelY,
            int screenHeight,
            int renderedHeight
    ) {
        int available =
                Math.max(
                        0,
                        screenHeight - renderedHeight
                );

        if (available == 0) {
            return 0.0;
        }

        return pixelY / (double) available;
    }
}
