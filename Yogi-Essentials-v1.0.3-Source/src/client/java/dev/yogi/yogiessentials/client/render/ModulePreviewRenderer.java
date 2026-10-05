package dev.yogi.yogiessentials.client.render;

import dev.yogi.yogiessentials.client.hud.HudManager;
import dev.yogi.yogiessentials.client.module.Module;
import dev.yogi.yogiessentials.client.module.hud.ArmorHudModule;
import dev.yogi.yogiessentials.client.module.hud.AttackCooldownHudModule;
import dev.yogi.yogiessentials.client.module.hud.CoordinatesHudModule;
import dev.yogi.yogiessentials.client.module.hud.FpsHudModule;
import dev.yogi.yogiessentials.client.module.hud.PingHudModule;
import dev.yogi.yogiessentials.client.module.hud.PotionEffectsHudModule;
import dev.yogi.yogiessentials.client.module.hud.ShieldStatusHudModule;
import dev.yogi.yogiessentials.client.module.hud.StyledHudModule;
import dev.yogi.yogiessentials.client.module.hud.TotemHudModule;
import dev.yogi.yogiessentials.client.module.performance.BorderlessFullscreenModule;
import dev.yogi.yogiessentials.client.module.performance.LowEntityDistanceModule;
import dev.yogi.yogiessentials.client.module.performance.MinimalParticlesModule;
import dev.yogi.yogiessentials.client.module.performance.NoAmbientOcclusionModule;
import dev.yogi.yogiessentials.client.module.performance.NoBiomeBlendModule;
import dev.yogi.yogiessentials.client.module.performance.NoChunkFadeModule;
import dev.yogi.yogiessentials.client.module.performance.NoCloudsModule;
import dev.yogi.yogiessentials.client.module.performance.NoEntityShadowsModule;
import dev.yogi.yogiessentials.client.module.performance.NoVignetteModule;
import dev.yogi.yogiessentials.client.module.pvp.CrosshairModule;
import dev.yogi.yogiessentials.client.module.pvp.WarningModule;
import dev.yogi.yogiessentials.client.module.visual.LowFireModule;
import dev.yogi.yogiessentials.client.module.visual.LowShieldModule;
import dev.yogi.yogiessentials.client.module.visual.ItemViewmodelModule;
import dev.yogi.yogiessentials.client.module.visual.ExtendedFovModule;
import dev.yogi.yogiessentials.client.module.visual.SideShieldModule;
import dev.yogi.yogiessentials.client.module.visual.SmallBlockModule;
import dev.yogi.yogiessentials.client.module.visual.SmallTotemModule;
import org.joml.Matrix3x2fStack;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.class_10799;
import net.minecraft.class_1291;
import net.minecraft.class_1294;
import net.minecraft.class_1799;
import net.minecraft.class_1802;
import net.minecraft.class_310;
import net.minecraft.class_329;
import net.minecraft.class_332;
import net.minecraft.class_6880;

public final class ModulePreviewRenderer {

    private static final int PANEL =
            0xFF101014;

    private static final int PANEL_INNER =
            0xFF15151A;

    private static final int BORDER =
            0xFF303038;

    private static final int ORANGE =
            0xFFFF6A00;

    private static final int TEXT =
            0xFFF4F4F5;

    private static final int MUTED =
            0xFF9999A1;

    private ModulePreviewRenderer() {
    }

    public static void render(
            class_332 context,
            Module module,
            int x,
            int y,
            int width,
            int height
    ) {
        class_310 client =
                class_310.method_1551();

        context.method_25294(
                x,
                y,
                x + width,
                y + height,
                PANEL
        );

        outline(
                context,
                x,
                y,
                width,
                height,
                ORANGE
        );

        context.method_25294(
                x,
                y,
                x + 3,
                y + height,
                ORANGE
        );

        context.method_25303(
                client.field_1772,
                "LIVE PREVIEW",
                x + 13,
                y + 12,
                ORANGE
        );

        drawPreviewText(context, 
                client.field_1772,
                module.getName(),
                x + 13,
                y + 29,
                TEXT,
                true
        );

        int innerX =
                x + 13;

        int innerY =
                y + 50;

        int innerWidth =
                width - 26;

        int innerHeight =
                height - 63;

        context.method_25294(
                innerX,
                innerY,
                innerX + innerWidth,
                innerY + innerHeight,
                PANEL_INNER
        );

        outline(
                context,
                innerX,
                innerY,
                innerWidth,
                innerHeight,
                0xFF26262D
        );

        if (
                module instanceof CrosshairModule crosshair
        ) {
            CrosshairRenderer.renderPreview(
                    context,
                    crosshair,
                    innerX + innerWidth / 2,
                    innerY + innerHeight / 2
            );

            return;
        }

        if (
                module instanceof LowFireModule
                        ||
                module instanceof LowShieldModule
                        ||
                module instanceof SideShieldModule
                        ||
                module instanceof SmallBlockModule
                        ||
                module instanceof SmallTotemModule
                        ||
                module instanceof ItemViewmodelModule
        ) {
            int square =
                    Math.min(
                            innerWidth,
                            innerHeight
                    );

            int previewX =
                    innerX
                            + (
                            innerWidth - square
                    ) / 2;

            int previewY =
                    innerY
                            + (
                            innerHeight - square
                    ) / 2;

            FirstPersonPreviewRenderer.render(
                    context,
                    module,
                    previewX,
                    previewY,
                    square,
                    square
            );

            return;
        }

        if (
                module instanceof StyledHudModule hud
        ) {
            renderHudPreview(
                    context,
                    client,
                    hud,
                    innerX,
                    innerY,
                    innerWidth,
                    innerHeight
            );

            return;
        }

        if (
                module instanceof WarningModule warning
        ) {
            renderWarningPreview(
                    context,
                    client,
                    warning,
                    innerX,
                    innerY,
                    innerWidth,
                    innerHeight
            );

            return;
        }

        if (
                isPerformanceModule(
                        module
                )
        ) {
            renderPerformancePreview(
                    context,
                    client,
                    module,
                    innerX,
                    innerY,
                    innerWidth,
                    innerHeight
            );

            return;
        }

        renderGenericPreview(
                context,
                client,
                module,
                innerX,
                innerY,
                innerWidth,
                innerHeight
        );
    }

