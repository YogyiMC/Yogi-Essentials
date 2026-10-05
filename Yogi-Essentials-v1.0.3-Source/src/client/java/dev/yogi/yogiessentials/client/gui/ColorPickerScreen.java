package dev.yogi.yogiessentials.client.gui;

import dev.yogi.yogiessentials.client.config.ConfigManager;
import dev.yogi.yogiessentials.client.module.Module;
import dev.yogi.yogiessentials.client.render.ModulePreviewRenderer;
import dev.yogi.yogiessentials.client.setting.ColorSetting;
import dev.yogi.yogiessentials.client.util.UiSoundManager;
import org.lwjgl.glfw.GLFW;

import java.util.function.IntConsumer;
import net.minecraft.class_11905;
import net.minecraft.class_11908;
import net.minecraft.class_11909;
import net.minecraft.class_2561;
import net.minecraft.class_332;
import net.minecraft.class_437;

public class ColorPickerScreen extends class_437 {

    private static final int ORANGE =
            0xFFFF6A00;

    private static final int PANEL =
            0xFF101014;

    private static final int CARD =
            0xFF17171C;

    private static final int BORDER =
            0xFF303038;

    private static final int TEXT =
            0xFFF4F4F5;

    private static final int MUTED =
            0xFF9999A1;

    private final class_437 parent;
    private final Module module;
    private final ColorSetting setting;
    private final IntConsumer colorListener;
    private final boolean persistConfig;

    private float hue;
    private float saturation;
    private float value;

    private boolean draggingSquare;
    private boolean draggingHue;

    private boolean hexFocused;
    private String hexText;

    public ColorPickerScreen(
            class_437 parent,
            Module module,
            ColorSetting setting
    ) {
        this(parent, module, setting, null, true);
    }

    



    public ColorPickerScreen(
            class_437 parent,
            String label,
            int initialArgb,
            IntConsumer colorListener
    ) {
        this(
                parent,
                null,
                new ColorSetting(label, initialArgb),
                colorListener,
                false
        );
    }

    private ColorPickerScreen(
            class_437 parent,
            Module module,
            ColorSetting setting,
            IntConsumer colorListener,
            boolean persistConfig
    ) {
        super(class_2561.method_43470("Color Picker"));

        this.parent = parent;
        this.module = module;
        this.setting = setting;
        this.colorListener = colorListener;
        this.persistConfig = persistConfig;

        float[] hsv = rgbToHsv(setting.getRgb());

        this.hue = hsv[0];
        this.saturation = hsv[1];
        this.value = hsv[2];
        this.hexText = setting.getHexRgb().substring(1);
    }

