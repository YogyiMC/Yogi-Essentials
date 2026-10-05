package dev.yogi.yogiessentials.client.gui;

import dev.yogi.yogiessentials.client.config.ConfigManager;
import dev.yogi.yogiessentials.client.module.performance.AdaptiveOptimizer;
import dev.yogi.yogiessentials.client.util.FrameTelemetry;
import dev.yogi.yogiessentials.client.module.performance.PerformanceOptimizer;
import dev.yogi.yogiessentials.client.module.performance.PerformanceOptimizer.CompatibilityInfo;
import dev.yogi.yogiessentials.client.module.performance.PerformanceOptimizer.HardwareInfo;
import dev.yogi.yogiessentials.client.module.performance.PerformanceOptimizer.ImpactChange;
import dev.yogi.yogiessentials.client.module.performance.PerformanceOptimizer.OptimizationImpact;
import dev.yogi.yogiessentials.client.module.performance.PerformanceOptimizer.OptimizationResult;
import dev.yogi.yogiessentials.client.util.UiSoundManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.class_11909;
import net.minecraft.class_2561;
import net.minecraft.class_332;
import net.minecraft.class_437;

public class PerformanceOptimizerScreen extends class_437 {

    private static final int ORANGE = 0xFFFF6A00;
    private static final int PANEL = 0xF20D0D10;
    private static final int CARD = 0xF016161B;
    private static final int BORDER = 0xFF303038;
    private static final int TEXT = 0xFFF5F5F5;
    private static final int MUTED = 0xFF9A9AA2;
    private static final int GREEN = 0xFF68D391;
    private static final int RED = 0xFFFF7777;

    private static final int TARGET_WIDTH = 780;
    private static final int TARGET_HEIGHT = 540;
    private static final int CONTENT_GAP = 10;
    private static final int SCROLL_STEP = 42;

    private static final String[] STAGES = {
            "Analyzing hardware",
            "Checking installed mods",
            "Choosing Minecraft config",
            "Applying Yogi Essentials settings",
            "Saving configuration"
    };

    private static final String[] IMPACT_CATEGORIES = {
            "FPS",
            "Rendering",
            "CPU/Chunks",
            "Memory",
            "Visual Quality"
    };

    private final class_437 parent;
    private static final int[] TARGETS = {0, 60, 120, 144, 165, 180, 240, 360};
    private int targetIndex;
    private AdaptiveOptimizer session;
    private boolean viewAllChanges;
    private ButtonBounds changesButton;

    private HardwareInfo hardware;
    private CompatibilityInfo compatibility;
    private OptimizationImpact impactPreview;
    private OptimizationResult result;

    private boolean optimizing;
    private int completedStages;
    private int stageTicks;
    private String optimizerWarning;

    private int scrollOffset;
    private int contentHeight;
    private int viewportTop;
    private int viewportBottom;

    public PerformanceOptimizerScreen(
            class_437 parent
    ) {
        super(class_2561.method_43470("Performance Optimizer"));
        this.parent = parent;
        for (int i = 0; i < TARGETS.length; i++) {
            if (TARGETS[i] == PerformanceOptimizer.exportConfig().targetFps) {
                targetIndex = i;
                break;
            }
        }
        refreshPreview();

        if (PerformanceOptimizer.isOptimizationActive()) {
            completedStages = STAGES.length;
            result = new OptimizationResult(
                    true,
                    "Already optimized.",
                    PerformanceOptimizer.getLastChanges()
            );
        }
    }

    @Override
    public void method_25393() {
        if (session == null) return;

        if (!session.finished()) {
            session.tick();
        }

        if (session.finished() && optimizing) {
            result = session.result();
            optimizing = false;
            completedStages = STAGES.length;
            refreshPreview();
        }
    }

