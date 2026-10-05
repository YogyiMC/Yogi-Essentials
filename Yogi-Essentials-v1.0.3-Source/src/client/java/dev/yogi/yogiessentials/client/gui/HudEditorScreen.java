package dev.yogi.yogiessentials.client.gui;

import dev.yogi.yogiessentials.client.YogiEssentialsClient;
import dev.yogi.yogiessentials.client.config.ConfigManager;
import dev.yogi.yogiessentials.client.hud.HudManager;
import dev.yogi.yogiessentials.client.module.Module;
import dev.yogi.yogiessentials.client.module.hud.ArmorHudModule;
import dev.yogi.yogiessentials.client.module.hud.HudModule;
import dev.yogi.yogiessentials.client.module.hud.PotionEffectsHudModule;
import dev.yogi.yogiessentials.client.module.hud.StyledHudModule;
import dev.yogi.yogiessentials.client.module.pvp.WarningModule;
import dev.yogi.yogiessentials.client.setting.NumberSetting;
import dev.yogi.yogiessentials.client.util.UiSoundManager;
import net.minecraft.class_11909;
import net.minecraft.class_2561;
import net.minecraft.class_310;
import net.minecraft.class_332;
import net.minecraft.class_437;

public class HudEditorScreen extends class_437 {

    private static final int ORANGE = 0xFFFF6A00;
    private static final int TEXT = 0xFFF4F4F5;
    private static final int MUTED = 0xFFAAAAAF;
    private static final int SNAP_DISTANCE = 7;
    private static final int CLOSE_SIZE = 11;
    private static final int EDGE_HIT = 6;
    private static final int MIN_SIZE = 20;

    private enum ResizeMode {
        NONE, N, S, E, W, NE, NW, SE, SW
    }

    private Module dragging;
    private Module clickedModule;
    private Module resizing;
    private ResizeMode resizeMode = ResizeMode.NONE;

    private ArmorHudModule armorPieceDragging;
    private int armorPieceIndex = -1;
    private double armorPieceGrabX;
    private double armorPieceGrabY;
    private ResizeMode armorPieceResizeMode = ResizeMode.NONE;
    private int armorPieceResizeStartSize;
    private double armorPieceResizeStartScale;
    private int armorPieceResizeStartX;
    private int armorPieceResizeStartY;

    private double dragOffsetX;
    private double dragOffsetY;
    private double clickStartX;
    private double clickStartY;
    private boolean pointerMoved;

    private int resizeStartX;
    private int resizeStartY;
    private int resizeStartWidth;
    private int resizeStartHeight;
    private double resizeStartScale = 1.0;

    private int guideX = -1;
    private int guideY = -1;
    private String status = "";

    public HudEditorScreen() {
        super(class_2561.method_43470("Yogi Essentials HUD Editor"));
    }

    @Override
    public void method_25394(class_332 context, int mouseX, int mouseY, float deltaTicks) {
        HudManager.renderEditorHud(context);

        if ((dragging != null || armorPieceDragging != null) && guideX >= 0) {
            context.method_25294(guideX, 0, guideX + 1, field_22790, 0x99FF6A00);
        }
        if ((dragging != null || armorPieceDragging != null) && guideY >= 0) {
            context.method_25294(0, guideY, field_22789, guideY + 1, 0x99FF6A00);
        }

        renderEditorBoxes(context, mouseX, mouseY);
        renderSettingsButton(context, mouseX, mouseY);

        if ((!status.isBlank()) && (dragging != null || resizing != null || armorPieceDragging != null)) {
            context.method_25300(field_22793, status, field_22789 / 2, 18, ORANGE);
        }

        String help = field_22793.method_27523(
                "Click = settings  •  Drag = move  •  Edge = resize box  •  Corner = scale all  •  Armor/items = drag + resize  •  x = disable",
                Math.max(8, field_22789 - 12)
        );
        context.method_25300(
                field_22793,
                help,
                field_22789 / 2,
                field_22790 - 18,
                MUTED
        );
    }