    private static void renderHudPreview(
            class_332 context,
            class_310 client,
            StyledHudModule module,
            int x,
            int y,
            int width,
            int height
    ) {
        context.method_44379(
                x,
                y,
                x + width,
                y + height
        );

        int baseWidth =
                HudManager.getBaseWidth(
                        module
                );

        int baseHeight =
                HudManager.getBaseHeight(
                        module
                );

        float scale =
                module
                        .getScale()
                        .get()
                        .floatValue();

        int renderedWidth =
                Math.round(
                        baseWidth * scale
                );

        int renderedHeight =
                Math.round(
                        baseHeight * scale
                );

        int drawX =
                x
                        + (
                        width - renderedWidth
                ) / 2;

        int drawY =
                y
                        + (
                        height - renderedHeight
                ) / 2;

        Matrix3x2fStack matrices =
                context.method_51448();

        matrices.pushMatrix();

        matrices.translate(
                drawX,
                drawY
        );

        matrices.scale(
                scale,
                scale
        );

        if (module instanceof ArmorHudModule armor
                && armor.getIndividualArmorPositions().get()
                && armor.isIconMode()) {
            armor.enforceIndividualModeStyle();
        }

        boolean renderBackground = true;
        if (module instanceof TotemHudModule totem
                && totem.getDisplayMode().get() == TotemHudModule.DisplayMode.TEXT) {
            renderBackground = totem.getTextBackground().get();
        } else if (module instanceof PotionEffectsHudModule potion
                && potion.getDisplayMode().get() == PotionEffectsHudModule.DisplayMode.TEXT) {
            renderBackground = potion.getTextBackground().get();
        }

        drawHudCard(
                context,
                module,
                0,
                0,
                baseWidth,
                baseHeight,
                renderBackground
        );

        if (
                module
                        instanceof ArmorHudModule armor
        ) {
            renderArmorHudPreview(
                    context,
                    client,
                    armor,
                    baseWidth,
                    baseHeight
            );

            matrices.popMatrix();
            context.method_44380();
            return;
        }

        if (
                module
                        instanceof TotemHudModule totem
        ) {
            renderTotemHudPreview(
                    context,
                    client,
                    totem,
                    baseWidth,
                    baseHeight
            );

            matrices.popMatrix();
            context.method_44380();
            return;
        }

        if (
                module
                        instanceof ShieldStatusHudModule shield
        ) {
            renderShieldHudPreview(
                    context,
                    client,
                    shield,
                    baseWidth,
                    baseHeight
            );

            matrices.popMatrix();
            context.method_44380();
            return;
        }

        if (
                module
                        instanceof PotionEffectsHudModule potion
        ) {
            renderPotionHudPreview(
                    context,
                    client,
                    potion,
                    baseWidth,
                    baseHeight
            );

            matrices.popMatrix();
            context.method_44380();
            return;
        }

        String label = sampleLabel(module);
        String value = sampleValue(module);
        int padding = module.getPadding().get().intValue();
        boolean showLabel = module.getShowLabel().get();
        boolean showValue = module.getShowValue().get();
        String renderedLabel = previewHudLabel(module, label);
        int usableWidth = Math.max(1, baseWidth - padding * 2);
        String visibleValue = value == null ? "" : value;
        int valueWidth = client.field_1772.method_1727(visibleValue);
        int labelX = padding + module.getLabelOffsetX().get().intValue();
        int labelY = padding + 2 + module.getLabelOffsetY().get().intValue();
        boolean compact = module.getCompactMode().get();
        int valueX = (compact ? padding : baseWidth - padding - valueWidth)
                + module.getValueOffsetX().get().intValue();
        int valueY = padding + 2 + module.getValueOffsetY().get().intValue();

        valueX = resolvePreviewValueX(
                client, renderedLabel, labelX, labelY, valueX, valueY, valueWidth,
                showLabel && showValue
        );

        if (showLabel && !renderedLabel.isEmpty()) {
            drawPreviewText(context, 
                    client.field_1772,
                    renderedLabel,
                    labelX,
                    labelY,
                    module.getLabelColor().getArgb(),
                    module.getTextShadow().get()
            );
        }

        if (showValue && !visibleValue.isEmpty()) {
            drawPreviewText(context, 
                    client.field_1772,
                    visibleValue,
                    valueX,
                    valueY,
                    module.getValueColor().getArgb(),
                    module.getTextShadow().get()
            );
        }

        if (
                module
                        instanceof AttackCooldownHudModule
                        &&
                module
                        .getShowValue()
                        .get()
        ) {
            int barX =
                    padding;

            int barY =
                    baseHeight
                            - padding
                            - 4;

            int barWidth =
                    baseWidth
                            - padding * 2;

            context.method_25294(
                    barX,
                    barY,
                    barX + barWidth,
                    barY + 4,
                    0xFF34343A
            );

            context.method_25294(
                    barX,
                    barY,
                    barX
                            + Math.round(
                            barWidth * 0.76F
                    ),
                    barY + 4,
                    module
                            .getAccentColor()
                            .getArgb()
            );
        }

        matrices.popMatrix();
        context.method_44380();
    }