    @Override
    public void method_25394(
            class_332 context,
            int mouseX,
            int mouseY,
            float deltaTicks
    ) {
        if (optimizing && session != null && !session.finished()) {
            renderBenchmarkOverlay(context, mouseX, mouseY);
            return;
        }

        context.method_25294(0, 0, field_22789, field_22790, 0x76000000);

        int panelWidth = Math.max(
                1,
                Math.min(TARGET_WIDTH, field_22789 - 12)
        );
        int panelHeight = Math.max(
                1,
                Math.min(TARGET_HEIGHT, field_22790 - 12)
        );
        int panelX = (field_22789 - panelWidth) / 2;
        int panelY = (field_22790 - panelHeight) / 2;

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
        context.method_25294(
                panelX,
                panelY,
                panelX + panelWidth,
                panelY + 3,
                ORANGE
        );

        int padding = panelWidth < 420 ? 8 : 14;
        int baseHeaderHeight = panelWidth < 430 ? 100 : 80;
        int warningHeight = optimizerWarning == null ? 0 : 24;
        int headerHeight = baseHeaderHeight + warningHeight;
        int footerHeight = restoreStacks(panelWidth) ? 72 : 50;

        renderHeader(
                context,
                panelX,
                panelY,
                panelWidth,
                padding
        );

        if (optimizerWarning != null) {
            renderOptimizerWarning(context, panelX + padding, panelY + baseHeaderHeight,
                    Math.max(20, panelWidth - padding * 2), 18);
        }

        viewportTop = Math.min(
                panelY + panelHeight - 1,
                panelY + headerHeight
        );
        viewportBottom = Math.max(
                viewportTop + 1,
                panelY + panelHeight - footerHeight
        );

        context.method_44379(
                panelX + 1,
                viewportTop,
                panelX + panelWidth - 1,
                viewportBottom
        );

        int innerX = panelX + padding;
        int innerWidth = Math.max(20, panelWidth - padding * 2);
        int y = viewportTop + 8 - scrollOffset;

        boolean wide = innerWidth >= 600;

        if (wide) {
            int half = Math.max(
                    20,
                    (innerWidth - CONTENT_GAP) / 2
            );
            int rightWidth = Math.max(
                    20,
                    innerWidth - half - CONTENT_GAP
            );
            int cardHeight = 132;

            renderHardwareCard(
                    context,
                    innerX,
                    y,
                    half,
                    cardHeight
            );
            renderCompatibilityCard(
                    context,
                    innerX + half + CONTENT_GAP,
                    y,
                    rightWidth,
                    cardHeight,
                    true
            );
            y += cardHeight + CONTENT_GAP;
        } else {
            int hardwareHeight = 132;
            int compatibilityHeight = innerWidth >= 320 ? 132 : 228;

            renderHardwareCard(
                    context,
                    innerX,
                    y,
                    innerWidth,
                    hardwareHeight
            );
            y += hardwareHeight + CONTENT_GAP;

            renderCompatibilityCard(
                    context,
                    innerX,
                    y,
                    innerWidth,
                    compatibilityHeight,
                    innerWidth >= 320
            );
            y += compatibilityHeight + CONTENT_GAP;
        }

        renderMeasuredCard(context, innerX, y, innerWidth);
        y += 170 + CONTENT_GAP;

        if (optimizing || result != null || PerformanceOptimizer.isOptimizationActive()) {
            int progressHeight = calculateProgressHeight(innerWidth);
            renderProgressCard(
                    context,
                    innerX,
                    y,
                    innerWidth,
                    progressHeight
            );
            y += progressHeight + CONTENT_GAP;
        }

        contentHeight = Math.max(
                0,
                y - (viewportTop + 8 - scrollOffset)
        );

        context.method_44380();

        clampScroll();
        renderScrollbar(
                context,
                panelX + panelWidth - 4,
                viewportTop,
                viewportBottom
        );

        renderBottomButtons(
                context,
                panelX,
                panelY,
                panelWidth,
                panelHeight,
                padding,
                mouseX,
                mouseY
        );
    }

    private void renderBenchmarkOverlay(class_332 context, int mouseX, int mouseY) {
        int overlayWidth = Math.min(360, Math.max(220, field_22789 - 24));
        int overlayHeight = 58;
        int x = (field_22789 - overlayWidth) / 2;
        int y = 12;

        context.method_25294(x, y, x + overlayWidth, y + overlayHeight, 0xD9101014);
        outline(context, x, y, overlayWidth, overlayHeight, ORANGE);

        context.method_25303(field_22793, "AUTO OPTIMIZER — MEASURING IN-WORLD", x + 10, y + 8, ORANGE);
        String status = session.status();
        context.method_51433(field_22793,
                field_22793.method_27523(status == null ? "Measuring..." : status, overlayWidth - 20),
                x + 10, y + 23, TEXT, true);

        int barX = x + 10;
        int barY = y + 39;
        int barWidth = overlayWidth - 96;
        context.method_25294(barX, barY, barX + barWidth, barY + 7, 0xFF2B2B31);
        context.method_25294(barX, barY, barX + Math.round(barWidth * session.progress()), barY + 7, ORANGE);

        ButtonBounds back = new ButtonBounds(x + overlayWidth - 76, y + 34, 66, 18);
        drawButton(context, back, "Back", back.contains(mouseX, mouseY), false);
    }

    private void renderHeader(
            class_332 context,
            int panelX,
            int panelY,
            int panelWidth,
            int padding
    ) {
        int maxTextWidth = Math.max(20, panelWidth - padding * 2);

        context.method_25303(
                field_22793,
                field_22793.method_27523(
                        "PERFORMANCE OPTIMIZER",
                        maxTextWidth
                ),
                panelX + padding,
                panelY + 15,
                ORANGE
        );

        String subtitle = panelWidth < 430
                ? "Measured groups. Exact undo."
                : "Measured groups. Exact undo. Compare in the same world.";

        context.method_51433(
                field_22793,
                field_22793.method_27523(
                        subtitle,
                        maxTextWidth
                ),
                panelX + padding,
                panelY + 32,
                MUTED,
                true
        );

        if (panelWidth < 430) {
            context.method_51433(
                    field_22793,
                    field_22793.method_27523(
                            "Frame pacing + practical FPS.",
                            maxTextWidth
                    ),
                    panelX + padding,
                    panelY + 45,
                    MUTED,
                    true
            );
        }
        int optionsY = panelY + (panelWidth < 430 ? 62 : 51);
        context.method_51433(field_22793, field_22793.method_27523(
                "Target FPS: " + targetLabel() + "  >",
                Math.max(12, panelWidth - padding * 2)),
                panelX + padding, optionsY, ORANGE, true);
        ButtonBounds profiler = new ButtonBounds(panelX + panelWidth - padding - 80,
                panelY + 10, 80, 22);
        drawButton(context, profiler, "Profiler  >", false, false);
    }