    private void renderEditorBoxes(class_332 context, int mouseX, int mouseY) {
        for (Module module : YogiEssentialsClient.getModuleManager().getModules()) {
            if (!isEditorElement(module)) continue;

            if (module instanceof ArmorHudModule armor
                    && armor.getIndividualArmorPositions().get()
                    && armor.isIconMode()) {
                armor.enforceIndividualModeStyle();
                renderArmorPieceHandles(context, armor, mouseX, mouseY);
                continue;
            }

            int boxWidth = HudManager.getEditorRenderedWidth(module);
            int boxHeight = HudManager.getEditorRenderedHeight(module);
            int x = HudManager.getEditorPixelX(module, field_22789);
            int y = HudManager.getEditorPixelY(module, field_22790);

            boolean hovered = inside(mouseX, mouseY, x, y, boxWidth, boxHeight);
            boolean active = module == dragging || module == resizing;
            int fill = active ? 0xD82B1E16 : hovered ? 0xBA24242B : 0x8518181D;

            context.method_25294(x, y, x + boxWidth, y + boxHeight, fill);
            outline(context, x, y, boxWidth, boxHeight, active ? ORANGE : hovered ? 0xCCAAAAAF : 0x88888890);

            if (module instanceof StyledHudModule styled
                    && styled.hasCustomEditorSize()) {
                int contentX = HudManager.getEditorContentPixelX(
                        module,
                        field_22789
                );
                int contentY = HudManager.getEditorContentPixelY(
                        module,
                        field_22790
                );
                int contentWidth = HudManager.getEditorContentRenderedWidth(
                        module
                );
                int contentHeight = HudManager.getEditorContentRenderedHeight(
                        module
                );

                if (contentX != x
                        || contentY != y
                        || contentWidth != boxWidth
                        || contentHeight != boxHeight) {
                    outline(
                            context,
                            contentX,
                            contentY,
                            contentWidth,
                            contentHeight,
                            0x996ED0FF
                    );
                }
            }

            int titleRight = x + boxWidth - CLOSE_SIZE - 7;
            int titleAvailable = Math.max(
                    0,
                    titleRight - (x + 4)
            );
            boolean titleInside = boxHeight >= 15
                    && titleAvailable >= 18;

            if (titleInside) {
                String title = field_22793.method_27523(
                        module.getName(),
                        titleAvailable
                );
                context.method_25303(
                        field_22793,
                        title,
                        x + 4,
                        y + 4,
                        ORANGE
                );
            } else {
                
                
                String title = field_22793.method_27523(
                        module.getName(),
                        Math.max(8, field_22789 - 8)
                );
                int titleWidth = field_22793.method_1727(title);
                int titleX = Math.max(
                        2,
                        Math.min(
                                Math.max(2, field_22789 - titleWidth - 2),
                                x
                        )
                );
                int titleY = y >= field_22793.field_2000 + 4
                        ? y - field_22793.field_2000 - 2
                        : Math.min(
                        Math.max(0, field_22790 - field_22793.field_2000 - 1),
                        y + boxHeight + 2
                );

                context.method_25303(
                        field_22793,
                        title,
                        titleX,
                        titleY,
                        ORANGE
                );
            }

            renderCloseButton(context, x, y, boxWidth, insideClose(mouseX, mouseY, x, y, boxWidth));

            if (canContainerResize(module)) {
                renderResizeEdges(context, x, y, boxWidth, boxHeight, detectResizeMode(mouseX, mouseY, x, y, boxWidth, boxHeight));
            } else if (module instanceof WarningModule) {
                renderLegacyScaleHandle(context, x, y, boxWidth, boxHeight,
                        inside(mouseX, mouseY, x + boxWidth - 11, y + boxHeight - 11, 13, 13));
            }

        }
    }

    private void renderArmorPieceHandles(class_332 context, ArmorHudModule armor, int mouseX, int mouseY) {
        for (int i = 0; i < armor.getConfiguredItemCount(); i++) {
            int x = HudManager.getArmorPieceEditorX(armor, i, field_22789);
            int y = HudManager.getArmorPieceEditorY(armor, i, field_22790);
            int size = HudManager.getArmorPieceEditorSize(armor, i);
            boolean hovered = inside(mouseX, mouseY, x, y, size, size);
            boolean active = armorPieceDragging == armor && armorPieceIndex == i;
            outline(context, x - 1, y - 1, size + 2, size + 2, active || hovered ? ORANGE : 0x779999A0);
            renderResizeEdges(context, x, y, size, size,
                    detectResizeMode(mouseX, mouseY, x, y, size, size));
        }
    }

    private boolean isEditorElement(Module module) {
        if (module instanceof HudModule) return module.isEnabled();
        return HudManager.isCustomWarningEditorElement(module);
    }

    private boolean canContainerResize(Module module) {
        return module instanceof StyledHudModule;
    }

    private NumberSetting getScaleSetting(Module module) {
        if (module instanceof StyledHudModule styled) return styled.getScale();
        if (module instanceof WarningModule warning) return warning.getScale();
        return null;
    }

    private void renderCloseButton(class_332 context, int x, int y, int boxWidth, boolean hovered) {
        int closeX = x + boxWidth - CLOSE_SIZE - 3;
        int closeY = y + 3;
        context.method_25294(closeX, closeY, closeX + CLOSE_SIZE, closeY + CLOSE_SIZE, hovered ? 0xE6B93434 : 0xAA1B1B20);
        context.method_25300(field_22793, "x", closeX + CLOSE_SIZE / 2, closeY + 2, hovered ? 0xFFFFFFFF : MUTED);
    }