    private static void renderArmorHudPreview(
            class_332 context,
            class_310 client,
            ArmorHudModule module,
            int width,
            int height
    ) {
        if (module.getIndividualArmorPositions().get()) {
            return;
        }
        ArmorHudModule.DisplayMode mode = module.getDisplayMode().get();
        int padding = module.getPadding().get().intValue();
        List<String> previewValues = new ArrayList<>(List.of("100%", "82%", "64%", "47%"));
        if (module.getShowTools().get()) previewValues.add("73%");
        if (module.getShowOffhand().get()) previewValues.add("91%");
        String[] values = previewValues.toArray(String[]::new);

        if (mode == ArmorHudModule.DisplayMode.TEXT) {
            List<String> previewLabels = new ArrayList<>(List.of("HELMET", "CHEST", "LEGS", "BOOTS"));
            if (module.getShowTools().get()) previewLabels.add("TOOL");
            if (module.getShowOffhand().get()) previewLabels.add("OFFHAND");
            String[] labels = previewLabels.toArray(String[]::new);
            int rowY = padding + (module.getShowLabel().get() ? 17 : 0);
            if (module.getShowLabel().get()) {
                drawPreviewText(context, client.field_1772, previewHudLabel(module, "ARMOR"),
                        padding + module.getLabelOffsetX().get().intValue(),
                        padding + module.getLabelOffsetY().get().intValue(),
                        module.getLabelColor().getArgb(), module.getTextShadow().get());
            }
            for (int i = 0; i < labels.length; i++) {
                if (module.getShowLabel().get()) {
                    drawPreviewText(context, client.field_1772, labels[i],
                            padding + module.getLabelOffsetX().get().intValue(),
                            rowY + i * 14 + module.getLabelOffsetY().get().intValue(),
                            module.getLabelColor().getArgb(), module.getTextShadow().get());
                }
                if (module.getShowValue().get()) {
                    int tw = client.field_1772.method_1727(values[i]);
                    drawPreviewText(context, client.field_1772, values[i],
                            width - padding - tw + module.getValueOffsetX().get().intValue(),
                            rowY + i * 14 + module.getValueOffsetY().get().intValue(),
                            module.getValueColor().getArgb(), module.getTextShadow().get());
                }
            }
            return;
        }

        boolean horizontal = HudManager.resolveArmorHorizontal(module);
        int spacing = module.getIconSpacing().get().intValue();

        if (mode == ArmorHudModule.DisplayMode.PERCENT_ONLY) {
            int count = Math.max(1, values.length);
            int totalSpacing = spacing * Math.max(0, count - 1);
            int cellWidth = horizontal ? Math.max(1, (width - padding * 2 - totalSpacing) / count) : Math.max(1, width - padding * 2);
            int cellHeight = horizontal ? Math.max(client.field_1772.field_2000, height - padding * 2)
                    : Math.max(client.field_1772.field_2000, (height - padding * 2 - totalSpacing) / count);
            for (int i = 0; i < count; i++) {
                int tw = client.field_1772.method_1727(values[i]);
                int x = horizontal ? padding + i * (cellWidth + spacing) + Math.max(0, (cellWidth - tw) / 2)
                        : padding + Math.max(0, (cellWidth - tw) / 2);
                int y = horizontal ? padding + Math.max(0, (cellHeight - client.field_1772.field_2000) / 2)
                        : padding + i * (cellHeight + spacing) + Math.max(0, (cellHeight - client.field_1772.field_2000) / 2);
                drawPreviewText(context, client.field_1772, values[i], x, y, module.getValueColor().getArgb(), module.getTextShadow().get());
            }
            return;
        }

        List<class_1799> previewStacks = new ArrayList<>(List.of(
                new class_1799(class_1802.field_8805), new class_1799(class_1802.field_8058),
                new class_1799(class_1802.field_8348), new class_1799(class_1802.field_8285)
        ));
        if (module.getShowTools().get()) previewStacks.add(new class_1799(class_1802.field_8802));
        if (module.getShowOffhand().get()) previewStacks.add(new class_1799(class_1802.field_8255));
        class_1799[] stacks = previewStacks.toArray(class_1799[]::new);
        int requestedIcon = module.getIconSize().get().intValue();
        boolean percentages = mode == ArmorHudModule.DisplayMode.ICONS_PERCENT && module.getShowValue().get();
        int labelRoom = module.getShowLabel().get() ? 14 : 0;
        int contentY = padding + labelRoom;

        if (module.getShowLabel().get()) {
            drawPreviewText(context, client.field_1772, previewHudLabel(module, "ARMOR"),
                    padding + module.getLabelOffsetX().get().intValue(),
                    padding + module.getLabelOffsetY().get().intValue(),
                    module.getLabelColor().getArgb(), module.getTextShadow().get());
        }

        for (int i = 0; i < stacks.length; i++) {
            int icon, iconX, iconY;
            if (horizontal) {
                int available = Math.max(stacks.length * 10, width - padding * 2 - spacing * Math.max(0, stacks.length - 1));
                int cell = Math.max(10, available / Math.max(1, stacks.length));
                icon = Math.max(10, Math.min(requestedIcon, cell));
                iconX = padding + i * (cell + spacing) + Math.max(0, (cell - icon) / 2);
                iconY = Math.min(Math.max(0, height - icon), contentY);
            } else {
                int availableH = Math.max(stacks.length * 10, height - contentY - padding - spacing * Math.max(0, stacks.length - 1));
                int cell = Math.max(10, availableH / Math.max(1, stacks.length));
                icon = Math.max(10, Math.min(requestedIcon, cell));
                iconX = padding;
                iconY = contentY + i * (cell + spacing) + Math.max(0, (cell - icon) / 2);
            }
            if (module.getIndividualArmorPositions().get()) {
                iconX += module.getPieceOffsetXValue(i);
                iconY += module.getPieceOffsetYValue(i);
                icon = Math.max(4, (int) Math.round(icon * module.getPieceScale(i).get()));
            }

            drawScaledItemPreview(context, stacks[i], iconX, iconY, icon);
            if (module.getShowDurabilityBar().get()) {
                int percent = 100 - i * 18;
                if (percent < 100) {
                    context.method_25294(iconX, iconY + icon - 2, iconX + icon, iconY + icon, 0xCC25252B);
                    context.method_25294(iconX, iconY + icon - 2,
                            iconX + Math.round(icon * percent / 100.0F), iconY + icon,
                            module.getDurabilityBarColorForPercent(percent));
                }
            }
            if (percentages) {
                int textX = horizontal ? iconX + Math.max(0, (icon - client.field_1772.method_1727(values[i])) / 2) : iconX + icon + 6;
                int textY = horizontal ? iconY + icon + 2 : iconY + Math.max(0, (icon - client.field_1772.field_2000) / 2);
                drawPreviewText(context, client.field_1772, values[i],
                        textX + module.getValueOffsetX().get().intValue(),
                        textY + module.getValueOffsetY().get().intValue(),
                        module.getValueColor().getArgb(), module.getTextShadow().get());
            }
        }
    }