    private String targetLabel() {
        int value = TARGETS[targetIndex];
        return value == 0 ? "Unlimited" : Integer.toString(value);
    }

    private void renderMeasuredCard(class_332 context, int x, int y, int width) {
        card(context, x, y, width, 170, "AUTO OPTIMIZER");
        int row = y + 29;
        int contentWidth = Math.max(12, width - 24);
        String[] details = {
                "One adaptive optimizer path automatically applies the strongest safe settings.",
                "Uses matched warm-up + render-frame windows and rejects measured regressions.",
                "FPS cap is optional and defaults to Unlimited; benchmarks always measure uncapped performance."
        };
        for (String line : details) {
            context.method_51433(field_22793, field_22793.method_27523(line, contentWidth),
                    x + 12, row, MUTED, true);
            row += 14;
        }
        FrameTelemetry.Sample first = session == null ? null : session.before();
        FrameTelemetry.Sample last = session == null ? null : session.after();
        row += 5;
        int metricX = x + 12;
        int beforeX = metricX + Math.max(108, Math.min(180, contentWidth * 16 / 100));
        int afterX = metricX + Math.max(196, Math.min(320, contentWidth * 30 / 100));
        context.method_51433(field_22793, "Metric", metricX, row, ORANGE, true);
        context.method_51433(field_22793, "Before", beforeX, row, ORANGE, true);
        context.method_51433(field_22793, "After", afterX, row, ORANGE, true);
        row += 15;
        drawMetric(context, x + 12, row, contentWidth, "Average FPS", first, last, 0);
        row += 14;
        drawMetric(context, x + 12, row, contentWidth, "1% low FPS", first, last, 1);
        row += 14;
        drawMetric(context, x + 12, row, contentWidth, "Frame time", first, last, 2);
        row += 14;
        drawMetric(context, x + 12, row, contentWidth, "Major spikes", first, last, 3);
    }

    private void drawMetric(class_332 context, int x, int y, int width, String name,
                            FrameTelemetry.Sample first, FrameTelemetry.Sample last, int kind) {
        String beforeText = metric(first, kind);
        String afterText = metric(last, kind);
        int beforeX = x + Math.max(108, Math.min(180, width * 16 / 100));
        int afterX = x + Math.max(196, Math.min(320, width * 30 / 100));
        int metricWidth = Math.max(20, beforeX - x - 8);
        int beforeWidth = Math.max(20, afterX - beforeX - 8);
        int afterWidth = Math.max(20, width - (afterX - x));
        context.method_51433(field_22793, field_22793.method_27523(name + ":", metricWidth), x, y, TEXT, true);
        context.method_51433(field_22793, field_22793.method_27523(beforeText, beforeWidth), beforeX, y, TEXT, true);
        context.method_51433(field_22793, field_22793.method_27523(afterText, afterWidth), afterX, y, TEXT, true);
    }

    private String metric(FrameTelemetry.Sample value, int kind) {
        if (value == null || !value.available()) {
            return "N/A";
        }
        return switch (kind) {
            case 0 -> String.format(Locale.ROOT, "%.1f", value.averageFps());
            case 1 -> String.format(Locale.ROOT, "%.1f", value.onePercentLow());
            case 2 -> String.format(Locale.ROOT, "%.1f ms", value.averageMs());
            default -> Integer.toString(value.stutters());
        };
    }

    private void renderHardwareCard(
            class_332 context,
            int x,
            int y,
            int width,
            int height
    ) {
        card(
                context,
                x,
                y,
                width,
                height,
                "HARDWARE"
        );

        int row = y + 30;
        int rowWidth = Math.max(16, width - 24);

        drawKeyValue(
                context,
                "CPU threads",
                Integer.toString(hardware.logicalProcessors()),
                x + 12,
                row,
                rowWidth
        );
        row += 17;

        String memory = hardware.physicalMemoryMb() > 0
                ? mbToReadable(hardware.physicalMemoryMb())
                : "Unavailable";

        drawKeyValue(
                context,
                "System memory",
                memory,
                x + 12,
                row,
                rowWidth
        );
        row += 17;

        drawKeyValue(
                context,
                "JVM max",
                mbToReadable(hardware.jvmMaxMemoryMb()),
                x + 12,
                row,
                rowWidth
        );
        row += 17;

        String gpu = hardware.gpuRenderer();
        if (gpu == null || gpu.isBlank() || "Unavailable".equals(gpu)) {
            gpu = hardware.gpuVendor();
        }

        drawKeyValueWrapped(
                context,
                "GPU",
                gpu == null || gpu.isBlank() ? "Unavailable" : gpu,
                x + 12,
                row,
                rowWidth
        );
    }