    private void renderResizeEdges(class_332 context, int x, int y, int boxWidth, int boxHeight, ResizeMode hovered) {
        int normal = 0xAA777780;
        int hot = ORANGE;
        int h = hovered == ResizeMode.N || hovered == ResizeMode.NE || hovered == ResizeMode.NW ? hot : normal;
        int b = hovered == ResizeMode.S || hovered == ResizeMode.SE || hovered == ResizeMode.SW ? hot : normal;
        int l = hovered == ResizeMode.W || hovered == ResizeMode.NW || hovered == ResizeMode.SW ? hot : normal;
        int r = hovered == ResizeMode.E || hovered == ResizeMode.NE || hovered == ResizeMode.SE ? hot : normal;

        context.method_25294(x + 8, y, x + Math.max(9, boxWidth - 8), y + 2, h);
        context.method_25294(x + 8, y + boxHeight - 2, x + Math.max(9, boxWidth - 8), y + boxHeight, b);
        context.method_25294(x, y + 8, x + 2, y + Math.max(9, boxHeight - 8), l);
        context.method_25294(x + boxWidth - 2, y + 8, x + boxWidth, y + Math.max(9, boxHeight - 8), r);

        int corner = hovered == ResizeMode.SE || hovered == ResizeMode.SW || hovered == ResizeMode.NE || hovered == ResizeMode.NW ? hot : normal;
        context.method_25294(x, y, x + 4, y + 4, hovered == ResizeMode.NW ? hot : normal);
        context.method_25294(x + boxWidth - 4, y, x + boxWidth, y + 4, hovered == ResizeMode.NE ? hot : normal);
        context.method_25294(x, y + boxHeight - 4, x + 4, y + boxHeight, hovered == ResizeMode.SW ? hot : normal);
        context.method_25294(x + boxWidth - 4, y + boxHeight - 4, x + boxWidth, y + boxHeight, hovered == ResizeMode.SE ? hot : corner);
    }

    private void renderLegacyScaleHandle(class_332 context, int x, int y, int boxWidth, int boxHeight, boolean hovered) {
        int color = hovered || resizing != null ? ORANGE : 0xFF8B8B94;
        context.method_25294(x + boxWidth - 6, y + boxHeight - 2, x + boxWidth, y + boxHeight, color);
        context.method_25294(x + boxWidth - 3, y + boxHeight - 6, x + boxWidth, y + boxHeight, color);
    }

    private void renderSettingsButton(class_332 context, int mouseX, int mouseY) {
        int buttonWidth = Math.min(156, Math.max(110, field_22789 - 36));
        int buttonHeight = 36;
        int buttonX = (field_22789 - buttonWidth) / 2;
        int buttonY = (field_22790 - buttonHeight) / 2;
        boolean hovered = inside(mouseX, mouseY, buttonX, buttonY, buttonWidth, buttonHeight);

        context.method_25294(buttonX, buttonY, buttonX + buttonWidth, buttonY + buttonHeight, hovered ? 0xF52C1B12 : 0xED151519);
        outline(context, buttonX, buttonY, buttonWidth, buttonHeight, ORANGE);
        context.method_25294(buttonX, buttonY, buttonX + 3, buttonY + buttonHeight, ORANGE);
        context.method_25300(field_22793, "Open Settings", field_22789 / 2, buttonY + 14, TEXT);
        context.method_25300(field_22793, "HUD EDITOR", field_22789 / 2, buttonY + buttonHeight + 12, ORANGE);
        context.method_25300(field_22793, "Resize from any edge", field_22789 / 2, buttonY + buttonHeight + 27, MUTED);
    }

    @Override
    public boolean method_25402(class_11909 click, boolean doubled) {
        if (click.method_74245() != 0) return super.method_25402(click, doubled);

        int buttonWidth = Math.min(156, Math.max(110, field_22789 - 36));
        int buttonHeight = 36;
        int buttonX = (field_22789 - buttonWidth) / 2;
        int buttonY = (field_22790 - buttonHeight) / 2;
        if (inside(click.comp_4798(), click.comp_4799(), buttonX, buttonY, buttonWidth, buttonHeight)) {
            UiSoundManager.click();
            class_310.method_1551().method_1507(new YogiEssentialsScreen());
            return true;
        }

        for (Module module : YogiEssentialsClient.getModuleManager().getModules()) {
            if (!isEditorElement(module)) continue;

            if (module instanceof ArmorHudModule armor
                    && armor.getIndividualArmorPositions().get()
                    && armor.isIconMode()) {
                armor.enforceIndividualModeStyle();
                for (int i = 0; i < armor.getConfiguredItemCount(); i++) {
                    int pieceX = HudManager.getArmorPieceEditorX(armor, i, field_22789);
                    int pieceY = HudManager.getArmorPieceEditorY(armor, i, field_22790);
                    int size = HudManager.getArmorPieceEditorSize(armor, i);
                    ResizeMode pieceResize = detectResizeMode(click.comp_4798(), click.comp_4799(), pieceX, pieceY, size, size);
                    if (pieceResize != ResizeMode.NONE || inside(click.comp_4798(), click.comp_4799(), pieceX, pieceY, size, size)) {
                        armorPieceDragging = armor;
                        armorPieceIndex = i;
                        armorPieceResizeMode = pieceResize;
                        armorPieceResizeStartSize = size;
                        armorPieceResizeStartScale = armor.getPieceScale(i).get();
                        armorPieceResizeStartX = pieceX;
                        armorPieceResizeStartY = pieceY;
                        armorPieceGrabX = click.comp_4798() - pieceX;
                        armorPieceGrabY = click.comp_4799() - pieceY;
                        clickStartX = click.comp_4798();
                        clickStartY = click.comp_4799();
                        pointerMoved = false;
                        dragging = null;
                        resizing = null;
                        clickedModule = null;
                        clearGuide();
                        status = armorPieceName(i) + (pieceResize == ResizeMode.NONE ? " POSITION" : " RESIZE");
                        return true;
                    }
                }
                
                continue;
            }

            int boxWidth = HudManager.getEditorRenderedWidth(module);
            int boxHeight = HudManager.getEditorRenderedHeight(module);
            int x = HudManager.getEditorPixelX(module, field_22789);
            int y = HudManager.getEditorPixelY(module, field_22790);

            if (insideClose(click.comp_4798(), click.comp_4799(), x, y, boxWidth)) {
                module.setEnabled(false);
                ConfigManager.save();
                UiSoundManager.click();
                return true;
            }

            if (module instanceof StyledHudModule) {
                ResizeMode mode = detectResizeMode(click.comp_4798(), click.comp_4799(), x, y, boxWidth, boxHeight);
                if (mode != ResizeMode.NONE) {
                    beginResize(module, mode, x, y, boxWidth, boxHeight, click.comp_4798(), click.comp_4799());
                    return true;
                }
            } else if (module instanceof WarningModule
                    && inside(click.comp_4798(), click.comp_4799(), x + boxWidth - 11, y + boxHeight - 11, 13, 13)) {
                beginResize(module, ResizeMode.SE, x, y, boxWidth, boxHeight, click.comp_4798(), click.comp_4799());
                return true;
            }


            if (inside(click.comp_4798(), click.comp_4799(), x, y, boxWidth, boxHeight)) {
                dragging = module;
                clickedModule = module;
                resizing = null;
                armorPieceDragging = null;
                armorPieceIndex = -1;
                dragOffsetX = click.comp_4798() - x;
                dragOffsetY = click.comp_4799() - y;
                clickStartX = click.comp_4798();
                clickStartY = click.comp_4799();
                pointerMoved = false;
                clearGuide();
                return true;
            }
        }

        return super.method_25402(click, doubled);
    }

