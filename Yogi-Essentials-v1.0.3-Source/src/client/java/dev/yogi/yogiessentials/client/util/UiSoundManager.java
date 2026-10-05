package dev.yogi.yogiessentials.client.util;

import net.minecraft.class_1109;
import net.minecraft.class_310;
import net.minecraft.class_3417;

public final class UiSoundManager {

    private UiSoundManager() {
    }

    public static void menuOpen() {

        play(
                1.15F
        );
    }

    public static void menuClose() {

        play(
                0.90F
        );
    }

    public static void moduleEnabled() {

        play(
                1.35F
        );
    }

    public static void moduleDisabled() {

        play(
                0.78F
        );
    }

    public static void category() {

        play(
                1.08F
        );
    }

    public static void slider() {

        play(
                1.42F
        );
    }

    public static void click() {

        play(
                1.00F
        );
    }

    private static void play(
            float pitch
    ) {

        class_310 client =
                class_310.method_1551();

        if (client == null) {
            return;
        }

        client
                .method_1483()
                .method_4873(
                        class_1109.method_47978(
                                class_3417.field_15015,
                                pitch
                        )
                );
    }
}