    private void renderCompatibilityCard(
            class_332 context,
            int x,
            int y,
            int width,
            int height,
            boolean twoColumns
    ) {
        card(
                context,
                x,
                y,
                width,
                height,
                "COMPATIBILITY"
        );

        List<ModState> mods = compatibilityStates();
        int startY = y + 30;

        if (twoColumns) {
            int colWidth = Math.max(
                    20,
                    (width - 24) / 2
            );
            int rows = (mods.size() + 1) / 2;

            for (int i = 0; i < mods.size(); i++) {
                int col = i / rows;
                int row = i % rows;

                drawMod(
                        context,
                        mods.get(i),
                        x + 12 + col * colWidth,
                        startY + row * 14,
                        Math.max(16, colWidth - 6)
                );
            }
        } else {
            for (int i = 0; i < mods.size(); i++) {
                drawMod(
                        context,
                        mods.get(i),
                        x + 12,
                        startY + i * 14,
                        Math.max(16, width - 24)
                );
            }
        }
    }

    private int calculateImpactHeight() {
        int height = 68;

        if (impactPreview == null || impactPreview.changes().isEmpty()) {
            return height + 24;
        }

        for (String category : IMPACT_CATEGORIES) {
            int count = categoryChanges(category).size();

            if (count > 0) {
                height += 17 + count * 28;
            }
        }

        return height + 8;
    }

    private void renderImpactCard(
            class_332 context,
            int x,
            int y,
            int width,
            int height
    ) {
        card(
                context,
                x,
                y,
                width,
                height,
                "AUTO OPTIMIZER IMPACT PREVIEW"
        );

        int contentX = x + 12;
        int contentWidth = Math.max(16, width - 24);
        int row = y + 30;

        int minecraftCount = impactPreview == null
                ? 0
                : impactPreview.minecraftSettingsChanging();
        int yogiCount = impactPreview == null
                ? 0
                : impactPreview.yogiSettingsChanging();

        String counts = minecraftCount
                + " Minecraft setting"
                + (minecraftCount == 1 ? "" : "s")
                + " changing  •  "
                + yogiCount
                + " Yogi Essentials module"
                + (yogiCount == 1 ? "" : "s")
                + " changing";

        context.method_51433(
                field_22793,
                field_22793.method_27523(counts, contentWidth),
                contentX,
                row,
                ORANGE,
                true
        );
        row += 14;

        context.method_51433(
                field_22793,
                field_22793.method_27523(
                        "Aggressive safe targets are shown below; final choices are measured in-world.",
                        contentWidth
                ),
                contentX,
                row,
                MUTED,
                true
        );
        row += 18;

        if (impactPreview == null || impactPreview.changes().isEmpty()) {
            context.method_51433(
                    field_22793,
                    field_22793.method_27523(
                        "Current settings already match the optimizer's static targets.",
                            contentWidth
                    ),
                    contentX,
                    row,
                    GREEN,
                    true
            );
            return;
        }

        for (String category : IMPACT_CATEGORIES) {
            List<ImpactChange> categoryChanges = categoryChanges(category);

            if (categoryChanges.isEmpty()) {
                continue;
            }

            context.method_25303(
                    field_22793,
                    category.toUpperCase(),
                    contentX,
                    row,
                    TEXT
            );
            row += 14;

            for (ImpactChange change : categoryChanges) {
                String prefix = change.yogiSetting()
                        ? "Yogi Essentials • "
                        : "";
                String values = prefix
                        + change.setting()
                        + ": "
                        + change.currentValue()
                        + " -> "
                        + change.optimizedValue();

                context.method_51433(
                        field_22793,
                        field_22793.method_27523(values, contentWidth),
                        contentX + 4,
                        row,
                        TEXT,
                        true
                );

                context.method_51433(
                        field_22793,
                        field_22793.method_27523(
                                change.impact(),
                                Math.max(8, contentWidth - 8)
                        ),
                        contentX + 8,
                        row + 12,
                        MUTED,
                        true
                );

                row += 28;
            }

            row += 3;
        }
    }

    private int calculateProgressHeight(
            int width
    ) {
        int base = width < 430
                ? 124
                : 112;

        if (result == null || result.changes().isEmpty()) {
            return base;
        }

        return base + (viewAllChanges ? result.changes().size() * 12 + 40 : 26);
    }