    private static void renderTotemHudPreview(
            class_332 context,
            class_310 client,
            TotemHudModule module,
            int width,
            int height
    ) {
        TotemHudModule.DisplayMode mode =
                module
                        .getDisplayMode()
                        .get();

        if (
                mode
                        == TotemHudModule.DisplayMode.TEXT
        ) {
            drawSimpleHudText(
                    context,
                    client,
                    module,
                    previewHudLabel(
                            module,
                            "TOTEMS"
                    ),
                    "7",
                    width
            );

            return;
        }

        int padding =
                module
                        .getPadding()
                        .get()
                        .intValue();

        int icon =
                module
                        .getIconSize()
                        .get()
                        .intValue();

        int contentY =
                padding;

        if (
                module
                        .getShowLabel()
                        .get()
        ) {
            drawPreviewText(context, 
                    client.field_1772,
                    previewHudLabel(
                            module,
                            "TOTEMS"
                    ),
                    padding
                            + module
                            .getLabelOffsetX()
                            .get()
                            .intValue(),
                    padding
                            + module
                            .getLabelOffsetY()
                            .get()
                            .intValue(),
                    module
                            .getLabelColor()
                            .getArgb(),
                    module
                            .getTextShadow()
                            .get()
            );

            contentY += 14;
        }

        drawScaledItemPreview(
                context,
                new class_1799(
                        class_1802.field_8288
                ),
                padding,
                contentY,
                icon
        );

        if (mode == TotemHudModule.DisplayMode.ICON_OVERLAY_COUNT
                && module.getShowValue().get()) {
            drawPreviewOverlayCount(
                    context, client, "7", padding, contentY, icon,
                    module.getValueColor().getArgb(), module.getTextShadow().get()
            );
            return;
        }

        if (
                mode
                        == TotemHudModule.DisplayMode.ICON_COUNT
                &&
                module
                        .getShowValue()
                        .get()
        ) {
            drawPreviewText(context, 
                    client.field_1772,
                    "7",
                    padding + icon + 6
                            + module.getValueOffsetX().get().intValue(),
                    contentY
                            + Math.max(
                            0,
                            (
                                    icon
                                            - client
                                            .field_1772
                                            .field_2000
                            ) / 2
                    )
                            + module
                            .getValueOffsetY()
                            .get()
                            .intValue(),
                    module
                            .getValueColor()
                            .getArgb(),
                    module
                            .getTextShadow()
                            .get()
            );
        }
    }

    private static void renderShieldHudPreview(
            class_332 context,
            class_310 client,
            ShieldStatusHudModule module,
            int width,
            int height
    ) {
        ShieldStatusHudModule.DisplayMode mode =
                module
                        .getDisplayMode()
                        .get();

        if (
                mode
                        == ShieldStatusHudModule.DisplayMode.TEXT
        ) {
            drawSimpleHudText(
                    context,
                    client,
                    module,
                    previewHudLabel(
                            module,
                            "SHIELD"
                    ),
                    "READY",
                    width
            );

            return;
        }

        int padding =
                module
                        .getPadding()
                        .get()
                        .intValue();

        int icon =
                module
                        .getIconSize()
                        .get()
                        .intValue();

        int contentY =
                padding;

        if (
                module
                        .getShowLabel()
                        .get()
        ) {
            drawPreviewText(context, 
                    client.field_1772,
                    previewHudLabel(
                            module,
                            "SHIELD"
                    ),
                    padding
                            + module
                            .getLabelOffsetX()
                            .get()
                            .intValue(),
                    padding
                            + module
                            .getLabelOffsetY()
                            .get()
                            .intValue(),
                    module
                            .getLabelColor()
                            .getArgb(),
                    module
                            .getTextShadow()
                            .get()
            );

            contentY += 14;
        }

        drawScaledItemPreview(
                context,
                new class_1799(
                        class_1802.field_8255
                ),
                padding,
                contentY,
                icon
        );

        if (
                mode
                        == ShieldStatusHudModule.DisplayMode.ICON_TEXT
                &&
                module
                        .getShowValue()
                        .get()
        ) {
            drawPreviewText(context, 
                    client.field_1772,
                    "READY",
                    padding + icon + 6
                            + module.getValueOffsetX().get().intValue(),
                    contentY
                            + Math.max(
                            0,
                            (
                                    icon
                                            - client
                                            .field_1772
                                            .field_2000
                            ) / 2
                    )
                            + module
                            .getValueOffsetY()
                            .get()
                            .intValue(),
                    module
                            .getValueColor()
                            .getArgb(),
                    module
                            .getTextShadow()
                            .get()
            );
        }
    }

