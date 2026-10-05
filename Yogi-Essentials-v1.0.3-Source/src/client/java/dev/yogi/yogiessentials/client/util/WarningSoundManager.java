package dev.yogi.yogiessentials.client.util;

import dev.yogi.yogiessentials.client.module.pvp.WarningModule;
import net.minecraft.class_1109;
import net.minecraft.class_1113;
import net.minecraft.class_310;
import net.minecraft.class_3414;
import net.minecraft.class_3417;
import net.minecraft.class_3419;








public final class WarningSoundManager {

    private WarningSoundManager() {
    }

    public static void play(
            WarningModule.WarningSound sound,
            double volume
    ) {
        if (sound == null) {
            return;
        }

        class_310 client =
                class_310.method_1551();

        if (
                client == null
                        || client.method_1483() == null
        ) {
            return;
        }

        float safeVolume =
                (float) Math.max(
                        0.05,
                        Math.min(2.0, volume)
                );

        class_3414 soundEvent;
        float pitch;

        switch (sound) {
            case BELL -> {
                soundEvent =
                        class_3417.field_17265;
                pitch = 1.05F;
            }

            case CHIME -> {
                soundEvent =
                        class_3417.field_43154;
                pitch = 1.20F;
            }

            case LEVEL_UP -> {
                soundEvent =
                        class_3417.field_14709;
                pitch = 1.00F;
            }

            case EXPERIENCE -> {
                soundEvent =
                        class_3417.field_14627;
                pitch = 1.15F;
            }

            case ANVIL -> {
                soundEvent =
                        class_3417.field_14833;
                pitch = 1.10F;
            }

            case CLICK -> {
                soundEvent =
                        class_3417.field_15015.comp_349();
                pitch = 1.00F;
            }

            default -> {
                return;
            }
        }

        class_1109 soundInstance =
                new class_1109(
                        soundEvent.comp_3319(),
                        class_3419.field_15250,
                        safeVolume,
                        pitch,
                        class_1113.method_43221(),
                        false,
                        0,
                        class_1113.class_1114.field_5478,
                        0.0,
                        0.0,
                        0.0,
                        true
                );

        client
                .method_1483()
                .method_4873(soundInstance);
    }
}