    private void beginResize(Module module, ResizeMode mode, int x, int y, int w, int h, double mouseX, double mouseY) {
        resizing = module;
        resizeMode = mode;
        dragging = null;
        clickedModule = null;
        armorPieceDragging = null;
        armorPieceIndex = -1;
        armorPieceResizeMode = ResizeMode.NONE;
        resizeStartX = x;
        resizeStartY = y;
        resizeStartWidth = w;
        resizeStartHeight = h;
        NumberSetting scaleSetting = getScaleSetting(module);
        resizeStartScale = scaleSetting == null ? 1.0 : Math.max(0.001, scaleSetting.get());
        clickStartX = mouseX;
        clickStartY = mouseY;
        pointerMoved = false;
        clearGuide();
        status = "RESIZE";
    }

    @Override
    public boolean method_25403(class_11909 click, double offsetX, double offsetY) {
        if (click.method_74245() != 0) return super.method_25403(click, offsetX, offsetY);

        if (armorPieceDragging != null && armorPieceIndex >= 0) {
            markPointerMoved(click.comp_4798(), click.comp_4799());
            if (armorPieceResizeMode != ResizeMode.NONE) {
                double dx = click.comp_4798() - clickStartX;
                double dy = click.comp_4799() - clickStartY;
                double delta = resizeSignedDelta(armorPieceResizeMode, dx, dy);

                
                
                
                double desiredSize = Math.max(6.0, armorPieceResizeStartSize + delta);
                double factor = desiredSize / Math.max(1.0, armorPieceResizeStartSize);
                armorPieceDragging.getPieceScale(armorPieceIndex).set(armorPieceResizeStartScale * factor);

                int newSize = HudManager.getArmorPieceEditorSize(armorPieceDragging, armorPieceIndex);
                int targetX = movesWest(armorPieceResizeMode)
                        ? armorPieceResizeStartX + armorPieceResizeStartSize - newSize
                        : armorPieceResizeStartX;
                int targetY = movesNorth(armorPieceResizeMode)
                        ? armorPieceResizeStartY + armorPieceResizeStartSize - newSize
                        : armorPieceResizeStartY;

                targetX = (int) clamp(targetX, 0, Math.max(0, field_22789 - newSize));
                targetY = (int) clamp(targetY, 0, Math.max(0, field_22790 - newSize));
                HudManager.setArmorPieceEditorPixelPosition(
                        armorPieceDragging,
                        armorPieceIndex,
                        targetX,
                        targetY,
                        field_22789,
                        field_22790
                );

                status = armorPieceName(armorPieceIndex) + " SCALE "
                        + Math.round(armorPieceDragging.getPieceScale(armorPieceIndex).get() * 100.0) + "%";
                return true;
            }
            int desiredX = (int) Math.round(click.comp_4798() - armorPieceGrabX);
            int desiredY = (int) Math.round(click.comp_4799() - armorPieceGrabY);
            int pieceSize = HudManager.getArmorPieceEditorSize(armorPieceDragging, armorPieceIndex);
            desiredX = (int) clamp(desiredX, 0, Math.max(0, field_22789 - pieceSize));
            desiredY = (int) clamp(desiredY, 0, Math.max(0, field_22790 - pieceSize));
            clearGuide();
            double[] snapped = snapArmorPiece(desiredX, desiredY, pieceSize);
            desiredX = (int) clamp(Math.round(snapped[0]), 0, Math.max(0, field_22789 - pieceSize));
            desiredY = (int) clamp(Math.round(snapped[1]), 0, Math.max(0, field_22790 - pieceSize));
            HudManager.setArmorPieceEditorPixelPosition(
                    armorPieceDragging,
                    armorPieceIndex,
                    desiredX,
                    desiredY,
                    field_22789,
                    field_22790
            );
            status = armorPieceName(armorPieceIndex)
                    + "  X " + armorPieceDragging.getPieceOffsetX(armorPieceIndex).get().intValue()
                    + "  Y " + armorPieceDragging.getPieceOffsetY(armorPieceIndex).get().intValue();
            return true;
        }

        if (resizing != null) {
            markPointerMoved(click.comp_4798(), click.comp_4799());
            if (resizing instanceof StyledHudModule styled) {
                resizeStyledHud(styled, click.comp_4798(), click.comp_4799());
            } else {
                resizeLegacyScale(click.comp_4798(), click.comp_4799());
            }
            return true;
        }

        if (dragging == null) return super.method_25403(click, offsetX, offsetY);

        markPointerMoved(click.comp_4798(), click.comp_4799());
        int boxWidth = HudManager.getEditorRenderedWidth(dragging);
        int boxHeight = HudManager.getEditorRenderedHeight(dragging);
        int maxX = Math.max(0, field_22789 - boxWidth);
        int maxY = Math.max(0, field_22790 - boxHeight);
        double x = clamp(click.comp_4798() - dragOffsetX, 0, maxX);
        double y = clamp(click.comp_4799() - dragOffsetY, 0, maxY);

        clearGuide();
        if (Math.abs(x) <= SNAP_DISTANCE) { x = 0; guideX = 0; status = "LEFT EDGE"; }
        if (Math.abs(x - maxX) <= SNAP_DISTANCE) { x = maxX; guideX = field_22789 - 1; status = "RIGHT EDGE"; }
        if (Math.abs(y) <= SNAP_DISTANCE) { y = 0; guideY = 0; status = "TOP EDGE"; }
        if (Math.abs(y - maxY) <= SNAP_DISTANCE) { y = maxY; guideY = field_22790 - 1; status = "BOTTOM EDGE"; }

        double centerX = maxX / 2.0;
        double centerY = maxY / 2.0;
        if (Math.abs(x - centerX) <= SNAP_DISTANCE) { x = centerX; guideX = field_22789 / 2; status = "CENTER X"; }
        if (Math.abs(y - centerY) <= SNAP_DISTANCE) { y = centerY; guideY = field_22790 / 2; status = guideX == field_22789 / 2 ? "CENTERED" : "CENTER Y"; }

        double[] resolved = snapToEditorElement(x, y, boxWidth, boxHeight);
        x = clamp(resolved[0], 0, maxX);
        y = clamp(resolved[1], 0, maxY);
        HudManager.setEditorPixelPosition(dragging, (int) Math.round(x), (int) Math.round(y), field_22789, field_22790);
        return true;
    }