    private void renderProgressCard(
            class_332 context,
            int x,
            int y,
            int width,
            int height
    ) {
        card(
                context,
                x,
                y,
                width,
                height,
                optimizing
                        ? "APPLYING OPTIMIZATION"
                        : "OPTIMIZER STATUS"
        );

        int contentX = x + 12;
        int contentWidth = Math.max(16, width - 24);
        int row = y + 30;

        if (optimizing) {
            int stageIndex = Math.min(
                    STAGES.length - 1,
                    completedStages
            );

            context.method_51433(
                    field_22793,
                    field_22793.method_27523(
                            session == null ? STAGES[stageIndex] : session.status(),
                            contentWidth
                    ),
                    contentX,
                    row,
                    ORANGE,
                    true
            );
            row += 16;
        } else if (result != null) {
            context.method_25303(
                    field_22793,
                    field_22793.method_27523(
                            result.message(),
                            contentWidth
                    ),
                    contentX,
                    row,
                    result.success() ? GREEN : RED
            );
            row += 16;
        } else {
            context.method_51433(
                    field_22793,
                    field_22793.method_27523(
                            "Ready. Review the preview above before applying.",
                            contentWidth
                    ),
                    contentX,
                    row,
                    MUTED,
                    true
            );
            row += 16;
        }

        if (result != null && !result.changes().isEmpty()) {
            changesButton = new ButtonBounds(contentX, row, 112, 16);
            context.method_25303(field_22793,
                    viewAllChanges ? "Hide Changes  ^" : "View Changes  v",
                    contentX, row, ORANGE);
            row += 17;
            if (viewAllChanges) {
                context.method_51433(field_22793, "Applied changes (" + result.changes().size() + "):",
                        contentX, row, GREEN, true);
                row += 14;
                for (int i = 0; i < result.changes().size(); i++) {
                    context.method_51433(
                            field_22793,
                            field_22793.method_27523("• " + result.changes().get(i), contentWidth),
                            contentX,
                            row,
                            MUTED,
                            true
                    );
                    row += 12;
                }
            }
        }
    }

    private void renderBottomButtons(
            class_332 context,
            int panelX,
            int panelY,
            int panelWidth,
            int panelHeight,
            int padding,
            int mouseX,
            int mouseY
    ) {
        ButtonBounds back = backButton(
                panelX,
                panelY,
                panelWidth,
                panelHeight,
                padding
        );
        ButtonBounds optimize = optimizeButton(
                panelX,
                panelY,
                panelWidth,
                panelHeight,
                padding
        );
        ButtonBounds restore = restoreButton(
                panelX,
                panelY,
                panelWidth,
                panelHeight,
                padding
        );

        drawButton(
                context,
                back,
                result != null && result.success() ? "Done" : "Back",
                back.contains(mouseX, mouseY),
                false
        );

        boolean alreadyOptimized = PerformanceOptimizer.isOptimizationActive();
        String optimizeLabel = optimizing
                ? "Optimizing..."
                : alreadyOptimized
                ? "Re-optimize"
                : result != null ? "Run Again" : "Auto Optimize";

        drawButton(
                context,
                optimize,
                optimizeLabel,
                !optimizing && optimize.contains(mouseX, mouseY),
                !optimizing
        );

        if (restore != null) {
            drawButton(
                    context,
                    restore,
                    "Undo Optimization",
                    restore.contains(mouseX, mouseY),
                    false
            );
        }
    }

    @Override
    public boolean method_25402(
            class_11909 click,
            boolean doubled
    ) {
        if (click.method_74245() != 0) {
            return super.method_25402(click, doubled);
        }

        if (optimizing && session != null && !session.finished()) {
            int overlayWidth = Math.min(360, Math.max(220, field_22789 - 24));
            int x = (field_22789 - overlayWidth) / 2;
            int y = 12;
            ButtonBounds benchmarkBack = new ButtonBounds(x + overlayWidth - 76, y + 34, 66, 18);
            if (benchmarkBack.contains(click.comp_4798(), click.comp_4799())) {
                UiSoundManager.click();
                session.cancel();
                optimizing = false;
                result = session.result();
                refreshPreview();
                return true;
            }
            return true;
        }

        int panelWidth = Math.max(
                1,
                Math.min(TARGET_WIDTH, field_22789 - 12)
        );
        int panelHeight = Math.max(
                1,
                Math.min(TARGET_HEIGHT, field_22790 - 12)
        );
        int panelX = (field_22789 - panelWidth) / 2;
        int panelY = (field_22790 - panelHeight) / 2;
        int padding = panelWidth < 420 ? 8 : 14;
        if (result != null && changesButton != null
                && click.comp_4799() >= viewportTop && click.comp_4799() < viewportBottom
                && changesButton.contains(click.comp_4798(), click.comp_4799())) {
            viewAllChanges = !viewAllChanges;
            return true;
        }

        ButtonBounds back = backButton(
                panelX,
                panelY,
                panelWidth,
                panelHeight,
                padding
        );

        if (back.contains(click.comp_4798(), click.comp_4799())) {
            UiSoundManager.click();
            if (session != null && !session.finished()) {
                session.cancel();
            }

            if (field_22787 != null) {
                field_22787.method_1507(parent);
            }

            return true;
        }

        if (!optimizing && click.comp_4799() >= panelY + 10 && click.comp_4799() < panelY + 32
                && click.comp_4798() >= panelX + panelWidth - padding - 80
                && click.comp_4798() < panelX + panelWidth - padding) {
            if (field_22787 != null) {
                field_22787.method_1507(new PerformanceProfilerScreen(this));
            }
            return true;
        }

        int optionY = panelY + (panelWidth < 430 ? 62 : 51);
        if (!optimizing && click.comp_4799() >= optionY - 3 && click.comp_4799() < optionY + 15
                && click.comp_4798() >= panelX + padding
                && click.comp_4798() < panelX + panelWidth - padding) {
            targetIndex = (targetIndex + 1) % TARGETS.length;
            PerformanceOptimizer.setSelectedFrameTarget(TARGETS[targetIndex]);
            ConfigManager.save();
            return true;
        }

        ButtonBounds optimize = optimizeButton(
                panelX,
                panelY,
                panelWidth,
                panelHeight,
                padding
        );

        if (!optimizing && optimize.contains(click.comp_4798(), click.comp_4799())) {
            UiSoundManager.click();
            session = null;
            result = null;
            completedStages = 0;
            stageTicks = 0;
            scrollOffset = 0;
            startOptimization();
            return true;
        }

        ButtonBounds restore = restoreButton(
                panelX,
                panelY,
                panelWidth,
                panelHeight,
                padding
        );

        if (!optimizing
                && restore != null
                && restore.contains(click.comp_4798(), click.comp_4799())) {
            UiSoundManager.click();

            result = PerformanceOptimizer.restorePreviousSettings();
            session = null;
            completedStages = 0;
            stageTicks = 0;
            optimizing = false;
            scrollOffset = 0;
            refreshPreview();

            return true;
        }

        return super.method_25402(click, doubled);
    }