    @Override
    public void method_25394(
            class_332 context,
            int mouseX,
            int mouseY,
            float deltaTicks
    ) {
        context.method_25294(
                0,
                0,
                field_22789,
                field_22790,
                0xFF09090D
        );

        int panelWidth =
                Math.min(
                        760,
                        field_22789 - 40
                );

        int panelHeight =
                Math.min(
                        420,
                        field_22790 - 40
                );

        int panelX =
                (
                        field_22789 - panelWidth
                ) / 2;

        int panelY =
                (
                        field_22790 - panelHeight
                ) / 2;

        context.method_25294(
                panelX,
                panelY,
                panelX + panelWidth,
                panelY + panelHeight,
                PANEL
        );

        outline(
                context,
                panelX,
                panelY,
                panelWidth,
                panelHeight,
                ORANGE
        );

        context.method_25303(
                field_22793,
                "COLOR PICKER",
                panelX + 20,
                panelY + 18,
                ORANGE
        );

        context.method_25303(
                field_22793,
                setting.getName(),
                panelX + 20,
                panelY + 34,
                TEXT
        );

        int squareX =
                panelX + 24;

        int squareY =
                panelY + 66;

        int squareWidth =
                Math.min(
                        300,
                        Math.max(
                                180,
                                panelWidth - 400
                        )
                );

        int squareHeight =
                200;

        renderSvSquare(
                context,
                squareX,
                squareY,
                squareWidth,
                squareHeight
        );

        int hueX =
                squareX
                        + squareWidth
                        + 16;

        int hueWidth =
                18;

        renderHueBar(
                context,
                hueX,
                squareY,
                hueWidth,
                squareHeight
        );

        int previewX =
                hueX + 36;

        int previewWidth =
                panelX
                        + panelWidth
                        - 24
                        - previewX;

        int previewHeight =
                128;

        if (module != null) {
            ModulePreviewRenderer.render(
                    context,
                    module,
                    previewX,
                    squareY,
                    previewWidth,
                    previewHeight
            );
        } else {
            renderColorPreview(
                    context,
                    previewX,
                    squareY,
                    previewWidth,
                    previewHeight
            );
        }

        context.method_25300(
                field_22793,
                setting.getHexRgb(),
                previewX + previewWidth / 2,
                squareY + previewHeight + 10,
                TEXT
        );

        int hexY =
                squareY + previewHeight + 32;

        context.method_25303(
                field_22793,
                "HEX",
                previewX,
                hexY,
                MUTED
        );

        context.method_25294(
                previewX,
                hexY + 14,
                previewX + previewWidth,
                hexY + 40,
                hexFocused
                        ? 0xFF202027
                        : CARD
        );

        outline(
                context,
                previewX,
                hexY + 14,
                previewWidth,
                26,
                hexFocused
                        ? ORANGE
                        : BORDER
        );

        context.method_25303(
                field_22793,
                "#"
                        + hexText,
                previewX + 9,
                hexY + 23,
                TEXT
        );

        context.method_51433(
                field_22793,
                "Click the color square or hue strip, or type an exact hex value.",
                panelX + 24,
                panelY + panelHeight - 68,
                MUTED,
                true
        );

        renderButton(
                context,
                panelX + 24,
                panelY + panelHeight - 42,
                92,
                26,
                "Back",
                mouseX,
                mouseY
        );

        renderButton(
                context,
                panelX + panelWidth - 116,
                panelY + panelHeight - 42,
                92,
                26,
                "Done",
                mouseX,
                mouseY
        );
    }

    private void renderSvSquare(
            class_332 context,
            int x,
            int y,
            int width,
            int height
    ) {
        int step =
                2;

        for (
                int py = 0;
                py < height;
                py += step
        ) {
            float v =
                    1.0F
                            - py
                            / (float) Math.max(
                            1,
                            height - 1
                    );

            for (
                    int px = 0;
                    px < width;
                    px += step
            ) {
                float s =
                        px
                                / (float) Math.max(
                                1,
                                width - 1
                        );

                int color =
                        0xFF000000
                                |
                        hsvToRgb(
                                hue,
                                s,
                                v
                        );

                context.method_25294(
                        x + px,
                        y + py,
                        x
                                + Math.min(
                                width,
                                px + step
                        ),
                        y
                                + Math.min(
                                height,
                                py + step
                        ),
                        color
                );
            }
        }

        outline(
                context,
                x,
                y,
                width,
                height,
                BORDER
        );

        int markerX =
                x
                        + Math.round(
                        saturation
                                * (
                                width - 1
                        )
                );

        int markerY =
                y
                        + Math.round(
                        (
                                1.0F - value
                        )
                                * (
                                height - 1
                        )
                );

        context.method_25294(
                markerX - 3,
                markerY - 3,
                markerX + 4,
                markerY + 4,
                0xFF000000
        );

        context.method_25294(
                markerX - 2,
                markerY - 2,
                markerX + 3,
                markerY + 3,
                0xFFFFFFFF
        );
    }

    private void renderHueBar(
            class_332 context,
            int x,
            int y,
            int width,
            int height
    ) {
        for (
                int py = 0;
                py < height;
                py++
        ) {
            float h =
                    py
                            / (float) Math.max(
                            1,
                            height - 1
                    );

            context.method_25294(
                    x,
                    y + py,
                    x + width,
                    y + py + 1,
                    0xFF000000
                            |
                    hsvToRgb(
                            h,
                            1.0F,
                            1.0F
                    )
            );
        }

        outline(
                context,
                x,
                y,
                width,
                height,
                BORDER
        );

        int markerY =
                y
                        + Math.round(
                        hue
                                * (
                                height - 1
                        )
                );

        context.method_25294(
                x - 3,
                markerY - 1,
                x + width + 3,
                markerY + 2,
                0xFFFFFFFF
        );
    }