    private static void renderPotionHudPreview(
            class_332 context,
            class_310 client,
            PotionEffectsHudModule module,
            int width,
            int height
    ) {
        String[] effects = {
                "Strength II",
                "Speed II",
                "Fire Resistance"
        };

        String[] timers = {
                "0:42",
                "1:18",
                "4:31"
        };

        List<class_6880<class_1291>> effectTypes =
                List.of(
                        class_1294.field_5910,
                        class_1294.field_5904,
                        class_1294.field_5918
                );

        int padding =
                module
                        .getPadding()
                        .get()
                        .intValue();

        int icon =
                module
                        .getIconSize()
                        .get()
                        .intValue();

        PotionEffectsHudModule.DisplayMode mode =
                module
                        .getDisplayMode()
                        .get();

        if (mode == PotionEffectsHudModule.DisplayMode.ICONS) {
            int iconSpacing = module.getIconSpacing().get().intValue();
            boolean horizontal = HudManager.resolvePotionIconsHorizontal(module);

            for (int index = 0; index < effectTypes.size(); index++) {
                int iconX = horizontal ? padding + index * (icon + iconSpacing) : padding;
                int iconY = horizontal ? padding : padding + index * (icon + iconSpacing);
                if (iconX + icon > width || iconY + icon > height) break;

                context.method_52706(
                        class_10799.field_56883,
                        class_329.method_71644(effectTypes.get(index)),
                        iconX, iconY, icon, icon
                );

                if (module.getShowTimer().get()) {
                    drawTinyPreviewTimer(
                            context, client, timers[index], iconX, iconY, icon,
                            module.getValueColor().getArgb(), module.getIconTimerShadow().get()
                    );
                }
            }

            return;
        }

        int rowY =
                padding;

        if (
                module
                        .getShowLabel()
                        .get()
        ) {
            drawPreviewText(context, 
                    client.field_1772,
                    previewHudLabel(
                            module,
                            "EFFECTS"
                    ),
                    padding
                            + module
                            .getLabelOffsetX()
                            .get()
                            .intValue(),
                    rowY
                            + module
                            .getLabelOffsetY()
                            .get()
                            .intValue(),
                    module
                            .getLabelColor()
                            .getArgb(),
                    module
                            .getTextShadow()
                            .get()
            );

            rowY += 16;
        }

        int spacing =
                module
                        .getRowSpacing()
                        .get()
                        .intValue();

        for (
                int index = 0;
                index < effects.length;
                index++
        ) {
            if (mode == PotionEffectsHudModule.DisplayMode.TEXT) {
                drawPreviewText(context, 
                        client.field_1772,
                        effects[index],
                        padding
                                + module
                                .getLabelOffsetX()
                                .get()
                                .intValue(),
                        rowY
                                + module
                                .getLabelOffsetY()
                                .get()
                                .intValue(),
                        module
                                .getEffectTextColor()
                                .getArgb(),
                        module
                                .getTextShadow()
                                .get()
                );
            } else {
                context.method_52706(
                        class_10799.field_56883,
                        class_329.method_71644(
                                effectTypes.get(index)
                        ),
                        padding,
                        rowY,
                        icon,
                        icon
                );

                if (
                        mode
                                == PotionEffectsHudModule.DisplayMode.ICON_NAME_TIMER
                ) {
                    drawPreviewText(context, 
                            client.field_1772,
                            effects[index],
                            padding + icon + 5
                                    + module
                                    .getLabelOffsetX()
                                    .get()
                                    .intValue(),
                            rowY
                                    + module
                                    .getLabelOffsetY()
                                    .get()
                                    .intValue()
                                    + Math.max(
                                    0,
                                    (
                                            icon
                                                    - client
                                                    .field_1772
                                                    .field_2000
                                    ) / 2
                            ),
                            module
                                    .getEffectTextColor()
                                    .getArgb(),
                            module
                                    .getTextShadow()
                                    .get()
                    );
                }
            }

            if (
                    module
                            .getShowTimer()
                            .get()
                            && module
                            .getShowValue()
                            .get()
            ) {
                int timerWidth =
                        client
                                .field_1772
                                .method_1727(
                                        timers[index]
                                );

                drawPreviewText(context, 
                        client.field_1772,
                        timers[index],
                        width
                                - padding
                                - timerWidth
                                + module
                                .getValueOffsetX()
                                .get()
                                .intValue(),
                        rowY
                                + module
                                .getValueOffsetY()
                                .get()
                                .intValue()
                                + Math.max(
                                0,
                                (
                                        spacing
                                                - client
                                                .field_1772
                                                .field_2000
                                ) / 2
                        ),
                        module
                                .getValueColor()
                                .getArgb(),
                        module
                                .getTextShadow()
                                .get()
                );
            }

            rowY += spacing;
        }
    }

    private static void drawPreviewOverlayCount(
            class_332 context,
            class_310 client,
            String value,
            int iconX,
            int iconY,
            int iconSize,
            int color,
            boolean shadow
    ) {
        int textWidth = Math.max(1, client.field_1772.method_1727(value));
        float textScale = Math.max(0.55F, Math.min(0.82F, (iconSize - 3.0F) / textWidth));
        Matrix3x2fStack matrices = context.method_51448();
        matrices.pushMatrix();
        matrices.translate(iconX + iconSize - 1.0F, iconY + iconSize - 1.0F);
        matrices.scale(textScale, textScale);
        int tx = -textWidth;
        int ty = -client.field_1772.field_2000;
        if (shadow) {
            int shadowColor = (color & 0xFF000000) | ((color >> 2) & 0x003F3F3F);
            drawPreviewText(context, client.field_1772, value, tx + 2, ty + 2, shadowColor, false);
        }
        drawPreviewText(context, client.field_1772, value, tx, ty, color, false);
        matrices.popMatrix();
    }

