package dev.yogi.yogiessentials.client.render;

import dev.yogi.yogiessentials.client.YogiEssentialsClient;
import dev.yogi.yogiessentials.client.module.pvp.CrosshairModule;
import dev.yogi.yogiessentials.client.util.CombatHitboxColorManager;
import net.minecraft.class_310;
import net.minecraft.class_332;

public final class CrosshairRenderer {

    private CrosshairRenderer() {
    }

    public static boolean shouldOverrideVanilla() {
        class_310 client =
                class_310.method_1551();

        CrosshairModule module =
                getModule();

        if (
                module == null
                        ||
                !module.isEnabled()
        ) {
            return false;
        }

        boolean firstPerson =
                client.field_1690
                        .method_31044()
                        .method_31034();

        return firstPerson
                ||
                module
                        .getShowInThirdPerson()
                        .get();
    }

    public static void render(
            class_332 context
    ) {
        class_310 client =
                class_310.method_1551();

        CrosshairModule module =
                getModule();

        if (
                module == null
                        ||
                !module.isEnabled()
                        ||
                client.field_1724 == null
        ) {
            return;
        }

        boolean firstPerson =
                client.field_1690
                        .method_31044()
                        .method_31034();

        if (
                !firstPerson
                        &&
                !module
                        .getShowInThirdPerson()
                        .get()
        ) {
            return;
        }

        renderAt(
                context,
                module,
                context.method_51421() / 2,
                context.method_51443() / 2,
                client.field_1724
                        .method_7261(
                                0.0F
                        )
        );
    }

    public static void renderPreview(
            class_332 context,
            CrosshairModule module,
            int centerX,
            int centerY
    ) {
        float previewCooldown =
                (
                        System.currentTimeMillis()
                                % 1600L
                )
                        / 1600.0F;

        renderAt(
                context,
                module,
                centerX,
                centerY,
                previewCooldown
        );
    }

    private static void renderAt(
            class_332 context,
            CrosshairModule module,
            int centerX,
            int centerY,
            float cooldownProgress
    ) {
        int size =
                Math.max(
                        1,
                        module
                                .getSize()
                                .get()
                                .intValue()
                );

        int thickness =
                Math.max(
                        1,
                        module
                                .getThickness()
                                .get()
                                .intValue()
                );

        int gap =
                Math.max(
                        0,
                        module
                                .getGap()
                                .get()
                                .intValue()
                );

        if (
                module
                        .getAttackCooldownAnimation()
                        .get()
        ) {
            int animationAmount =
                    Math.round(
                            (
                                    1.0F
                                            - Math.max(
                                            0.0F,
                                            Math.min(
                                                    1.0F,
                                                    cooldownProgress
                                            )
                                    )
                            )
                                    *
                            module
                                    .getCooldownExpansion()
                                    .get()
                                    .floatValue()
                    );

            CrosshairModule.Shape shape =
                    module
                            .getShape()
                            .get();

            if (
                    shape == CrosshairModule.Shape.DOT
                            ||
                    shape == CrosshairModule.Shape.PLUS
            ) {
                size +=
                        animationAmount;
            } else {
                gap +=
                        animationAmount;
            }
        }

        Integer reachColor = CombatHitboxColorManager.activeReachCrosshairColor();
        int configuredMain = reachColor != null ? reachColor : module.getColor().getArgb();
        int mainColor =
                applyOpacity(
                        configuredMain,
                        module
                                .getOpacity()
                                .get()
                                .floatValue()
                );

        int outlineColor =
                applyOpacity(
                        module.getOutlineColor().getArgb(),
                        module
                                .getOpacity()
                                .get()
                                .floatValue()
                );

        boolean outline =
                module
                        .getOutline()
                        .get();

        int outlineThickness =
                Math.max(
                        1,
                        module
                                .getOutlineThickness()
                                .get()
                                .intValue()
                );

        switch (
                module
                        .getShape()
                        .get()
        ) {
            case DOT ->
                    drawDot(
                            context,
                            centerX,
                            centerY,
                            size,
                            mainColor,
                            outline,
                            outlineColor,
                            outlineThickness
                    );

            case PLUS ->
                    drawContinuousPlus(
                            context,
                            centerX,
                            centerY,
                            size,
                            thickness,
                            mainColor,
                            outline,
                            outlineColor,
                            outlineThickness
                    );

            case CROSS ->
                    drawSeparatedCross(
                            context,
                            centerX,
                            centerY,
                            size,
                            gap,
                            thickness,
                            mainColor,
                            outline,
                            outlineColor,
                            outlineThickness,
                            false
                    );

            case T_SHAPE ->
                    drawSeparatedCross(
                            context,
                            centerX,
                            centerY,
                            size,
                            gap,
                            thickness,
                            mainColor,
                            outline,
                            outlineColor,
                            outlineThickness,
                            true
                    );

            case X ->
                    drawX(
                            context,
                            centerX,
                            centerY,
                            size,
                            gap,
                            thickness,
                            mainColor,
                            outline,
                            outlineColor,
                            outlineThickness
                    );
        }
    }

    private static void drawDot(
            class_332 context,
            int centerX,
            int centerY,
            int size,
            int color,
            boolean outline,
            int outlineColor,
            int outlineThickness
    ) {
        int dotSize =
                Math.max(
                        1,
                        size
                );

        if (outline) {
            fillCentered(
                    context,
                    centerX,
                    centerY,
                    dotSize
                            + outlineThickness * 2,
                    dotSize
                            + outlineThickness * 2,
                    outlineColor
            );
        }

        fillCentered(
                context,
                centerX,
                centerY,
                dotSize,
                dotSize,
                color
        );
    }