    @Override
    public boolean method_25402(
            class_11909 click,
            boolean doubled
    ) {
        if (
                click.method_74245()
                        != 0
        ) {
            return super.method_25402(
                    click,
                    doubled
            );
        }

        int panelWidth =
                Math.min(
                        760,
                        field_22789 - 40
                );

        int panelHeight =
                Math.min(
                        420,
                        field_22790 - 40
                );

        int panelX =
                (
                        field_22789 - panelWidth
                ) / 2;

        int panelY =
                (
                        field_22790 - panelHeight
                ) / 2;

        int squareX =
                panelX + 24;

        int squareY =
                panelY + 66;

        int squareWidth =
                Math.min(
                        300,
                        Math.max(
                                180,
                                panelWidth - 400
                        )
                );

        int squareHeight =
                200;

        int hueX =
                squareX
                        + squareWidth
                        + 16;

        if (
                inside(
                        click.comp_4798(),
                        click.comp_4799(),
                        squareX,
                        squareY,
                        squareWidth,
                        squareHeight
                )
        ) {
            draggingSquare = true;
            updateSquare(
                    click.comp_4798(),
                    click.comp_4799(),
                    squareX,
                    squareY,
                    squareWidth,
                    squareHeight
            );

            return true;
        }

        if (
                inside(
                        click.comp_4798(),
                        click.comp_4799(),
                        hueX,
                        squareY,
                        18,
                        squareHeight
                )
        ) {
            draggingHue = true;
            updateHue(
                    click.comp_4799(),
                    squareY,
                    squareHeight
            );

            return true;
        }

        int previewX =
                hueX + 36;

        int previewWidth =
                panelX
                        + panelWidth
                        - 24
                        - previewX;

        int hexY =
                squareY + 128 + 32;

        if (
                inside(
                        click.comp_4798(),
                        click.comp_4799(),
                        previewX,
                        hexY + 14,
                        previewWidth,
                        26
                )
        ) {
            hexFocused = true;
            return true;
        }

        hexFocused = false;

        if (
                inside(
                        click.comp_4798(),
                        click.comp_4799(),
                        panelX + 24,
                        panelY + panelHeight - 42,
                        92,
                        26
                )
        ) {
            method_25419();
            return true;
        }

        if (
                inside(
                        click.comp_4798(),
                        click.comp_4799(),
                        panelX + panelWidth - 116,
                        panelY + panelHeight - 42,
                        92,
                        26
                )
        ) {
            applyHexIfValid();
            persistIfNeeded();
            UiSoundManager.click();

            if (field_22787 != null) {
                field_22787.method_1507(
                        parent
                );
            }

            return true;
        }

        return super.method_25402(
                click,
                doubled
        );
    }

    @Override
    public boolean method_25403(
            class_11909 click,
            double offsetX,
            double offsetY
    ) {
        int panelWidth =
                Math.min(
                        760,
                        field_22789 - 40
                );

        int panelHeight =
                Math.min(
                        420,
                        field_22790 - 40
                );

        int panelX =
                (
                        field_22789 - panelWidth
                ) / 2;

        int panelY =
                (
                        field_22790 - panelHeight
                ) / 2;

        int squareX =
                panelX + 24;

        int squareY =
                panelY + 66;

        int squareWidth =
                Math.min(
                        300,
                        Math.max(
                                180,
                                panelWidth - 400
                        )
                );

        int squareHeight =
                200;

        if (draggingSquare) {
            updateSquare(
                    click.comp_4798(),
                    click.comp_4799(),
                    squareX,
                    squareY,
                    squareWidth,
                    squareHeight
            );

            return true;
        }

        if (draggingHue) {
            updateHue(
                    click.comp_4799(),
                    squareY,
                    squareHeight
            );

            return true;
        }

        return super.method_25403(
                click,
                offsetX,
                offsetY
        );
    }

    @Override
    public boolean method_25406(
            class_11909 click
    ) {
        draggingSquare = false;
        draggingHue = false;

        persistIfNeeded();

        return super.method_25406(
                click
        );
    }