    private static void drawTinyPreviewTimer(
            class_332 context,
            class_310 client,
            String timer,
            int x,
            int y,
            int icon,
            int color,
            boolean shadow
    ) {
        int textWidth = Math.max(1, client.field_1772.method_1727(timer));
        float scale = Math.max(0.42F, Math.min(0.62F, (icon - 2.0F) / textWidth));
        int strip = Math.max(5, Math.round(client.field_1772.field_2000 * scale) + 1);
        context.method_25294(x, y + icon - strip, x + icon, y + icon, 0x99000000);
        Matrix3x2fStack matrices = context.method_51448();
        matrices.pushMatrix();
        matrices.translate(x + icon / 2.0F, y + icon - strip + 1.0F);
        matrices.scale(scale, scale);
        
        
        if (shadow) {
            int shadowColor = (color & 0xFF000000) | ((color >> 2) & 0x003F3F3F);
            drawPreviewText(context, client.field_1772, timer, -textWidth / 2 + 2, 2, shadowColor, false);
        }
        drawPreviewText(context, client.field_1772, timer, -textWidth / 2, 0, color, false);
        matrices.popMatrix();
    }

    private static void drawSimpleHudText(
            class_332 context,
            class_310 client,
            StyledHudModule module,
            String label,
            String value,
            int width
    ) {
        int padding =
                module
                        .getPadding()
                        .get()
                        .intValue();

        int labelX =
                padding
                        + module
                        .getLabelOffsetX()
                        .get()
                        .intValue();

        int labelY =
                padding + 2
                        + module
                        .getLabelOffsetY()
                        .get()
                        .intValue();

        if (
                module
                        .getShowLabel()
                        .get()
        ) {
            drawPreviewText(context, 
                    client.field_1772,
                    label,
                    labelX,
                    labelY,
                    module
                            .getLabelColor()
                            .getArgb(),
                    module
                            .getTextShadow()
                            .get()
            );
        }

        if (
                module
                        .getShowValue()
                        .get()
        ) {
            int valueWidth =
                    client
                            .field_1772
                            .method_1727(
                                    value
                            );

            int baseValueX =
                    module
                            .getCompactMode()
                            .get()
                            ? padding
                            : width
                            - padding
                            - valueWidth;

            drawPreviewText(context, 
                    client.field_1772,
                    value,
                    baseValueX
                            + module
                            .getValueOffsetX()
                            .get()
                            .intValue(),
                    padding + 2
                            + module
                            .getValueOffsetY()
                            .get()
                            .intValue(),
                    module
                            .getValueColor()
                            .getArgb(),
                    module
                            .getTextShadow()
                            .get()
            );
        }
    }


    private static void drawScaledItemPreview(
            class_332 context,
            class_1799 stack,
            int x,
            int y,
            int size
    ) {
        float scale =
                Math.max(
                        0.25F,
                        size / 16.0F
                );

        Matrix3x2fStack matrices =
                context.method_51448();

        matrices.pushMatrix();
        matrices.translate(
                x,
                y
        );
        matrices.scale(
                scale,
                scale
        );

        context.method_51427(
                stack,
                0,
                0
        );

        matrices.popMatrix();
    }

    private static void renderWarningPreview(
            class_332 context,
            class_310 client,
            WarningModule module,
            int x,
            int y,
            int width,
            int height
    ) {
        if (!module.getScreenWarning().get()) {
            int centerX = x + width / 2;
            int centerY = y + height / 2;

            context.method_25300(
                    client.field_1772,
                    "SCREEN WARNING DISABLED",
                    centerX,
                    centerY - 15,
                    MUTED
            );

            String soundText;

            if (module.getSound().get()) {
                String selected =
                        module
                                .getWarningSound()
                                .get()
                                .name()
                                .replace('_', ' ');

                int volumePercent =
                        (int) Math.round(
                                module
                                        .getSoundVolume()
                                        .get()
                                        * 100.0
                        );

                soundText =
                        "Sound only: "
                                + selected
                                + "  •  "
                                + volumePercent
                                + "%";
            } else {
                soundText = "Warning sound is also disabled.";
            }

            context.method_25300(
                    client.field_1772,
                    soundText,
                    centerX,
                    centerY + 4,
                    module.getSound().get()
                            ? ORANGE
                            : MUTED
            );

            return;
        }

        String message =
                sampleWarning(
                        module
                );

        float scale =
                module
                        .getScale()
                        .get()
                        .floatValue();

        int baseWidth =
                Math.min(
                        HudManager.WARNING_WIDTH,
                        Math.max(
                                40,
                                width - 24
                        )
                );

        int baseHeight =
                HudManager.WARNING_HEIGHT;

        int drawX =
                x
                        + (
                        width
                                - Math.round(
                                baseWidth * scale
                        )
                ) / 2;

        int drawY =
                y
                        + (
                        height
                                - Math.round(
                                baseHeight * scale
                        )
                ) / 2;

        context.method_44379(
                x,
                y,
                x + width,
                y + height
        );

        Matrix3x2fStack matrices =
                context.method_51448();

        matrices.pushMatrix();

        matrices.translate(
                drawX,
                drawY
        );

        matrices.scale(
                scale,
                scale
        );

        if (
                module
                        .getBackground()
                        .get()
        ) {
            int background =
                    withOpacity(
                            module
                                    .getBackgroundColor()
                                    .getArgb(),
                            module
                                    .getBackgroundOpacity()
                                    .get()
                                    .floatValue()
                    );

            context.method_25294(
                    0,
                    0,
                    baseWidth,
                    baseHeight,
                    background
            );
        }

        context.method_25294(
                0,
                0,
                3,
                baseHeight,
                module
                        .getAccentColor()
                        .getArgb()
        );

        drawPreviewText(context, 
                client.field_1772,
                message,
                10,
                9,
                module
                        .getTextColor()
                        .getArgb(),
                module
                        .getTextShadow()
                        .get()
        );

        matrices.popMatrix();
        context.method_44380();
    }