    @Override
    public boolean method_25401(
            double mouseX,
            double mouseY,
            double horizontalAmount,
            double verticalAmount
    ) {
        if (mouseY < viewportTop || mouseY > viewportBottom) {
            return super.method_25401(
                    mouseX,
                    mouseY,
                    horizontalAmount,
                    verticalAmount
            );
        }

        scrollOffset -= (int) Math.round(
                verticalAmount * SCROLL_STEP
        );
        clampScroll();
        return true;
    }

    @Override
    public void method_25419() {
        if (session != null && !session.finished()) {
            session.cancel();
        }
        if (field_22787 != null) {
            field_22787.method_1507(parent);
        }
    }

    @Override
    public boolean method_25421() {
        return false;
    }

    private void startOptimization() {
        int target = TARGETS[targetIndex];
        optimizerWarning = null;
        session = new AdaptiveOptimizer(target);
        viewAllChanges = false;
        optimizing = !session.finished();
        completedStages = 0;
        scrollOffset = 0;
        result = session.finished() ? session.result() : null;
    }

    private void refreshPreview() {
        hardware = PerformanceOptimizer.detectHardware();
        compatibility = PerformanceOptimizer.detectCompatibility();
        impactPreview = PerformanceOptimizer.previewAutoOptimizerImpact();
    }

    private List<ImpactChange> categoryChanges(
            String category
    ) {
        List<ImpactChange> result = new ArrayList<>();

        if (impactPreview == null) {
            return result;
        }

        for (ImpactChange change : impactPreview.changes()) {
            if (category.equals(change.category())) {
                result.add(change);
            }
        }

        return result;
    }

    private List<ModState> compatibilityStates() {
        return List.of(
                new ModState("Sodium", compatibility.sodium()),
                new ModState("Sodium Extra", compatibility.sodiumExtra()),
                new ModState("Reese's Options", compatibility.reesesSodiumOptions()),
                new ModState("Lithium", compatibility.lithium()),
                new ModState("ImmediatelyFast", compatibility.immediatelyFast()),
                new ModState("MoreCulling", compatibility.moreCulling()),
                new ModState("BadOptimizations", compatibility.badOptimizations()),
                new ModState("EntityCulling", compatibility.entityCulling()),
                new ModState("Dynamic FPS", compatibility.dynamicFps()),
                new ModState("FerriteCore", compatibility.ferriteCore()),
                new ModState("C2ME", compatibility.c2me()),
                new ModState("Nvidium", compatibility.nvidium()),
                new ModState("VulkanMod", compatibility.vulkanMod()),
                new ModState("Exordium", compatibility.exordium())
        );
    }

    private void clampScroll() {
        int viewportHeight = Math.max(
                1,
                viewportBottom - viewportTop - 16
        );
        int maxScroll = Math.max(
                0,
                contentHeight - viewportHeight
        );

        scrollOffset = Math.max(
                0,
                Math.min(maxScroll, scrollOffset)
        );
    }

    private void renderOptimizerWarning(class_332 context, int x, int y, int width, int height) {
        context.method_25294(x, y, x + width, y + height, 0xE02A1111);
        outline(context, x, y, width, height, RED);
        context.method_25303(
                field_22793,
                field_22793.method_27523(optimizerWarning, Math.max(8, width - 12)),
                x + 6,
                y + 5,
                RED
        );
    }