    private void resizeStyledHud(StyledHudModule styled, double mouseX, double mouseY) {
        
        
        
        if (isCorner(resizeMode)) {
            resizeUniformScale(styled, mouseX, mouseY);
            return;
        }

        resizeContainerBox(styled, mouseX, mouseY);
    }

    







    private void resizeContainerBox(StyledHudModule styled, double mouseX, double mouseY) {
        double dx = mouseX - clickStartX;
        double dy = mouseY - clickStartY;

        int startLeft = resizeStartX;
        int startTop = resizeStartY;
        int startRight = resizeStartX + resizeStartWidth;
        int startBottom = resizeStartY + resizeStartHeight;

        int left = startLeft;
        int top = startTop;
        int right = startRight;
        int bottom = startBottom;

        if (movesWest(resizeMode)) {
            left = (int) Math.round(clamp(startLeft + dx, 0, startRight - MIN_SIZE));
        } else if (movesEast(resizeMode)) {
            right = (int) Math.round(clamp(startRight + dx, startLeft + MIN_SIZE, field_22789));
        }

        if (movesNorth(resizeMode)) {
            top = (int) Math.round(clamp(startTop + dy, 0, startBottom - MIN_SIZE));
        } else if (movesSouth(resizeMode)) {
            bottom = (int) Math.round(clamp(startBottom + dy, startTop + MIN_SIZE, field_22790));
        }

        
        
        
        
        if (!HudManager.usesContainerResize(styled)) {
            int contentLeft = HudManager.getEditorContentPixelX(styled, field_22789);
            int contentTop = HudManager.getEditorContentPixelY(styled, field_22790);
            int contentRight = contentLeft + HudManager.getEditorContentRenderedWidth(styled);
            int contentBottom = contentTop + HudManager.getEditorContentRenderedHeight(styled);

            if (movesWest(resizeMode)) {
                left = Math.min(left, contentLeft);
            } else if (movesEast(resizeMode)) {
                right = Math.max(right, contentRight);
            }

            if (movesNorth(resizeMode)) {
                top = Math.min(top, contentTop);
            } else if (movesSouth(resizeMode)) {
                bottom = Math.max(bottom, contentBottom);
            }

            left = (int) clamp(left, 0, Math.max(0, right - MIN_SIZE));
            top = (int) clamp(top, 0, Math.max(0, bottom - MIN_SIZE));
            right = (int) clamp(right, Math.min(field_22789, left + MIN_SIZE), field_22789);
            bottom = (int) clamp(bottom, Math.min(field_22790, top + MIN_SIZE), field_22790);
        }

        int renderedW = Math.max(MIN_SIZE, right - left);
        int renderedH = Math.max(MIN_SIZE, bottom - top);
        HudManager.setEditorBoxBounds(
                styled, left, top, renderedW, renderedH, field_22789, field_22790
        );

        String orientation = "";
        if (styled instanceof ArmorHudModule armor && armor.getAutoLayout().get()) {
            orientation = HudManager.resolveArmorHorizontal(armor) ? "  •  HORIZONTAL" : "  •  VERTICAL";
        } else if (styled instanceof PotionEffectsHudModule potion
                && potion.getDisplayMode().get() == PotionEffectsHudModule.DisplayMode.ICONS
                && potion.getAutoLayout().get()) {
            orientation = HudManager.resolvePotionIconsHorizontal(potion) ? "  •  HORIZONTAL" : "  •  VERTICAL";
        }

        status = "BOX " + renderedW + " × " + renderedH + orientation;
    }

    





