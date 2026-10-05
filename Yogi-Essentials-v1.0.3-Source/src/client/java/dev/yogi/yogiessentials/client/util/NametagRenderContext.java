package dev.yogi.yogiessentials.client.util;

import net.minecraft.class_2561;

/** Marks the exact player-name label command currently emitted by PlayerEntityRenderer. */
public final class NametagRenderContext {
    private record Active(class_2561 expected, class_2561 replacement) {}

    private static final ThreadLocal<Active> PLAYER_NAMETAG = new ThreadLocal<>();

    private NametagRenderContext() {}

    public static void beginPlayerNametag(class_2561 expectedLabel) {
        beginPlayerNametag(expectedLabel, expectedLabel);
    }

    public static void beginPlayerNametag(class_2561 expectedLabel, class_2561 replacementLabel) {
        if (expectedLabel == null) {
            PLAYER_NAMETAG.remove();
            return;
        }
        PLAYER_NAMETAG.set(new Active(expectedLabel, replacementLabel == null ? expectedLabel : replacementLabel));
    }

    public static void endPlayerNametag() {
        PLAYER_NAMETAG.remove();
    }

    public static boolean isCurrentPlayerNametag(class_2561 label) {
        Active active = PLAYER_NAMETAG.get();
        if (active == null || label == null) return false;
        class_2561 expected = active.expected();
        return label == expected || label.equals(expected) || expected.getString().equals(label.getString());
    }

    public static class_2561 replacementFor(class_2561 label) {
        Active active = PLAYER_NAMETAG.get();
        if (active == null || !isCurrentPlayerNametag(label)) return label;
        return active.replacement();
    }
}