    private static void renderShieldPreview(
            class_332 context,
            class_310 client,
            int x,
            int y,
            int width,
            int height,
            float offsetX,
            float offsetY,
            float scale,
            float rotation,
            String label
    ) {
        renderItemPreview(
                context,
                client,
                new class_1799(
                        class_1802.field_8255
                ),
                x,
                y,
                width,
                height,
                offsetX,
                offsetY,
                scale,
                rotation,
                label
        );
    }

    private static void renderItemPreview(
            class_332 context,
            class_310 client,
            class_1799 stack,
            int x,
            int y,
            int width,
            int height,
            float offsetX,
            float offsetY,
            float scale,
            float rotation,
            String label
    ) {
        int itemX =
                x
                        + width / 2
                        - 8
                        + Math.round(
                        offsetX * 70.0F
                );

        int itemY =
                y
                        + height / 2
                        - 8
                        + Math.round(
                        offsetY * 70.0F
                );

        Matrix3x2fStack matrices =
                context.method_51448();

        matrices.pushMatrix();

        matrices.translate(
                itemX + 8,
                itemY + 8
        );

        matrices.rotate(
                (float) Math.toRadians(
                        rotation
                )
        );

        matrices.translate(
                -8,
                -8
        );

        matrices.scale(
                Math.max(
                        0.35F,
                        Math.min(
                                1.8F,
                                scale
                        )
                ),
                Math.max(
                        0.35F,
                        Math.min(
                                1.8F,
                                scale
                        )
                )
        );

        context.method_51427(
                stack,
                0,
                0
        );

        matrices.popMatrix();

        context.method_25300(
                client.field_1772,
                label,
                x + width / 2,
                y + height - 22,
                MUTED
        );
    }

    private static void renderFirePreview(
            class_332 context,
            class_310 client,
            int x,
            int y,
            int width,
            int height,
            float configuredHeight
    ) {
        int bottom =
                y + height - 20;

        int center =
                x + width / 2;

        for (
                int i = 0;
                i < 5;
                i++
        ) {
            int flameWidth =
                    16 - i * 2;

            int flameHeight =
                    Math.max(
                            4,
                            Math.round(
                                    (
                                            13 + i * 4
                                    )
                                            * Math.max(
                                            0.10F,
                                            configuredHeight
                                    )
                            )
                    );

            int flameX =
                    center
                            - flameWidth / 2
                            + (
                            i - 2
                    )
                            * 12;

            context.method_25294(
                    flameX,
                    bottom - flameHeight,
                    flameX + flameWidth,
                    bottom,
                    i % 2 == 0
                            ? 0xFFFF6A00
                            : 0xFFFFB000
            );
        }

        context.method_25300(
                client.field_1772,
                "LOWER FIRE = MORE VISIBILITY",
                center,
                y + 14,
                MUTED
        );
    }

    private static void renderPerformancePreview(
            class_332 context,
            class_310 client,
            Module module,
            int x,
            int y,
            int width,
            int height
    ) {
        int centerX =
                x + width / 2;

        int barWidth =
                Math.min(
                        210,
                        width - 50
                );

        int barX =
                centerX
                        - barWidth / 2;

        int top =
                y + height / 2 - 28;

        context.method_25300(
                client.field_1772,
                module.isEnabled()
                        ? "OPTIMIZED"
                        : "VANILLA",
                centerX,
                top - 22,
                module.isEnabled()
                        ? 0xFF74E08C
                        : MUTED
        );

        context.method_25294(
                barX,
                top,
                barX + barWidth,
                top + 8,
                0xFF2B2B31
        );

        context.method_25294(
                barX,
                top,
                barX
                        + (
                        module.isEnabled()
                                ? Math.round(
                                barWidth * 0.88F
                        )
                                : Math.round(
                                barWidth * 0.55F
                        )
                ),
                top + 8,
                module.isEnabled()
                        ? 0xFF74E08C
                        : ORANGE
        );

        context.method_25300(
                client.field_1772,
                "Preview represents the module state.",
                centerX,
                top + 25,
                MUTED
        );
    }

    private static String trimPreviewTextToWidth(
            class_310 client,
            String text,
            int maxWidth
    ) {
        if (text == null || text.isEmpty() || maxWidth <= 0) return "";
        if (client.field_1772.method_1727(text) <= maxWidth) return text;
        String ellipsis = "…";
        int ellipsisWidth = client.field_1772.method_1727(ellipsis);
        if (ellipsisWidth >= maxWidth) return client.field_1772.method_27523(text, maxWidth);
        String body = client.field_1772.method_27523(text, maxWidth - ellipsisWidth);
        return body.isEmpty() ? ellipsis : body + ellipsis;
    }

    private static void renderGenericPreview(
            class_332 context,
            class_310 client,
            Module module,
            int x,
            int y,
            int width,
            int height
    ) {
        int centerX =
                x + width / 2;

        int centerY =
                y + height / 2;

        context.method_25300(
                client.field_1772,
                module.isEnabled()
                        ? "ENABLED"
                        : "DISABLED",
                centerX,
                centerY - 15,
                module.isEnabled()
                        ? 0xFF74E08C
                        : MUTED
        );

        context.method_25300(
                client.field_1772,
                "This effect applies live in-game.",
                centerX,
                centerY + 6,
                TEXT
        );
    }