    private void resizeUniformScale(StyledHudModule styled, double mouseX, double mouseY) {
        double dx = mouseX - clickStartX;
        double dy = mouseY - clickStartY;

        int left = resizeStartX;
        int top = resizeStartY;
        int right = resizeStartX + resizeStartWidth;
        int bottom = resizeStartY + resizeStartHeight;

        NumberSetting scaleSetting = styled.getScale();

        
        
        
        
        
        double signedDelta = resizeSignedDelta(resizeMode, dx, dy);

        
        
        
        
        double referenceSpan = Math.max(80.0, Math.max(resizeStartWidth, resizeStartHeight));
        double factor = Math.max(0.01, 1.0 + signedDelta / referenceSpan);
        scaleSetting.set(resizeStartScale * factor);

        
        
        int nw = HudManager.getEditorRenderedWidth(styled);
        int nh = HudManager.getEditorRenderedHeight(styled);
        int targetLeft = movesWest(resizeMode) ? right - nw : left;
        int targetTop = movesNorth(resizeMode) ? bottom - nh : top;
        targetLeft = (int) clamp(targetLeft, 0, Math.max(0, field_22789 - nw));
        targetTop = (int) clamp(targetTop, 0, Math.max(0, field_22790 - nh));
        HudManager.setEditorPixelPosition(styled, targetLeft, targetTop, field_22789, field_22790);

        status = "SCALE " + Math.round(scaleSetting.get() * 100.0) + "%";
    }

    private void resizeLegacyScale(double mouseX, double mouseY) {
        NumberSetting scaleSetting = getScaleSetting(resizing);
        if (scaleSetting == null) return;

        double dx = mouseX - clickStartX;
        double dy = mouseY - clickStartY;
        double signedDelta = resizeSignedDelta(resizeMode, dx, dy);
        double referenceSpan = Math.max(80.0, Math.max(resizeStartWidth, resizeStartHeight));
        double factor = Math.max(0.01, 1.0 + signedDelta / referenceSpan);
        scaleSetting.set(resizeStartScale * factor);
        status = "SCALE " + Math.round(scaleSetting.get() * 100.0) + "%";
    }

    private static boolean isCorner(ResizeMode mode) {
        return mode == ResizeMode.NW
                || mode == ResizeMode.NE
                || mode == ResizeMode.SW
                || mode == ResizeMode.SE;
    }

    




    private static double resizeSignedDelta(ResizeMode mode, double dx, double dy) {
        return switch (mode) {
            case E -> dx;
            case W -> -dx;
            case S -> dy;
            case N -> -dy;
            case SE -> (dx + dy) * 0.5;
            case SW -> (-dx + dy) * 0.5;
            case NE -> (dx - dy) * 0.5;
            case NW -> (-dx - dy) * 0.5;
            default -> 0.0;
        };
    }

    private static boolean movesWest(ResizeMode mode) {
        return mode == ResizeMode.W || mode == ResizeMode.NW || mode == ResizeMode.SW;
    }

    private static boolean movesEast(ResizeMode mode) {
        return mode == ResizeMode.E || mode == ResizeMode.NE || mode == ResizeMode.SE;
    }

    private static boolean movesNorth(ResizeMode mode) {
        return mode == ResizeMode.N || mode == ResizeMode.NW || mode == ResizeMode.NE;
    }

    private static boolean movesSouth(ResizeMode mode) {
        return mode == ResizeMode.S || mode == ResizeMode.SW || mode == ResizeMode.SE;
    }

    private void markPointerMoved(double x, double y) {
        if (Math.abs(x - clickStartX) > 2 || Math.abs(y - clickStartY) > 2) pointerMoved = true;
    }

    @Override
    public boolean method_25406(class_11909 click) {
        if (click.method_74245() == 0) {
            Module released = clickedModule;
            boolean openSettings = released != null && dragging == released && !pointerMoved;

            if (dragging != null || resizing != null || armorPieceDragging != null) ConfigManager.save();

            dragging = null;
            resizing = null;
            resizeMode = ResizeMode.NONE;
            clickedModule = null;
            armorPieceDragging = null;
            armorPieceIndex = -1;
            armorPieceResizeMode = ResizeMode.NONE;
            pointerMoved = false;
            clearGuide();

            if (openSettings && field_22787 != null) {
                UiSoundManager.click();
                field_22787.method_1507(new ModuleSettingsScreen(this, released));
                return true;
            }
        }
        return super.method_25406(click);
    }