    @Override
    public boolean method_25400(
            class_11905 input
    ) {
        if (!hexFocused) {
            return super.method_25400(
                    input
            );
        }

        String typed =
                input.method_74226()
                        .toUpperCase();

        if (
                typed.length() == 1
                        &&
                "0123456789ABCDEF"
                        .contains(
                                typed
                        )
                        &&
                hexText.length() < 6
        ) {
            hexText += typed;

            if (
                    hexText.length()
                            == 6
            ) {
                applyHexIfValid();
            }

            return true;
        }

        return false;
    }

    @Override
    public boolean method_25404(
            class_11908 input
    ) {
        if (hexFocused) {
            if (
                    input.comp_4795()
                            == GLFW.GLFW_KEY_BACKSPACE
            ) {
                if (!hexText.isEmpty()) {
                    hexText =
                            hexText.substring(
                                    0,
                                    hexText.length()
                                            - 1
                            );
                }

                return true;
            }

            if (
                    input.comp_4795()
                            == GLFW.GLFW_KEY_ENTER
                            ||
                    input.comp_4795()
                            == GLFW.GLFW_KEY_KP_ENTER
            ) {
                applyHexIfValid();
                hexFocused = false;
                return true;
            }
        }

        return super.method_25404(
                input
        );
    }

    private void updateSquare(
            double mouseX,
            double mouseY,
            int x,
            int y,
            int width,
            int height
    ) {
        saturation =
                (float) Math.max(
                        0.0,
                        Math.min(
                                1.0,
                                (
                                        mouseX - x
                                )
                                        / Math.max(
                                        1.0,
                                        width - 1.0
                                )
                        )
                );

        value =
                1.0F
                        - (float) Math.max(
                        0.0,
                        Math.min(
                                1.0,
                                (
                                        mouseY - y
                                )
                                        / Math.max(
                                        1.0,
                                        height - 1.0
                                )
                        )
                );

        applyCurrentColor();
    }

    private void updateHue(
            double mouseY,
            int y,
            int height
    ) {
        hue =
                (float) Math.max(
                        0.0,
                        Math.min(
                                1.0,
                                (
                                        mouseY - y
                                )
                                        / Math.max(
                                        1.0,
                                        height - 1.0
                                )
                        )
                );

        applyCurrentColor();
    }

    private void applyCurrentColor() {
        setting.setRgb(
                currentRgb()
        );
        notifyColorChanged();

        hexText =
                setting
                        .getHexRgb()
                        .substring(1);
    }

    private void applyHexIfValid() {
        if (
                hexText.length()
                        != 6
        ) {
            return;
        }

        try {
            int rgb =
                    Integer.parseInt(
                            hexText,
                            16
                    );

            setting.setRgb(
                    rgb
            );
            notifyColorChanged();

            float[] hsv =
                    rgbToHsv(
                            rgb
                    );

            hue = hsv[0];
            saturation = hsv[1];
            value = hsv[2];

        } catch (
                NumberFormatException ignored
        ) {
        }
    }

    private int currentRgb() {
        return hsvToRgb(
                hue,
                saturation,
                value
        );
    }

    @Override
    public void method_25419() {
        persistIfNeeded();

        if (field_22787 != null) {
            field_22787.method_1507(
                    parent
            );
        }
    }


    private void renderColorPreview(
            class_332 context,
            int x,
            int y,
            int width,
            int height
    ) {
        context.method_25294(x, y, x + width, y + height, CARD);
        outline(context, x, y, width, height, BORDER);

        int pad = Math.max(10, Math.min(22, Math.min(width, height) / 8));
        context.method_25294(
                x + pad,
                y + pad,
                x + width - pad,
                y + height - pad,
                0xFF000000 | setting.getRgb()
        );
        outline(
                context,
                x + pad,
                y + pad,
                Math.max(1, width - pad * 2),
                Math.max(1, height - pad * 2),
                0xFFFFFFFF
        );
    }

    private void notifyColorChanged() {
        if (colorListener != null) {
            colorListener.accept(setting.getRgb());
        }
    }

    private void persistIfNeeded() {
        if (persistConfig) {
            ConfigManager.save();
        }
    }

    @Override
    public boolean method_25421() {
        return false;
    }