    private static void drawContinuousPlus(
            class_332 context,
            int centerX,
            int centerY,
            int size,
            int thickness,
            int color,
            boolean outline,
            int outlineColor,
            int outlineThickness
    ) {
        int length =
                size * 2 + thickness;

        if (outline) {
            int expandedThickness =
                    thickness
                            + outlineThickness * 2;

            fillCentered(
                    context,
                    centerX,
                    centerY,
                    length
                            + outlineThickness * 2,
                    expandedThickness,
                    outlineColor
            );

            fillCentered(
                    context,
                    centerX,
                    centerY,
                    expandedThickness,
                    length
                            + outlineThickness * 2,
                    outlineColor
            );
        }

        fillCentered(
                context,
                centerX,
                centerY,
                length,
                thickness,
                color
        );

        fillCentered(
                context,
                centerX,
                centerY,
                thickness,
                length,
                color
        );
    }

    private static void drawSeparatedCross(
            class_332 context,
            int centerX,
            int centerY,
            int size,
            int gap,
            int thickness,
            int color,
            boolean outline,
            int outlineColor,
            int outlineThickness,
            boolean tShape
    ) {
        drawArm(
                context,
                centerX - gap - size,
                centerY - thickness / 2,
                size,
                thickness,
                color,
                outline,
                outlineColor,
                outlineThickness
        );

        drawArm(
                context,
                centerX + gap + 1,
                centerY - thickness / 2,
                size,
                thickness,
                color,
                outline,
                outlineColor,
                outlineThickness
        );

        drawArm(
                context,
                centerX - thickness / 2,
                centerY - gap - size,
                thickness,
                size,
                color,
                outline,
                outlineColor,
                outlineThickness
        );

        if (!tShape) {
            drawArm(
                    context,
                    centerX - thickness / 2,
                    centerY + gap + 1,
                    thickness,
                    size,
                    color,
                    outline,
                    outlineColor,
                    outlineThickness
            );
        }
    }

    private static void drawX(
            class_332 context,
            int centerX,
            int centerY,
            int size,
            int gap,
            int thickness,
            int color,
            boolean outline,
            int outlineColor,
            int outlineThickness
    ) {
        int dot =
                Math.max(
                        1,
                        thickness
                );

        for (
                int distance =
                        gap + 1;
                distance
                        <= gap + size;
                distance++
        ) {
            drawDiagonalPoint(
                    context,
                    centerX + distance,
                    centerY + distance,
                    dot,
                    color,
                    outline,
                    outlineColor,
                    outlineThickness
            );

            drawDiagonalPoint(
                    context,
                    centerX - distance,
                    centerY + distance,
                    dot,
                    color,
                    outline,
                    outlineColor,
                    outlineThickness
            );

            drawDiagonalPoint(
                    context,
                    centerX + distance,
                    centerY - distance,
                    dot,
                    color,
                    outline,
                    outlineColor,
                    outlineThickness
            );

            drawDiagonalPoint(
                    context,
                    centerX - distance,
                    centerY - distance,
                    dot,
                    color,
                    outline,
                    outlineColor,
                    outlineThickness
            );
        }
    }

    private static void drawDiagonalPoint(
            class_332 context,
            int x,
            int y,
            int size,
            int color,
            boolean outline,
            int outlineColor,
            int outlineThickness
    ) {
        if (outline) {
            fillCentered(
                    context,
                    x,
                    y,
                    size
                            + outlineThickness * 2,
                    size
                            + outlineThickness * 2,
                    outlineColor
            );
        }

        fillCentered(
                context,
                x,
                y,
                size,
                size,
                color
        );
    }

    private static void drawArm(
            class_332 context,
            int x,
            int y,
            int width,
            int height,
            int color,
            boolean outline,
            int outlineColor,
            int outlineThickness
    ) {
        if (outline) {
            context.method_25294(
                    x - outlineThickness,
                    y - outlineThickness,
                    x + width
                            + outlineThickness,
                    y + height
                            + outlineThickness,
                    outlineColor
            );
        }

        context.method_25294(
                x,
                y,
                x + width,
                y + height,
                color
        );
    }

    private static void fillCentered(
            class_332 context,
            int centerX,
            int centerY,
            int width,
            int height,
            int color
    ) {
        int x =
                centerX
                        - width / 2;

        int y =
                centerY
                        - height / 2;

        context.method_25294(
                x,
                y,
                x + width,
                y + height,
                color
        );
    }

    private static int applyOpacity(
            int argb,
            float opacity
    ) {
        int alpha =
                Math.round(
                        255.0F
                                * Math.max(
                                0.0F,
                                Math.min(
                                        1.0F,
                                        opacity
                                )
                        )
                );

        return (
                alpha << 24
        )
                |
                (
                        argb
                                & 0x00FFFFFF
                );
    }

    private static CrosshairModule getModule() {
        if (
                YogiEssentialsClient
                        .getModuleManager()
                        == null
        ) {
            return null;
        }

        return YogiEssentialsClient
                .getModuleManager()
                .getModule(
                        CrosshairModule.class
                );
    }
}
