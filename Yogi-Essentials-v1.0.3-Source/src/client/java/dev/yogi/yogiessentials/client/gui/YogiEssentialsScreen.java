package dev.yogi.yogiessentials.client.gui;

import dev.yogi.yogiessentials.client.YogiEssentialsClient;
import dev.yogi.yogiessentials.client.config.ConfigManager;
import dev.yogi.yogiessentials.client.module.Category;
import dev.yogi.yogiessentials.client.module.Module;
import dev.yogi.yogiessentials.client.module.ModuleRules;
import dev.yogi.yogiessentials.client.setting.Setting;
import dev.yogi.yogiessentials.client.module.performance.PerformanceOptimizer;
import dev.yogi.yogiessentials.client.module.performance.BorderlessFullscreenModule;
import dev.yogi.yogiessentials.client.util.VulkanWindowModeBridge;
import dev.yogi.yogiessentials.client.util.UiSoundManager;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.class_10799;
import net.minecraft.class_11905;
import net.minecraft.class_11908;
import net.minecraft.class_11909;
import net.minecraft.class_2561;
import net.minecraft.class_2960;
import net.minecraft.class_332;
import net.minecraft.class_437;
import net.minecraft.class_5481;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class YogiEssentialsScreen extends class_437 {

    private static final class_2960 LOGO =
            class_2960.method_60655(
                    "yogiessentials",
                    "textures/gui/icon.png"
            );

    private static final int ORANGE =
            0xFFFF6A00;

    private static final int PANEL =
            0xF20D0D10;

    private static final int SIDEBAR =
            0xF5121216;

    private static final int CARD =
            0xE817171C;

    private static final int CARD_HOVER =
            0xF0202026;

    private static final int BORDER =
            0xFF292930;

    private static final int TEXT =
            0xFFF5F5F5;

    private static final int TEXT_MUTED =
            0xFF99999F;

    private static final int TARGET_WIDTH =
            720;

    private static final int TARGET_HEIGHT =
            438;

    private static final int SIDEBAR_WIDTH =
            120;

    private static final int HEADER_HEIGHT =
            72;

    private static final int CARD_GAP =
            8;

    private static final int CARD_HEIGHT =
            92;

    private static final int OPTIMIZER_CARD_HEIGHT =
            76;

    private static final int REMINDER_WIDTH =
            214;

    private static final int REMINDER_HEIGHT =
            78;

    private static final int SCROLL_STEP =
            42;

    private static boolean vulkanNoticeShownThisSession;
    private boolean vulkanNoticeVisible;

    private Category selectedCategory =
            Category.VISUAL;

    private int scrollOffset;

    private boolean searchFocused;
    private String searchQuery =
            "";

    public YogiEssentialsScreen() {
        super(
                class_2561.method_43470(
                        "Yogi Essentials"
                )
        );

        if (FabricLoader.getInstance().isModLoaded("vulkanmod")
                && !vulkanNoticeShownThisSession) {
            vulkanNoticeShownThisSession = true;
            vulkanNoticeVisible = true;
        }
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
                0x72000000
        );

        int panelWidth =
                Math.min(
                        TARGET_WIDTH,
                        Math.max(1, field_22789 - 16)
                );

        int panelHeight =
                Math.min(
                        TARGET_HEIGHT,
                        Math.max(1, field_22790 - 8)
                );

        int panelX =
                (
                        field_22789 - panelWidth
                ) / 2;

        int panelY =
                (
                        field_22790 - panelHeight
                ) / 2;

        renderFrame(
                context,
                panelX,
                panelY,
                panelWidth,
                panelHeight
        );

        renderHeader(
                context,
                panelX,
                panelY,
                panelWidth
        );

        renderSidebar(
                context,
                panelX,
                panelY,
                panelWidth,
                panelHeight,
                mouseX,
                mouseY
        );

        renderContent(
                context,
                panelX,
                panelY,
                panelWidth,
                panelHeight,
                mouseX,
                mouseY
        );

        renderOptimizationReminder(
                context,
                panelX,
                panelY,
                panelWidth,
                panelHeight,
                mouseX,
                mouseY
        );

        if (vulkanNoticeVisible) {
            renderVulkanNotice(context, mouseX, mouseY);
        }
    }

    private void renderFrame(
            class_332 context,
            int x,
            int y,
            int width,
            int height
    ) {
        context.method_25294(
                x - 4,
                y - 4,
                x + width + 4,
                y + height + 4,
                0x65000000
        );

        context.method_25294(
                x,
                y,
                x + width,
                y + height,
                PANEL
        );

        context.method_25294(
                x,
                y,
                x + width,
                y + 2,
                ORANGE
        );

        context.method_25294(
                x,
                y,
                x + 2,
                y + height,
                ORANGE
        );

        context.method_25294(
                x + width - 2,
                y,
                x + width,
                y + height,
                ORANGE
        );

        context.method_25294(
                x,
                y + height - 2,
                x + width,
                y + height,
                ORANGE
        );

        int sidebarWidth = sidebarWidth(width);

        context.method_25294(
                x + 2,
                y + 2,
                x + sidebarWidth,
                y + height - 2,
                SIDEBAR
        );

        context.method_25294(
                x + sidebarWidth,
                y + HEADER_HEIGHT,
                x + sidebarWidth + 1,
                y + height - 2,
                BORDER
        );

        context.method_25294(
                x + 2,
                y + HEADER_HEIGHT,
                x + width - 2,
                y + HEADER_HEIGHT + 1,
                BORDER
        );
    }

    private void renderHeader(
            class_332 context,
            int panelX,
            int panelY,
            int panelWidth
    ) {
        int logoSize =
                48;

        int sidebar = sidebarWidth(panelWidth);
        int logoX = panelX + Math.max(0, (sidebar - logoSize) / 2);

        context.method_25302(
                class_10799.field_56883,
                LOGO,
                logoX,
                panelY + 12,
                0.0F,
                0.0F,
                logoSize,
                logoSize,
                512,
                512,
                512,
                512
        );

        int searchWidth = searchWidth(panelWidth);

        int searchX =
                panelX
                        + panelWidth
                        - searchWidth
                        - headerSidePadding(panelWidth);

        int searchY =
                panelY + 19;

        context.method_25294(
                searchX,
                searchY,
                searchX + searchWidth,
                searchY + 30,
                searchFocused
                        ? 0xFF1F1F23
                        : 0xFF17171A
        );

        outline(
                context,
                searchX,
                searchY,
                searchWidth,
                30,
                searchFocused
                        ? ORANGE
                        : BORDER
        );

        String displayed =
                searchQuery.isEmpty()
                        ? "Search all modules and settings..."
                        : searchQuery;
        displayed = field_22793.method_27523(displayed, Math.max(12, searchWidth - 20));

        context.method_25303(
                field_22793,
                displayed,
                searchX + 10,
                searchY + 11,
                searchQuery.isEmpty()
                        ? 0xFF707078
                        : TEXT
        );
    }

    private void renderSidebar(
            class_332 context,
            int panelX,
            int panelY,
            int panelWidth,
            int panelHeight,
            int mouseX,
            int mouseY
    ) {
        int y =
                panelY
                        + HEADER_HEIGHT
                        + 10;

        int sidebarWidth = sidebarWidth(panelWidth);
        int step = categoryStep(panelHeight);
        int buttonHeight = categoryButtonHeight(panelHeight);

        for (
                Category category
                : Category.values()
        ) {
            int x =
                    panelX + 8;

            int buttonWidth =
                    Math.max(28, sidebarWidth - 16);

            boolean selected =
                    category
                            == selectedCategory;

            boolean hovered =
                    inside(
                            mouseX,
                            mouseY,
                            x,
                            y,
                            buttonWidth,
                            buttonHeight
                    );

            if (selected) {
                context.method_25294(
                        x,
                        y,
                        x + buttonWidth,
                        y + buttonHeight,
                        0xFF73310F
                );

                context.method_25294(
                        x,
                        y,
                        x + 3,
                        y + buttonHeight,
                        ORANGE
                );
            } else if (hovered) {
                context.method_25294(
                        x,
                        y,
                        x + buttonWidth,
                        y + buttonHeight,
                        0xFF202025
                );
            }

            context.method_25303(
                    field_22793,
                    field_22793.method_27523(
                            category.getDisplayName(),
                            Math.max(8, buttonWidth - 18)
                    ),
                    x + 12,
                    y + Math.max(8, (buttonHeight - field_22793.field_2000) / 2),
                    selected
                            ? TEXT
                            : 0xFFC0C0C5
            );

            y += step;
        }


        String version = dev.yogi.yogiessentials.client.util.ModVersionUtil.displayVersion();
        int versionX = panelX + Math.max(4, (sidebarWidth - field_22793.method_1727(version)) / 2);
        context.method_25303(
                field_22793,
                version,
                versionX,
                panelY
                        + panelHeight
                        - 20,
                0xFF55555B
        );
    }

    private void renderContent(
            class_332 context,
            int panelX,
            int panelY,
            int panelWidth,
            int panelHeight,
            int mouseX,
            int mouseY
    ) {
        int side = sidebarWidth(panelWidth);
        int contentPadding = contentPadding(panelWidth);
        int contentX =
                panelX
                        + side
                        + contentPadding;

        int contentY =
                panelY
                        + HEADER_HEIGHT
                        + 18;

        int contentWidth =
                Math.max(40, panelWidth
                        - side
                        - contentPadding * 2);

        context.method_25303(
                field_22793,
                searchQuery.isBlank() ? selectedCategory.getDisplayName() + " Modules" : "Search results",
                contentX,
                contentY,
                TEXT
        );

        context.method_51433(
                field_22793,
                field_22793.method_27523(
                        categoryDescription(selectedCategory),
                        Math.max(20, contentWidth)
                ),
                contentX,
                contentY + 15,
                TEXT_MUTED,
                true
        );

        boolean showOptimizer =
                selectedCategory
                        == Category.PERFORMANCE
                        && searchQuery.isBlank();

        int clipTop =
                contentY + 38;

        int clipBottom =
                panelY
                        + panelHeight
                        - 12;

        boolean singleColumn = contentWidth < 360;
        int cardWidth = singleColumn
                ? contentWidth
                : (contentWidth - CARD_GAP) / 2;

        int scrollContentTop =
                contentY + 42;

        int moduleStartY =
                scrollContentTop
                        + (showOptimizer
                        ? OPTIMIZER_CARD_HEIGHT + CARD_GAP
                        : 0);

        List<CardLayout> layouts =
                buildLayouts(
                        filteredModules(),
                        contentX,
                        moduleStartY,
                        cardWidth,
                        singleColumn
                );

        context.method_44379(
                contentX,
                clipTop,
                contentX + contentWidth,
                clipBottom
        );

        if (showOptimizer) {
            int optimizerY =
                    scrollContentTop
                            - scrollOffset;

            if (
                    optimizerY + OPTIMIZER_CARD_HEIGHT >= clipTop
                            && optimizerY <= clipBottom
            ) {
                renderPerformanceOptimizerCard(
                        context,
                        contentX,
                        optimizerY,
                        contentWidth,
                        mouseX,
                        mouseY
                );
            }
        }

        if (layouts.isEmpty()) {
            int emptyY =
                    scrollContentTop
                            - scrollOffset
                            + (showOptimizer ? OPTIMIZER_CARD_HEIGHT + CARD_GAP : 0)
                            + 8;

            context.method_25303(
                    field_22793,
                    "No modules match your search.",
                    contentX,
                    emptyY,
                    TEXT_MUTED
            );

            context.method_44380();
            return;
        }

        for (
                CardLayout layout
                : layouts
        ) {
            int y =
                    layout.y()
                            - scrollOffset;

            if (
                    y + CARD_HEIGHT
                            < clipTop
                            ||
                    y > clipBottom
            ) {
                continue;
            }

            renderModuleCard(
                    context,
                    layout.module(),
                    layout.x(),
                    y,
                    cardWidth,
                    CARD_HEIGHT,
                    mouseX,
                    mouseY
            );
        }

        context.method_44380();
    }

    private List<CardLayout> buildLayouts(
            List<Module> modules,
            int contentX,
            int startY,
            int cardWidth,
            boolean singleColumn
    ) {
        List<CardLayout> layouts = new ArrayList<>();

        if (singleColumn) {
            int y = startY;
            for (Module module : modules) {
                layouts.add(new CardLayout(module, contentX, y));
                y += CARD_HEIGHT + CARD_GAP;
            }
            return layouts;
        }

        int leftY =
                startY;

        int rightY =
                startY;

        int rightX =
                contentX
                        + cardWidth
                        + CARD_GAP;

        for (
                Module module
                : modules
        ) {
            if (leftY <= rightY) {
                layouts.add(
                        new CardLayout(
                                module,
                                contentX,
                                leftY
                        )
                );

                leftY +=
                        CARD_HEIGHT
                                + CARD_GAP;
            } else {
                layouts.add(
                        new CardLayout(
                                module,
                                rightX,
                                rightY
                        )
                );

                rightY +=
                        CARD_HEIGHT
                                + CARD_GAP;
            }
        }

        return layouts;
    }

    private void renderModuleCard(
            class_332 context,
            Module module,
            int x,
            int y,
            int width,
            int height,
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
                        ? CARD_HOVER
                        : CARD
        );

        outline(
                context,
                x,
                y,
                width,
                height,
                module.isEnabled()
                        ? ORANGE
                        : BORDER
        );

        if (
                module.isEnabled()
        ) {
            context.method_25294(
                    x,
                    y,
                    x + 3,
                    y + height,
                    ORANGE
            );
        }

        context.method_25303(
                field_22793,
                searchQuery.isBlank() ? module.getName() : module.getCategory().getDisplayName() + " > " + module.getName(),
                x + 13,
                y + 13,
                TEXT
        );

        drawFittedCardDescription(
                context,
                matchingSettingDescription(module),
                x + 13,
                y + 31,
                width - 26,
                4,
                TEXT_MUTED
        );

        renderToggle(
                context,
                x + width - 44,
                y + 11,
                module.isEnabled()
        );

        String footer =
                module
                        .getSettings()
                        .isEmpty()
                        ? "Click to view preview"
                        : "Click to customize + preview";

        context.method_51433(
                field_22793,
                footer,
                x + 13,
                y + height - 18,
                hovered
                        ? ORANGE
                        : 0xFF77777F,
                true
        );
    }


    






    private void drawFittedCardDescription(
            class_332 context,
            String value,
            int x,
            int y,
            int maxWidth,
            int maxLines,
            int color
    ) {
        float scale = 1.0F;
        List<class_5481> lines = List.of();

        while (scale >= 0.68F) {
            int logicalWidth = Math.max(
                    1,
                    (int) Math.floor(maxWidth / scale)
            );

            lines = field_22793.method_1728(
                    class_2561.method_43470(value),
                    logicalWidth
            );

            if (lines.size() <= maxLines) {
                break;
            }

            scale -= 0.04F;
        }

        var matrices = context.method_51448();
        matrices.pushMatrix();
        matrices.translate(x, y);
        matrices.scale(scale, scale);

        int count = Math.min(maxLines, lines.size());
        for (int i = 0; i < count; i++) {
            context.method_51430(
                    field_22793,
                    lines.get(i),
                    0,
                    i * 10,
                    color,
                    true
            );
        }

        matrices.popMatrix();
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

        double mouseX =
                click.comp_4798();

        double mouseY =
                click.comp_4799();

        if (vulkanNoticeVisible) {
            int modalWidth = Math.min(500, Math.max(280, field_22789 - 40));
            int modalHeight = 180;
            int modalX = (field_22789 - modalWidth) / 2;
            int modalY = (field_22790 - modalHeight) / 2;
            int buttonY = modalY + modalHeight - 42;
            int gap = 10;
            int buttonWidth = (modalWidth - 34 - gap) / 2;
            int leftButtonX = modalX + 12;
            int rightButtonX = leftButtonX + buttonWidth + gap;

            if (inside(mouseX, mouseY, leftButtonX, buttonY, buttonWidth, 28)) {
                VulkanWindowModeBridge.forceWindowedMode();
                BorderlessFullscreenModule borderless = YogiEssentialsClient.getModuleManager() == null
                        ? null
                        : YogiEssentialsClient.getModuleManager().getModule(BorderlessFullscreenModule.class);
                if (borderless != null) {
                    if (!borderless.isEnabled()) {
                        borderless.setEnabled(true);
                    } else {
                        borderless.applyNow();
                    }
                    ConfigManager.save();
                }
                vulkanNoticeVisible = false;
                UiSoundManager.click();
                return true;
            }

            if (inside(mouseX, mouseY, rightButtonX, buttonY, buttonWidth, 28)) {
                vulkanNoticeVisible = false;
                UiSoundManager.click();
                return true;
            }

            return true;
        }

        int panelWidth =
                Math.min(
                        TARGET_WIDTH,
                        Math.max(1, field_22789 - 16)
                );

        int panelHeight =
                Math.min(
                        TARGET_HEIGHT,
                        Math.max(1, field_22790 - 8)
                );

        int panelX =
                (
                        field_22789 - panelWidth
                ) / 2;

        int panelY =
                (
                        field_22790 - panelHeight
                ) / 2;

        if (PerformanceOptimizer.shouldShowOptimizationReminder()) {
            ReminderLayout reminder =
                    optimizationReminderLayout(
                            panelX,
                            panelY,
                            panelWidth,
                            panelHeight
                    );

            if (reminder.visible()) {
                int closeX =
                        reminder.x() + REMINDER_WIDTH - 23;
                int closeY =
                        reminder.y() + 8;

                if (
                        inside(
                                mouseX,
                                mouseY,
                                closeX,
                                closeY,
                                14,
                                14
                        )
                ) {
                    UiSoundManager.click();
                    PerformanceOptimizer.dismissOptimizationReminder();
                    return true;
                }

                if (
                        inside(
                                mouseX,
                                mouseY,
                                reminder.x(),
                                reminder.y(),
                                REMINDER_WIDTH,
                                REMINDER_HEIGHT
                        )
                ) {
                    UiSoundManager.click();

                    if (field_22787 != null) {
                        field_22787.method_1507(
                                new PerformanceOptimizerScreen(
                                        this
                                )
                        );
                    }

                    return true;
                }
            }
        }

        int searchWidth = searchWidth(panelWidth);

        int searchX =
                panelX
                        + panelWidth
                        - searchWidth
                        - headerSidePadding(panelWidth);

        int searchY =
                panelY + 19;

        if (
                inside(
                        mouseX,
                        mouseY,
                        searchX,
                        searchY,
                        searchWidth,
                        30
                )
        ) {
            searchFocused = true;
            UiSoundManager.click();

            return true;
        }

        searchFocused = false;

        int categoryY =
                panelY
                        + HEADER_HEIGHT
                        + 10;
        int side = sidebarWidth(panelWidth);
        int categoryStep = categoryStep(panelHeight);
        int categoryButtonHeight = categoryButtonHeight(panelHeight);

        for (
                Category category
                : Category.values()
        ) {
            if (
                    inside(
                            mouseX,
                            mouseY,
                            panelX + 8,
                            categoryY,
                            Math.max(28, side - 16),
                            categoryButtonHeight
                    )
            ) {
                if (
                        selectedCategory
                                != category
                ) {
                    selectedCategory =
                            category;

                    scrollOffset = 0;
                    UiSoundManager.category();
                }

                return true;
            }

            categoryY += categoryStep;
        }


        int contentPadding = contentPadding(panelWidth);
        int contentX =
                panelX
                        + side
                        + contentPadding;

        int contentY =
                panelY
                        + HEADER_HEIGHT
                        + 18;

        int contentWidth =
                Math.max(40, panelWidth
                        - side
                        - contentPadding * 2);

        boolean singleColumn = contentWidth < 360;
        int cardWidth = singleColumn
                ? contentWidth
                : (contentWidth - CARD_GAP) / 2;

        boolean showOptimizer =
                selectedCategory
                        == Category.PERFORMANCE
                        && searchQuery.isBlank();

        int scrollContentTop =
                contentY + 42;

        if (showOptimizer) {
            int optimizerY =
                    scrollContentTop
                            - scrollOffset;

            int buttonWidth = 142;
            int buttonX =
                    contentX
                            + contentWidth
                            - buttonWidth
                            - 12;
            int buttonY =
                    optimizerY
                            + (OPTIMIZER_CARD_HEIGHT - 28) / 2;

            if (
                    inside(
                            mouseX,
                            mouseY,
                            buttonX,
                            buttonY,
                            buttonWidth,
                            28
                    )
            ) {
                UiSoundManager.click();

                if (field_22787 != null) {
                    field_22787.method_1507(
                            new PerformanceOptimizerScreen(
                                    this
                            )
                    );
                }

                return true;
            }
        }

        int clipTop =
                contentY + 38;

        int clipBottom =
                panelY
                        + panelHeight
                        - 12;

        if (
                mouseY < clipTop
                        ||
                mouseY > clipBottom
        ) {
            return false;
        }

        for (
                CardLayout layout
                : buildLayouts(
                        filteredModules(),
                        contentX,
                        scrollContentTop
                                + (showOptimizer
                                ? OPTIMIZER_CARD_HEIGHT + CARD_GAP
                                : 0),
                        cardWidth,
                        singleColumn
                )
        ) {
            int y =
                    layout.y()
                            - scrollOffset;

            if (
                    !inside(
                            mouseX,
                            mouseY,
                            layout.x(),
                            y,
                            cardWidth,
                            CARD_HEIGHT
                    )
            ) {
                continue;
            }

            int toggleX =
                    layout.x()
                            + cardWidth
                            - 44;

            if (
                    inside(
                            mouseX,
                            mouseY,
                            toggleX,
                            y + 11,
                            31,
                            15
                    )
            ) {
                if (!layout.module().isEnabled() && !ModuleRules.activeConflicts(layout.module()).isEmpty()) {
                    if (field_22787 != null) {
                        field_22787.method_1507(new ModuleConflictScreen(this, layout.module()));
                    }
                    return true;
                }
                if (!layout.module().isEnabled() && !ModuleRules.canEnable(layout.module())) {
                    return true;
                }
                layout
                        .module()
                        .toggle();

                if (
                        layout
                                .module()
                                .isEnabled()
                ) {
                    UiSoundManager
                            .moduleEnabled();
                } else {
                    UiSoundManager
                            .moduleDisabled();
                }

                ConfigManager.save();

                return true;
            }

            UiSoundManager.click();

            if (field_22787 != null) {
                field_22787.method_1507(
                        new ModuleSettingsScreen(
                                this,
                                layout.module()
                        )
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
    public boolean method_25401(
            double mouseX,
            double mouseY,
            double horizontalAmount,
            double verticalAmount
    ) {
        scrollOffset -=
                (int) Math.round(
                        verticalAmount
                                * SCROLL_STEP
                );

        scrollOffset =
                Math.max(
                        0,
                        Math.min(
                                maxScroll(),
                                scrollOffset
                        )
                );

        return true;
    }

    private void drawSidebarFooterText(
            class_332 context,
            String value,
            int x,
            int y,
            int maxWidth,
            int color
    ) {
        int textWidth = Math.max(1, field_22793.method_1727(value));
        if (textWidth <= maxWidth) {
            context.method_25303(field_22793, value, x, y, color);
            return;
        }

        
        
        
        float scale = Math.max(0.10F, Math.min(1.0F, maxWidth / (float) textWidth));
        var matrices = context.method_51448();
        matrices.pushMatrix();
        matrices.translate(x, y);
        matrices.scale(scale, scale);
        context.method_25303(field_22793, value, 0, 0, color);
        matrices.popMatrix();
    }

    private static int sidebarWidth(int panelWidth) {
        if (panelWidth < 420) return 90;
        if (panelWidth < 560) return 104;
        return SIDEBAR_WIDTH;
    }

    private static int contentPadding(int panelWidth) {
        if (panelWidth < 420) return 8;
        if (panelWidth < 560) return 12;
        return 20;
    }

    private static int headerSidePadding(int panelWidth) {
        return panelWidth < 420 ? 10 : 20;
    }

    private static int searchWidth(int panelWidth) {
        int available = Math.max(80, panelWidth - 96);
        if (panelWidth < 420) return Math.min(120, available);
        if (panelWidth < 560) return Math.min(150, available);
        return Math.min(205, available);
    }

    private static int categoryStep(int panelHeight) {
        int count = Math.max(1, Category.values().length);
        int available = Math.max(100, panelHeight - HEADER_HEIGHT - 64);
        return Math.max(20, Math.min(37, available / count));
    }

    private static int categoryButtonHeight(int panelHeight) {
        return Math.max(18, Math.min(30, categoryStep(panelHeight) - 3));
    }

    private int maxScroll() {
        List<Module> modules =
                filteredModules();

        int panelWidth = Math.min(TARGET_WIDTH, Math.max(1, field_22789 - 16));
        int contentWidth = Math.max(40, panelWidth - sidebarWidth(panelWidth) - contentPadding(panelWidth) * 2);
        boolean singleColumn = contentWidth < 360;
        int rows = singleColumn
                ? modules.size()
                : (modules.size() + 1) / 2;

        int totalHeight =
                rows
                        * CARD_HEIGHT
                        +
                Math.max(
                        0,
                        rows - 1
                )
                        * CARD_GAP;

        if (
                selectedCategory == Category.PERFORMANCE
                        && searchQuery.isBlank()
        ) {
            totalHeight +=
                    OPTIMIZER_CARD_HEIGHT + CARD_GAP;
        }

        int visibleHeight =
                Math.min(TARGET_HEIGHT, Math.max(1, field_22790 - 8))
                        - HEADER_HEIGHT
                        - 72;

        return Math.max(
                0,
                totalHeight
                        - visibleHeight
        );
    }

    @Override
    public boolean method_25400(
            class_11905 input
    ) {
        if (!searchFocused) {
            return super.method_25400(
                    input
            );
        }

        if (!input.method_74227()) {
            return false;
        }

        int codePoint =
                input.comp_4793();

        if (
                Character.isLetterOrDigit(
                        codePoint
                )
                        ||
                Character.isWhitespace(
                        codePoint
                )
                        ||
                codePoint == '-'
                        ||
                codePoint == '_'
        ) {
            searchQuery +=
                    input.method_74226();

            scrollOffset = 0;

            return true;
        }

        return false;
    }

    @Override
    public boolean method_25404(
            class_11908 input
    ) {
        if (vulkanNoticeVisible) {
            if (input.comp_4795() == GLFW.GLFW_KEY_ESCAPE
                    || input.comp_4795() == GLFW.GLFW_KEY_ENTER
                    || input.comp_4795() == GLFW.GLFW_KEY_KP_ENTER) {
                vulkanNoticeVisible = false;
                return true;
            }
            return true;
        }

        if (searchFocused) {
            if (
                    input.comp_4795()
                            == GLFW.GLFW_KEY_BACKSPACE
            ) {
                if (
                        !searchQuery.isEmpty()
                ) {
                    int last =
                            searchQuery
                                    .codePointBefore(
                                            searchQuery
                                                    .length()
                                    );

                    searchQuery =
                            searchQuery.substring(
                                    0,
                                    searchQuery.length()
                                            - Character
                                            .charCount(
                                                    last
                                            )
                            );

                    scrollOffset = 0;
                }

                return true;
            }

            if (
                    input.comp_4795()
                            == GLFW.GLFW_KEY_ESCAPE
            ) {
                searchFocused = false;
                return true;
            }

            if (
                    input.comp_4795()
                            == GLFW.GLFW_KEY_ENTER
                            ||
                    input.comp_4795()
                            == GLFW.GLFW_KEY_KP_ENTER
            ) {
                searchFocused = false;
                return true;
            }
        }

        return super.method_25404(
                input
        );
    }

    private List<Module> filteredModules() {
        List<Module> modules = searchQuery.isBlank()
                ? YogiEssentialsClient.getModuleManager().getModulesByCategory(selectedCategory)
                : YogiEssentialsClient.getModuleManager().getModules();

        if (
                searchQuery.isBlank()
        ) {
            return modules;
        }

        String query =
                searchQuery
                        .toLowerCase();

        return modules
                .stream()
                .filter(
                        module ->
                                module
                                        .getName()
                                        .toLowerCase()
                                        .contains(
                                                query
                                        )
                                        ||
                                module.getDescription().toLowerCase().contains(query)
                                        || module.getCategory().getDisplayName().toLowerCase().contains(query)
                                        || module.getSettings().stream().map(Setting::getName)
                                        .anyMatch(name -> name.toLowerCase().contains(query))
                )
                .toList();
    }

    private String matchingSettingDescription(Module module) {
        if (!searchQuery.isBlank()) {
            List<String> matches = module.getSettings().stream()
                    .map(Setting::getName)
                    .filter(name -> name.toLowerCase().contains(searchQuery.toLowerCase()))
                    .toList();
            if (!matches.isEmpty()) {
                return "Settings: " + String.join(", ", matches);
            }
        }
        return module.getDescription();
    }

    private String categoryDescription(
            Category category
    ) {
        return switch (category) {
            case VISUAL ->
                    "Visual modules. Click any card for a live preview and full settings.";

            case HUD ->
                    "HUD modules. Every element has its own preview and layout controls.";

            case PVP ->
                    "Combat-focused visual and quality-of-life modules.";

            case SMP ->
                    "Useful features for survival and multiplayer.";

            case CHAT ->
                    "Customizable chat alerts, appearance, search, history and message utilities.";

            case FIXES ->
                    "Client reliability fixes for input, focus, crosshair alignment and world transitions.";

            case OPTIMIZATIONS ->
                    "Context-aware PvP input and client render optimizations. Each is off by default and fully reverts when disabled.";

            case PERFORMANCE ->
                    "FPS-focused rendering options plus automatic hardware-aware tuning.";
        };
    }

    private void renderVulkanNotice(
            class_332 context,
            int mouseX,
            int mouseY
    ) {
        int modalWidth = Math.min(500, Math.max(280, field_22789 - 40));
        int modalHeight = 180;
        int x = (field_22789 - modalWidth) / 2;
        int y = (field_22790 - modalHeight) / 2;

        context.method_25294(0, 0, field_22789, field_22790, 0x99000000);
        context.method_25294(x - 3, y - 3, x + modalWidth + 3, y + modalHeight + 3, 0x8A000000);
        context.method_25294(x, y, x + modalWidth, y + modalHeight, 0xFC101014);
        outline(context, x, y, modalWidth, modalHeight, ORANGE);
        context.method_25294(x, y, x + modalWidth, y + 3, ORANGE);

        context.method_25300(
                field_22793,
                "VulkanMod + Yogi Essentials",
                x + modalWidth / 2,
                y + 16,
                ORANGE
        );

        int textX = x + 18;
        int textY = y + 44;
        int maxTextWidth = modalWidth - 36;
        String[] lines = {
                "VulkanMod was detected.",
                "Enable Borderless Fullscreen below and Yogi Essentials will set Vulkan's Window Mode to Windowed automatically.",
                "Yogi Essentials will then be the only owner of fullscreen presentation; Vulkan Fullscreen / Windowed Fullscreen will not compete with it.",
                "Minecraft remains a compositor-owned borderless window for smoother Discord capture and no taskbar gap."
        };
        for (String line : lines) {
            for (class_5481 wrapped : field_22793.method_1728(class_2561.method_43470(line), maxTextWidth)) {
                context.method_51430(field_22793, wrapped, textX, textY, TEXT_MUTED, true);
                textY += 11;
            }
            textY += 2;
        }

        int buttonY = y + modalHeight - 42;
        int gap = 10;
        int buttonWidth = (modalWidth - 34 - gap) / 2;
        int leftButtonX = x + 12;
        int rightButtonX = leftButtonX + buttonWidth + gap;
        renderModalButton(
                context,
                leftButtonX,
                buttonY,
                buttonWidth,
                "Enable Borderless Fullscreen",
                inside(mouseX, mouseY, leftButtonX, buttonY, buttonWidth, 28)
        );
        renderModalButton(
                context,
                rightButtonX,
                buttonY,
                buttonWidth,
                "Got it",
                inside(mouseX, mouseY, rightButtonX, buttonY, buttonWidth, 28)
        );
    }

    private void renderModalButton(
            class_332 context,
            int x,
            int y,
            int width,
            String label,
            boolean hovered
    ) {
        context.method_25294(x, y, x + width, y + 28, hovered ? 0xFFFF7B1C : 0xFFFF6A00);
        context.method_25300(
                field_22793,
                field_22793.method_27523(label, Math.max(8, width - 12)),
                x + width / 2,
                y + 10,
                0xFFFFFFFF
        );
    }

    private void renderOptimizationReminder(
            class_332 context,
            int panelX,
            int panelY,
            int panelWidth,
            int panelHeight,
            int mouseX,
            int mouseY
    ) {
        if (!PerformanceOptimizer.shouldShowOptimizationReminder()) {
            return;
        }

        ReminderLayout layout =
                optimizationReminderLayout(
                        panelX,
                        panelY,
                        panelWidth,
                        panelHeight
                );

        
        
        
        if (!layout.visible()) {
            return;
        }

        int x = layout.x();
        int y = layout.y();

        boolean hovered =
                inside(
                        mouseX,
                        mouseY,
                        x,
                        y,
                        REMINDER_WIDTH,
                        REMINDER_HEIGHT
                );

        context.method_25294(
                x - 4,
                y + 4,
                x + REMINDER_WIDTH + 4,
                y + REMINDER_HEIGHT + 5,
                0x66000000
        );

        context.method_25294(
                x,
                y,
                x + REMINDER_WIDTH,
                y + REMINDER_HEIGHT,
                hovered
                        ? 0xFA1B1B20
                        : 0xF516161A
        );

        outline(
                context,
                x,
                y,
                REMINDER_WIDTH,
                REMINDER_HEIGHT,
                ORANGE
        );

        context.method_25294(
                x,
                y,
                x + 3,
                y + REMINDER_HEIGHT,
                ORANGE
        );

        context.method_25303(
                field_22793,
                "Boost your performance",
                x + 12,
                y + 11,
                ORANGE
        );

        int closeX =
                x + REMINDER_WIDTH - 23;
        int closeY =
                y + 8;

        boolean closeHovered =
                inside(
                        mouseX,
                        mouseY,
                        closeX,
                        closeY,
                        14,
                        14
                );

        context.method_25300(
                field_22793,
                "×",
                closeX + 7,
                closeY + 3,
                closeHovered
                        ? 0xFFFFFFFF
                        : TEXT_MUTED
        );

        context.method_51433(
                field_22793,
                "Run Auto Optimize to tune",
                x + 12,
                y + 31,
                TEXT,
                true
        );

        context.method_51433(
                field_22793,
                "Minecraft for your PC.",
                x + 12,
                y + 43,
                TEXT_MUTED,
                true
        );

        context.method_25303(
                field_22793,
                "Optimize Now  →",
                x + 12,
                y + 61,
                hovered
                        ? ORANGE
                        : 0xFFB8B8BE
        );
    }

    private ReminderLayout optimizationReminderLayout(
            int panelX,
            int panelY,
            int panelWidth,
            int panelHeight
    ) {
        int gap = 12;
        int rightSpace = field_22789 - (panelX + panelWidth);
        int topSpace = panelY;
        int bottomSpace = field_22790 - (panelY + panelHeight);

        
        
        if (rightSpace >= REMINDER_WIDTH + gap) {
            return new ReminderLayout(
                    true,
                    panelX + panelWidth + gap,
                    Math.max(8, panelY)
            );
        }

        
        if (topSpace >= REMINDER_HEIGHT + gap) {
            return new ReminderLayout(
                    true,
                    Math.max(8, Math.min(
                            field_22789 - REMINDER_WIDTH - 8,
                            panelX + panelWidth - REMINDER_WIDTH
                    )),
                    panelY - REMINDER_HEIGHT - gap
            );
        }

        
        if (bottomSpace >= REMINDER_HEIGHT + gap) {
            return new ReminderLayout(
                    true,
                    Math.max(8, Math.min(
                            field_22789 - REMINDER_WIDTH - 8,
                            panelX + panelWidth - REMINDER_WIDTH
                    )),
                    panelY + panelHeight + gap
            );
        }

        return new ReminderLayout(false, 0, 0);
    }

    private record ReminderLayout(
            boolean visible,
            int x,
            int y
    ) {
    }

    private void renderPerformanceOptimizerCard(
            class_332 context,
            int x,
            int y,
            int width,
            int mouseX,
            int mouseY
    ) {
        int height = OPTIMIZER_CARD_HEIGHT;

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
                        ? CARD_HOVER
                        : CARD
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
                field_22793,
                "AUTO OPTIMIZER",
                x + 13,
                y + 13,
                ORANGE
        );

        List<class_5481> description =
                field_22793.method_1728(
                        class_2561.method_43470(
                                "Detects hardware and compatible mods, then applies the selected reversible optimizer profile."
                        ),
                        width - 190
                );

        for (
                int i = 0;
                i < Math.min(2, description.size());
                i++
        ) {
            context.method_51430(
                    field_22793,
                    description.get(i),
                    x + 13,
                    y + 31 + i * 11,
                    TEXT_MUTED,
                    true
            );
        }

        int buttonWidth = 142;
        int buttonHeight = 28;
        int buttonX =
                x + width - buttonWidth - 12;
        int buttonY =
                y + (height - buttonHeight) / 2;

        boolean alreadyOptimized =
                PerformanceOptimizer.isOptimizationActive();

        boolean buttonHovered =
                !alreadyOptimized
                        && inside(
                        mouseX,
                        mouseY,
                        buttonX,
                        buttonY,
                        buttonWidth,
                        buttonHeight
                );

        context.method_25294(
                buttonX,
                buttonY,
                buttonX + buttonWidth,
                buttonY + buttonHeight,
                alreadyOptimized
                        ? 0xFF17351F
                        : buttonHovered
                        ? 0xFFFF7B1A
                        : ORANGE
        );

        outline(
                context,
                buttonX,
                buttonY,
                buttonWidth,
                buttonHeight,
                alreadyOptimized
                        ? 0xFF68D391
                        : ORANGE
        );

        String label =
                alreadyOptimized
                        ? "Already Optimized"
                        : "Auto Optimize";

        context.method_25303(
                field_22793,
                label,
                buttonX
                        + (buttonWidth - field_22793.method_1727(label)) / 2,
                buttonY
                        + (buttonHeight - field_22793.field_2000) / 2,
                alreadyOptimized
                        ? 0xFFE8FFF0
                        : TEXT
        );
    }

    private void renderToggle(
            class_332 context,
            int x,
            int y,
            boolean enabled
    ) {
        context.method_25294(
                x,
                y,
                x + 31,
                y + 15,
                enabled
                        ? ORANGE
                        : 0xFF45454B
        );

        int knobX =
                enabled
                        ? x + 18
                        : x + 2;

        context.method_25294(
                knobX,
                y + 2,
                knobX + 11,
                y + 13,
                0xFFF4F4F4
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

    @Override
    public void method_25419() {
        ConfigManager.save();
        UiSoundManager.menuClose();
        super.method_25419();
    }

    @Override
    public boolean method_25421() {
        return false;
    }

    private record CardLayout(
            Module module,
            int x,
            int y
    ) {
    }
}