    private double[] snapToEditorElement(double x, double y, int boxWidth, int boxHeight) {
        double cx = x + boxWidth / 2.0;
        double cy = y + boxHeight / 2.0;

        for (Module module : YogiEssentialsClient.getModuleManager().getModules()) {
            if (module == dragging || !isEditorElement(module)) continue;
            if (module instanceof ArmorHudModule armor
                    && armor.getIndividualArmorPositions().get()
                    && armor.isIconMode()) {
                
                
                for (int i = 0; i < armor.getConfiguredItemCount(); i++) {
                    int ox = HudManager.getArmorPieceEditorX(armor, i, field_22789);
                    int oy = HudManager.getArmorPieceEditorY(armor, i, field_22790);
                    int os = HudManager.getArmorPieceEditorSize(armor, i);
                    double ocx = ox + os / 2.0;
                    double ocy = oy + os / 2.0;
                    if (Math.abs(cx - ocx) <= SNAP_DISTANCE) {
                        x = ocx - boxWidth / 2.0; cx = ocx; guideX = (int) Math.round(ocx); status = "HUD CENTER X";
                    }
                    if (Math.abs(cy - ocy) <= SNAP_DISTANCE) {
                        y = ocy - boxHeight / 2.0; cy = ocy; guideY = (int) Math.round(ocy); status = guideX >= 0 ? "HUD CENTERED" : "HUD CENTER Y";
                    }
                }
                continue;
            }

            int otherWidth = HudManager.getEditorRenderedWidth(module);
            int otherHeight = HudManager.getEditorRenderedHeight(module);
            int ox = HudManager.getEditorPixelX(module, field_22789);
            int oy = HudManager.getEditorPixelY(module, field_22790);
            int otherRight = ox + otherWidth;
            int otherBottom = oy + otherHeight;
            double ocx = ox + otherWidth / 2.0;
            double ocy = oy + otherHeight / 2.0;
            double right = x + boxWidth;
            double bottom = y + boxHeight;
            boolean verticalOverlap = bottom >= oy && y <= otherBottom;
            boolean horizontalOverlap = right >= ox && x <= otherRight;

            if (verticalOverlap && Math.abs(right - ox) <= SNAP_DISTANCE) { x = ox - boxWidth; guideX = ox; status = "HUD EDGE"; }
            if (verticalOverlap && Math.abs(x - otherRight) <= SNAP_DISTANCE) { x = otherRight; guideX = otherRight; status = "HUD EDGE"; }
            if (horizontalOverlap && Math.abs(bottom - oy) <= SNAP_DISTANCE) { y = oy - boxHeight; guideY = oy; status = "HUD EDGE"; }
            if (horizontalOverlap && Math.abs(y - otherBottom) <= SNAP_DISTANCE) { y = otherBottom; guideY = otherBottom; status = "HUD EDGE"; }
            if (Math.abs(x - ox) <= SNAP_DISTANCE) { x = ox; guideX = ox; status = "HUD LEFT"; }
            if (Math.abs((x + boxWidth) - otherRight) <= SNAP_DISTANCE) { x = otherRight - boxWidth; guideX = otherRight; status = "HUD RIGHT"; }
            if (Math.abs(y - oy) <= SNAP_DISTANCE) { y = oy; guideY = oy; status = "HUD TOP"; }
            if (Math.abs((y + boxHeight) - otherBottom) <= SNAP_DISTANCE) { y = otherBottom - boxHeight; guideY = otherBottom; status = "HUD BOTTOM"; }

            cx = x + boxWidth / 2.0;
            cy = y + boxHeight / 2.0;
            if (Math.abs(cx - ocx) <= SNAP_DISTANCE) {
                x = ocx - boxWidth / 2.0; cx = ocx; guideX = (int) Math.round(ocx); status = "HUD CENTER X";
            }
            if (Math.abs(cy - ocy) <= SNAP_DISTANCE) {
                y = ocy - boxHeight / 2.0; cy = ocy; guideY = (int) Math.round(ocy); status = guideX >= 0 ? "HUD CENTERED" : "HUD CENTER Y";
            }
        }
        return new double[] {x, y};
    }