    private void renderButton(
            class_332 context,
            int x,
            int y,
            int width,
            int height,
            String label,
            int mouseX,
            int mouseY
    ) {
        boolean hovered =
                inside(
                        mouseX,
                        mouseY,
                        x,
                        y,
                        width,
                        height
                );

        context.method_25294(
                x,
                y,
                x + width,
                y + height,
                hovered
                        ? 0xFF382117
                        : CARD
        );

        outline(
                context,
                x,
                y,
                width,
                height,
                hovered
                        ? ORANGE
                        : BORDER
        );

        context.method_25300(
                field_22793,
                label,
                x + width / 2,
                y + 9,
                hovered
                        ? ORANGE
                        : TEXT
        );
    }

    private static int hsvToRgb(
            float h,
            float s,
            float v
    ) {
        h =
                h
                        - (float) Math.floor(
                        h
                );

        float chroma =
                v * s;

        float sector =
                h * 6.0F;

        float x =
                chroma
                        * (
                        1.0F
                                - Math.abs(
                                sector
                                        % 2.0F
                                        - 1.0F
                        )
                );

        float r1;
        float g1;
        float b1;

        if (sector < 1.0F) {
            r1 = chroma;
            g1 = x;
            b1 = 0.0F;
        } else if (sector < 2.0F) {
            r1 = x;
            g1 = chroma;
            b1 = 0.0F;
        } else if (sector < 3.0F) {
            r1 = 0.0F;
            g1 = chroma;
            b1 = x;
        } else if (sector < 4.0F) {
            r1 = 0.0F;
            g1 = x;
            b1 = chroma;
        } else if (sector < 5.0F) {
            r1 = x;
            g1 = 0.0F;
            b1 = chroma;
        } else {
            r1 = chroma;
            g1 = 0.0F;
            b1 = x;
        }

        float m =
                v - chroma;

        int r =
                Math.round(
                        (
                                r1 + m
                        )
                                * 255.0F
                );

        int g =
                Math.round(
                        (
                                g1 + m
                        )
                                * 255.0F
                );

        int b =
                Math.round(
                        (
                                b1 + m
                        )
                                * 255.0F
                );

        return (
                clampByte(r) << 16
        )
                |
                (
                        clampByte(g) << 8
                )
                |
                clampByte(b);
    }

    private static float[] rgbToHsv(
            int rgb
    ) {
        float r =
                (
                        (
                                rgb >> 16
                        )
                                & 0xFF
                )
                        / 255.0F;

        float g =
                (
                        (
                                rgb >> 8
                        )
                                & 0xFF
                )
                        / 255.0F;

        float b =
                (
                        rgb
                                & 0xFF
                )
                        / 255.0F;

        float max =
                Math.max(
                        r,
                        Math.max(
                                g,
                                b
                        )
                );

        float min =
                Math.min(
                        r,
                        Math.min(
                                g,
                                b
                        )
                );

        float delta =
                max - min;

        float h;

        if (delta == 0.0F) {
            h = 0.0F;
        } else if (max == r) {
            h =
                    (
                            (
                                    g - b
                            )
                                    / delta
                    )
                            % 6.0F;
        } else if (max == g) {
            h =
                    (
                            b - r
                    )
                            / delta
                            + 2.0F;
        } else {
            h =
                    (
                            r - g
                    )
                            / delta
                            + 4.0F;
        }

        h /= 6.0F;

        if (h < 0.0F) {
            h += 1.0F;
        }

        float s =
                max == 0.0F
                        ? 0.0F
                        : delta / max;

        return new float[]{
                h,
                s,
                max
        };
    }

    private static int clampByte(
            int value
    ) {
        return Math.max(
                0,
                Math.min(
                        255,
                        value
                )
        );
    }

    private void outline(
            class_332 context,
            int x,
            int y,
            int width,
            int height,
            int color
    ) {
        context.method_25294(
                x,
                y,
                x + width,
                y + 1,
                color
        );

        context.method_25294(
                x,
                y + height - 1,
                x + width,
                y + height,
                color
        );

        context.method_25294(
                x,
                y,
                x + 1,
                y + height,
                color
        );

        context.method_25294(
                x + width - 1,
                y,
                x + width,
                y + height,
                color
        );
    }

    private boolean inside(
            double mouseX,
            double mouseY,
            int x,
            int y,
            int width,
            int height
    ) {
        return mouseX >= x
                &&
                mouseX <= x + width
                &&
                mouseY >= y
                &&
                mouseY <= y + height;
    }
}
