package dev.yogi.yogiessentials.client.render;

import net.minecraft.class_4587;

/** Shared fixed transform used by both gameplay and the native preview. */
public final class LowFireTransforms {

    /**
     * Single fixed Bactro-style lowering amount. One additional texture-pixel of lowering is applied so the visible flame reaches roughly 2 pixels into the screen instead of 3.
     *
     * <p>The fixed offset is intentionally aggressive so only the low flame tips remain
     * visible, matching the supplied PvP reference more closely.</p>
     *
     * <p>This is intentionally conservative enough to keep the native flame
     * tips visible at common aspect ratios while still clearing most of the
     * center of the screen.</p>
     *
     * <p>Do not turn this back into a user slider. The entire point of the
     * v1.0.3 Low Fire rework is one deterministic low position so gameplay and
     * preview cannot drift apart through independent height math.</p>
     */
    public static final float FIRST_PERSON_Y_OFFSET = -0.03125F;

    /**
     * Fixed downward translation for Small Ground Fire.
     *
     * <p>The value is intentionally unchanged from the known working v1.0.3
     * geometry. The resource-pack compatibility fix now applies this same
     * offset to Yogi-owned crossed fire planes instead of depending on a
     * resource-pack baked fire model.</p>
     */
    public static final float GROUND_FIRE_Y_OFFSET = -0.6875F;

    private LowFireTransforms() {
    }

    public static void applyFirstPerson(class_4587 matrices) {
        matrices.method_46416(0.0F, FIRST_PERSON_Y_OFFSET, 0.0F);
    }
}