    private double[] snapArmorPiece(double x, double y, int pieceSize) {
        double maxX = Math.max(0, field_22789 - pieceSize);
        double maxY = Math.max(0, field_22790 - pieceSize);
        double centerX = x + pieceSize / 2.0;
        double centerY = y + pieceSize / 2.0;

        if (Math.abs(x) <= SNAP_DISTANCE) { x = 0; guideX = 0; status = "LEFT EDGE"; }
        if (Math.abs(x - maxX) <= SNAP_DISTANCE) { x = maxX; guideX = field_22789 - 1; status = "RIGHT EDGE"; }
        if (Math.abs(y) <= SNAP_DISTANCE) { y = 0; guideY = 0; status = "TOP EDGE"; }
        if (Math.abs(y - maxY) <= SNAP_DISTANCE) { y = maxY; guideY = field_22790 - 1; status = "BOTTOM EDGE"; }

        centerX = x + pieceSize / 2.0;
        centerY = y + pieceSize / 2.0;
        if (Math.abs(centerX - field_22789 / 2.0) <= SNAP_DISTANCE) {
            x = field_22789 / 2.0 - pieceSize / 2.0; guideX = field_22789 / 2; status = "CENTER X";
        }
        if (Math.abs(centerY - field_22790 / 2.0) <= SNAP_DISTANCE) {
            y = field_22790 / 2.0 - pieceSize / 2.0; guideY = field_22790 / 2; status = guideX == field_22789 / 2 ? "CENTERED" : "CENTER Y";
        }

        centerX = x + pieceSize / 2.0;
        centerY = y + pieceSize / 2.0;
        for (Module module : YogiEssentialsClient.getModuleManager().getModules()) {
            if (!isEditorElement(module)) continue;
            if (module instanceof ArmorHudModule armor
                    && armor.getIndividualArmorPositions().get()
                    && armor.isIconMode()) {
                for (int i = 0; i < armor.getConfiguredItemCount(); i++) {
                    if (armor == armorPieceDragging && i == armorPieceIndex) continue;
                    int ox = HudManager.getArmorPieceEditorX(armor, i, field_22789);
                    int oy = HudManager.getArmorPieceEditorY(armor, i, field_22790);
                    int os = HudManager.getArmorPieceEditorSize(armor, i);
                    double ocx = ox + os / 2.0;
                    double ocy = oy + os / 2.0;
                    if (Math.abs(centerX - ocx) <= SNAP_DISTANCE) {
                        x = ocx - pieceSize / 2.0; centerX = ocx; guideX = (int) Math.round(ocx); status = "ARMOR CENTER X";
                    }
                    if (Math.abs(centerY - ocy) <= SNAP_DISTANCE) {
                        y = ocy - pieceSize / 2.0; centerY = ocy; guideY = (int) Math.round(ocy); status = guideX >= 0 ? "ARMOR CENTERED" : "ARMOR CENTER Y";
                    }
                }
                continue;
            }
            if (module == armorPieceDragging) continue;
            int ow = HudManager.getEditorRenderedWidth(module);
            int oh = HudManager.getEditorRenderedHeight(module);
            int ox = HudManager.getEditorPixelX(module, field_22789);
            int oy = HudManager.getEditorPixelY(module, field_22790);
            double ocx = ox + ow / 2.0;
            double ocy = oy + oh / 2.0;
            if (Math.abs(centerX - ocx) <= SNAP_DISTANCE) {
                x = ocx - pieceSize / 2.0; centerX = ocx; guideX = (int) Math.round(ocx); status = "HUD CENTER X";
            }
            if (Math.abs(centerY - ocy) <= SNAP_DISTANCE) {
                y = ocy - pieceSize / 2.0; centerY = ocy; guideY = (int) Math.round(ocy); status = guideX >= 0 ? "HUD CENTERED" : "HUD CENTER Y";
            }
        }
        return new double[] {x, y};
    }

    private ResizeMode detectResizeMode(double mouseX, double mouseY, int x, int y, int w, int h) {
        boolean nearLeft = Math.abs(mouseX - x) <= EDGE_HIT;
        boolean nearRight = Math.abs(mouseX - (x + w)) <= EDGE_HIT;
        boolean nearTop = Math.abs(mouseY - y) <= EDGE_HIT;
        boolean nearBottom = Math.abs(mouseY - (y + h)) <= EDGE_HIT;
        boolean withinX = mouseX >= x - EDGE_HIT && mouseX <= x + w + EDGE_HIT;
        boolean withinY = mouseY >= y - EDGE_HIT && mouseY <= y + h + EDGE_HIT;

        if (nearLeft && nearTop) return ResizeMode.NW;
        if (nearRight && nearTop) return ResizeMode.NE;
        if (nearLeft && nearBottom) return ResizeMode.SW;
        if (nearRight && nearBottom) return ResizeMode.SE;
        if (nearTop && withinX) return ResizeMode.N;
        if (nearBottom && withinX) return ResizeMode.S;
        if (nearLeft && withinY) return ResizeMode.W;
        if (nearRight && withinY) return ResizeMode.E;
        return ResizeMode.NONE;
    }

    private boolean insideClose(double mouseX, double mouseY, int x, int y, int boxWidth) {
        return inside(mouseX, mouseY, x + boxWidth - CLOSE_SIZE - 3, y + 3, CLOSE_SIZE, CLOSE_SIZE);
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static String armorPieceName(int index) {
        return switch (index) {
            case 0 -> "HELMET";
            case 1 -> "CHESTPLATE";
            case 2 -> "LEGGINGS";
            default -> "BOOTS";
        };
    }

    private static void outline(class_332 context, int x, int y, int width, int height, int color) {
        context.method_25294(x, y, x + width, y + 1, color);
        context.method_25294(x, y + height - 1, x + width, y + height, color);
        context.method_25294(x, y, x + 1, y + height, color);
        context.method_25294(x + width - 1, y, x + width, y + height, color);
    }

    private void clearGuide() {
        guideX = -1;
        guideY = -1;
        status = "";
    }

    @Override
    public void method_25419() {
        ConfigManager.save();
        if (field_22787 != null) field_22787.method_1507(null);
    }

    @Override
    public boolean method_25421() {
        return false;
    }
}