    private void renderScrollbar(
            class_332 context,
            int x,
            int top,
            int bottom
    ) {
        int viewportHeight = Math.max(
                1,
                bottom - top
        );

        if (contentHeight <= viewportHeight) {
            return;
        }

        int thumbHeight = Math.max(
                18,
                Math.round(
                        viewportHeight
                                * (viewportHeight / (float) contentHeight)
                )
        );
        int maxTrack = Math.max(
                1,
                viewportHeight - thumbHeight
        );
        int maxScroll = Math.max(
                1,
                contentHeight - viewportHeight
        );
        int thumbY = top + Math.round(
                maxTrack * (scrollOffset / (float) maxScroll)
        );

        context.method_25294(
                x,
                top,
                x + 2,
                bottom,
                0x66303038
        );
        context.method_25294(
                x,
                thumbY,
                x + 2,
                thumbY + thumbHeight,
                ORANGE
        );
    }

    private ButtonBounds backButton(
            int panelX,
            int panelY,
            int panelWidth,
            int panelHeight,
            int padding
    ) {
        int buttonY = panelY + panelHeight - 38;

        if (restoreStacks(panelWidth)) {
            buttonY = panelY + panelHeight - 64;
        }

        return new ButtonBounds(
                panelX + padding,
                buttonY,
                Math.min(82, Math.max(58, panelWidth / 4)),
                26
        );
    }

    private ButtonBounds optimizeButton(
            int panelX,
            int panelY,
            int panelWidth,
            int panelHeight,
            int padding
    ) {
        int optimizeWidth = Math.min(
                154,
                Math.max(96, panelWidth / 3)
        );
        int buttonY = panelY + panelHeight - 38;

        if (restoreStacks(panelWidth)) {
            buttonY = panelY + panelHeight - 64;
        }

        return new ButtonBounds(
                panelX + panelWidth - padding - optimizeWidth,
                buttonY,
                optimizeWidth,
                26
        );
    }

    private ButtonBounds restoreButton(
            int panelX,
            int panelY,
            int panelWidth,
            int panelHeight,
            int padding
    ) {
        if (!PerformanceOptimizer.hasRestoreSnapshot()) {
            return null;
        }

        if (restoreStacks(panelWidth)) {
            int width = Math.max(
                    120,
                    panelWidth - padding * 2
            );

            return new ButtonBounds(
                    panelX + padding,
                    panelY + panelHeight - 34,
                    width,
                    24
            );
        }

        ButtonBounds optimize = optimizeButton(
                panelX,
                panelY,
                panelWidth,
                panelHeight,
                padding
        );
        int gap = 8;
        int available = Math.max(
                70,
                optimize.x() - (panelX + padding) - gap - 82 - gap
        );
        int restoreWidth = Math.min(
                144,
                available
        );
        int restoreX = optimize.x() - gap - restoreWidth;

        return new ButtonBounds(
                restoreX,
                optimize.y(),
                restoreWidth,
                optimize.height()
        );
    }

    private boolean restoreStacks(
            int panelWidth
    ) {
        return PerformanceOptimizer.hasRestoreSnapshot()
                && panelWidth < 520;
    }

    private void card(
            class_332 context,
            int x,
            int y,
            int width,
            int height,
            String title
    ) {
        context.method_25294(
                x,
                y,
                x + width,
                y + height,
                CARD
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
                field_22793.method_27523(
                        title,
                        Math.max(8, width - 24)
                ),
                x + 12,
                y + 10,
                ORANGE
        );
    }

    private void drawKeyValue(
            class_332 context,
            String key,
            String value,
            int x,
            int y,
            int width
    ) {
        int gap = 8;
        int preferredKeyWidth = Math.max(58, Math.min(92, width / 3));
        int keyWidth = Math.min(preferredKeyWidth, Math.max(8, width - 36));
        int valueWidth = Math.max(20, width - keyWidth - gap);

        String safeKey = field_22793.method_27523(
                key,
                keyWidth
        );

        context.method_51433(
                field_22793,
                safeKey,
                x,
                y,
                MUTED,
                true
        );
        drawRightAlignedFittedText(
                context,
                value,
                x + keyWidth + gap,
                y,
                valueWidth,
                TEXT
        );
    }

    private void drawKeyValueWrapped(
            class_332 context,
            String key,
            String value,
            int x,
            int y,
            int width
    ) {
        int gap = 8;
        int preferredKeyWidth = Math.max(58, Math.min(92, width / 3));
        int keyWidth = Math.min(preferredKeyWidth, Math.max(8, width - 36));
        int valueWidth = Math.max(20, width - keyWidth - gap);
        String safe = value == null ? "" : value;

        context.method_51433(
                field_22793,
                field_22793.method_27523(key, keyWidth),
                x,
                y,
                MUTED,
                true
        );

        if (field_22793.method_1727(safe) <= valueWidth) {
            int valueX = x + width - field_22793.method_1727(safe);
            context.method_51433(field_22793, safe, valueX, y, TEXT, true);
            return;
        }

        
        
        java.util.List<String> lines = wrapPlainText(safe, width);
        int lineY = y + 12;
        for (int i = 0; i < lines.size() && i < 3; i++) {
            String line = lines.get(i);
            context.method_51433(
                    field_22793,
                    line,
                    x + width - field_22793.method_1727(line),
                    lineY + i * 10,
                    TEXT,
                    true
            );
        }
    }