    private static void drawHudCard(
            class_332 context,
            StyledHudModule module,
            int x,
            int y,
            int width,
            int height
    ) {
        drawHudCard(context, module, x, y, width, height, true);
    }

    private static void drawHudCard(
            class_332 context,
            StyledHudModule module,
            int x,
            int y,
            int width,
            int height,
            boolean renderBackground
    ) {
        if (
                renderBackground
                        && module
                        .getBackground()
                        .get()
        ) {
            context.method_25294(
                    x,
                    y,
                    x + width,
                    y + height,
                    withOpacity(
                            module
                                    .getBackgroundColor()
                                    .getArgb(),
                            module
                                    .getBackgroundOpacity()
                                    .get()
                                    .floatValue()
                    )
            );
        }

        if (
                module
                        .getBorder()
                        .get()
        ) {
            outline(
                    context,
                    x,
                    y,
                    width,
                    height,
                    module
                            .getBorderColor()
                            .getArgb()
            );
        }

        if (
                module
                        .getAccentLine()
                        .get()
        ) {
            context.method_25294(
                    x,
                    y,
                    x + 2,
                    y + height,
                    module
                            .getAccentColor()
                            .getArgb()
            );
        }
    }

    private static String previewHudLabel(
            StyledHudModule module,
            String label
    ) {
        if (
                module
                        .getShowColon()
                        .get()
                &&
                !label.endsWith(
                        ":"
                )
        ) {
            return label + ":";
        }

        return label;
    }

    private static String sampleLabel(
            StyledHudModule module
    ) {
        if (module instanceof FpsHudModule) {
            return "FPS";
        }

        if (module instanceof PingHudModule) {
            return "PING";
        }

        if (module instanceof CoordinatesHudModule) {
            return "XYZ";
        }

        if (module instanceof ArmorHudModule) {
            return "ARMOR";
        }

        if (module instanceof TotemHudModule) {
            return "TOTEMS";
        }

        if (module instanceof ShieldStatusHudModule) {
            return "SHIELD";
        }

        if (module instanceof AttackCooldownHudModule) {
            return "ATTACK";
        }

        if (module instanceof PotionEffectsHudModule) {
            return "EFFECTS";
        }

        return module.getName();
    }

    private static String sampleValue(
            StyledHudModule module
    ) {
        if (module instanceof FpsHudModule) {
            return "983";
        }

        if (module instanceof PingHudModule) {
            return "45 ms";
        }

        if (module instanceof CoordinatesHudModule) {
            return "4355  94  6315";
        }

        if (module instanceof TotemHudModule) {
            return "7";
        }

        if (module instanceof ShieldStatusHudModule) {
            return "READY";
        }

        if (module instanceof AttackCooldownHudModule) {
            return "76%";
        }

        return "";
    }

    private static String sampleWarning(
            WarningModule module
    ) {
        String name =
                module.getName();

        if (
                name.contains(
                        "Armor"
                )
        ) {
            return "CHESTPLATE LOW — 12%";
        }

        if (
                name.contains(
                        "Weapon"
                )
        ) {
            return "HELD ITEM LOW — 8%";
        }

        if (
                name.contains(
                        "Totem"
                )
        ) {
            return "LOW TOTEMS — 2";
        }

        if (
                name.contains(
                        "Health"
                )
        ) {
            return "LOW HEALTH — 3.0 ❤";
        }

        if (
                name.contains(
                        "Effect"
                )
        ) {
            return "STRENGTH EXPIRES IN 6s";
        }

        if (
                name.contains(
                        "Food"
                )
        ) {
            return "FOOD 5 • SAT 1.5";
        }

        return "WARNING";
    }

    private static boolean isPerformanceModule(
            Module module
    ) {
        return module
                        instanceof BorderlessFullscreenModule
                ||
                module
                        instanceof MinimalParticlesModule
                ||
                module
                        instanceof NoCloudsModule
                ||
                module
                        instanceof NoEntityShadowsModule
                ||
                module
                        instanceof LowEntityDistanceModule
                ||
                module
                        instanceof NoBiomeBlendModule
                ||
                module
                        instanceof NoAmbientOcclusionModule
                ||
                module
                        instanceof NoVignetteModule
                ||
                module
                        instanceof NoChunkFadeModule;
    }

    private static int withOpacity(
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

    private static int resolvePreviewValueX(
            class_310 client,
            String label,
            int labelX,
            int labelY,
            int valueX,
            int valueY,
            int valueWidth,
            boolean bothVisible
    ) {
        if (!bothVisible || label == null || label.isEmpty()) {
            return valueX;
        }

        int fontHeight = client.field_1772.field_2000;
        boolean verticalOverlap = labelY < valueY + fontHeight
                && valueY < labelY + fontHeight;
        if (!verticalOverlap) {
            return valueX;
        }

        int labelRight = labelX + client.field_1772.method_1727(label);
        int valueRight = valueX + valueWidth;
        if (valueRight <= labelX - 3 || valueX >= labelRight + 3) {
            return valueX;
        }

        
        
        
        int labelCenter = (labelX + labelRight) / 2;
        int valueCenter = valueX + valueWidth / 2;
        return valueCenter < labelCenter
                ? labelX - 3 - valueWidth
                : labelRight + 3;
    }

    private static void outline(
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
    private static void drawPreviewText(
            class_332 context,
            net.minecraft.class_327 renderer,
            String text, int x, int y, int color, boolean shadow
    ) {
        if (shadow) {
            int shadowColor = 0xB0000000;
            context.method_51433(renderer, text, x + 1, y + 1, shadowColor, false);
        }
        context.method_51433(renderer, text, x, y, color, false);
    }

}