    private java.util.List<String> wrapPlainText(String text, int maxWidth) {
        java.util.List<String> lines = new java.util.ArrayList<>();
        String[] words = text.trim().split("\\s+");
        StringBuilder current = new StringBuilder();

        for (String word : words) {
            String candidate = current.length() == 0
                    ? word
                    : current + " " + word;

            if (field_22793.method_1727(candidate) <= maxWidth) {
                current.setLength(0);
                current.append(candidate);
                continue;
            }

            if (current.length() > 0) {
                lines.add(current.toString());
                current.setLength(0);
            }

            if (field_22793.method_1727(word) <= maxWidth) {
                current.append(word);
                continue;
            }

            
            
            StringBuilder chunk = new StringBuilder();
            for (int i = 0; i < word.length(); i++) {
                char c = word.charAt(i);
                if (chunk.length() > 0
                        && field_22793.method_1727(chunk.toString() + c) > maxWidth) {
                    lines.add(chunk.toString());
                    chunk.setLength(0);
                }
                chunk.append(c);
            }
            current.append(chunk);
        }

        if (current.length() > 0) {
            lines.add(current.toString());
        }

        return lines;
    }

    private void drawRightAlignedFittedText(
            class_332 context,
            String value,
            int x,
            int y,
            int maxWidth,
            int color
    ) {
        String safe = value == null ? "" : value;
        int rawWidth = Math.max(1, field_22793.method_1727(safe));
        float scale = Math.min(1.0F, maxWidth / (float) rawWidth);
        
        
        scale = Math.max(0.10F, scale);
        int scaledWidth = Math.round(rawWidth * scale);

        var matrices = context.method_51448();
        matrices.pushMatrix();
        matrices.translate(x + maxWidth - scaledWidth, y);
        matrices.scale(scale, scale);
        context.method_51433(
                field_22793,
                safe,
                0,
                0,
                color,
                true
        );
        matrices.popMatrix();
    }

    private void drawMod(
            class_332 context,
            ModState mod,
            int x,
            int y,
            int width
    ) {
        String status = mod.loaded()
                ? "INSTALLED"
                : "NOT INSTALLED";
        int statusColor = mod.loaded()
                ? GREEN
                : MUTED;
        int statusWidth = field_22793.method_1727(status);
        int nameWidth = Math.max(
                8,
                width - statusWidth - 8
        );

        context.method_51433(
                field_22793,
                field_22793.method_27523(
                        mod.name(),
                        nameWidth
                ),
                x,
                y,
                TEXT,
                true
        );
        context.method_51433(
                field_22793,
                status,
                x + width - statusWidth,
                y,
                statusColor,
                true
        );
    }

    private void drawButton(
            class_332 context,
            ButtonBounds bounds,
            String label,
            boolean hovered,
            boolean primary
    ) {
        int fill = primary
                ? hovered
                ? 0xFFFF7A1A
                : ORANGE
                : hovered
                ? 0xFF2C2C34
                : 0xFF202027;
        int border = primary
                ? ORANGE
                : BORDER;

        context.method_25294(
                bounds.x(),
                bounds.y(),
                bounds.x() + bounds.width(),
                bounds.y() + bounds.height(),
                fill
        );
        outline(
                context,
                bounds.x(),
                bounds.y(),
                bounds.width(),
                bounds.height(),
                border
        );

        String safe = field_22793.method_27523(
                label,
                Math.max(8, bounds.width() - 10)
        );
        int textX = bounds.x()
                + (bounds.width() - field_22793.method_1727(safe)) / 2;
        int textY = bounds.y()
                + (bounds.height() - field_22793.field_2000) / 2;

        context.method_51433(
                field_22793,
                safe,
                textX,
                textY,
                TEXT,
                true
        );
    }

    private String truncate(
            String value,
            int maxWidth
    ) {
        if (value == null) {
            return "Unavailable";
        }

        return field_22793.method_27523(
                value,
                Math.max(8, maxWidth)
        );
    }

    private static String mbToReadable(
            long mb
    ) {
        if (mb <= 0) {
            return "Unavailable";
        }

        if (mb >= 1024) {
            double gb = mb / 1024.0;

            return String.format(
                    java.util.Locale.ROOT,
                    "%.1f GB",
                    gb
            );
        }

        return mb + " MB";
    }

    private static void outline(
            class_332 context,
            int x,
            int y,
            int width,
            int height,
            int color
    ) {
        context.method_25294(x, y, x + width, y + 1, color);
        context.method_25294(x, y + height - 1, x + width, y + height, color);
        context.method_25294(x, y, x + 1, y + height, color);
        context.method_25294(x + width - 1, y, x + width, y + height, color);
    }

    private record ModState(
            String name,
            boolean loaded
    ) {
    }

    private record ButtonBounds(
            int x,
            int y,
            int width,
            int height
    ) {
        private boolean contains(
                double mouseX,
                double mouseY
        ) {
            return mouseX >= x
                    && mouseX < x + width
                    && mouseY >= y
                    && mouseY < y + height;
        }
    }
}
