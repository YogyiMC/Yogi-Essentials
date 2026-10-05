package dev.yogi.yogiessentials.client.gui;

import dev.yogi.yogiessentials.client.module.performance.PerformanceOptimizer;
import dev.yogi.yogiessentials.client.module.performance.PerformanceOptimizer.HardwareInfo;
import dev.yogi.yogiessentials.client.module.performance.PerformanceTelemetry;
import dev.yogi.yogiessentials.client.module.performance.PerformanceTelemetry.Snapshot;
import dev.yogi.yogiessentials.client.util.UiSoundManager;
import java.util.List;
import java.util.Locale;
import net.minecraft.class_11909;
import net.minecraft.class_2561;
import net.minecraft.class_332;
import net.minecraft.class_437;
import net.minecraft.class_5481;

/** Responsive live dashboard shared by the Auto Optimizer. */
public final class PerformanceDashboardScreen extends class_437 {

    private static final int ORANGE = 0xFFFF6A00;
    private static final int PANEL = 0xF20D0D10;
    private static final int CARD = 0xF016161B;
    private static final int BORDER = 0xFF303038;
    private static final int TEXT = 0xFFF5F5F5;
    private static final int MUTED = 0xFF9A9AA2;
    private static final int GREEN = 0xFF68D391;

    private static final int TARGET_WIDTH = 790;
    private static final int TARGET_HEIGHT = 520;
    private static final int GAP = 10;

    private final class_437 parent;
    private final HardwareInfo hardware;

    private Snapshot snapshot;
    private long lastSnapshotMillis;
    private int scrollOffset;
    private int contentHeight;
    private int viewportTop;
    private int viewportBottom;

    public PerformanceDashboardScreen(class_437 parent) {
        super(class_2561.method_43470("Performance Dashboard"));
        this.parent = parent;
        this.hardware = PerformanceOptimizer.detectHardware();
        this.snapshot = PerformanceTelemetry.snapshot();
    }

    @Override
    public void method_25394(class_332 context, int mouseX, int mouseY, float deltaTicks) {
        long now = System.currentTimeMillis();
        if (now - lastSnapshotMillis >= 250L) {
            snapshot = PerformanceTelemetry.snapshot();
            lastSnapshotMillis = now;
        }

        context.method_25294(0, 0, field_22789, field_22790, 0x76000000);

        int panelWidth = Math.max(1, Math.min(TARGET_WIDTH, field_22789 - 12));
        int panelHeight = Math.max(1, Math.min(TARGET_HEIGHT, field_22790 - 12));
        int panelX = (field_22789 - panelWidth) / 2;
        int panelY = (field_22790 - panelHeight) / 2;
        int padding = panelWidth < 430 ? 8 : 14;

        context.method_25294(panelX, panelY, panelX + panelWidth, panelY + panelHeight, PANEL);
        outline(context, panelX, panelY, panelWidth, panelHeight, ORANGE);
        context.method_25294(panelX, panelY, panelX + panelWidth, panelY + 3, ORANGE);

        int maxTitleWidth = Math.max(20, panelWidth - padding * 2 - 110);
        context.method_25303(field_22793,
                field_22793.method_27523("PERFORMANCE DASHBOARD", maxTitleWidth),
                panelX + padding, panelY + 15, ORANGE);
        context.method_51433(field_22793,
                field_22793.method_27523(
                        "Live real-frame data. Values update four times per second to avoid adding stutter.",
                        Math.max(20, panelWidth - padding * 2)),
                panelX + padding, panelY + 32, MUTED, true);

        ButtonBounds back = new ButtonBounds(
                panelX + panelWidth - padding - 82, panelY + 12, 82, 24);
        drawButton(context, back, "Back", back.contains(mouseX, mouseY));

        viewportTop = panelY + 54;
        viewportBottom = panelY + panelHeight - 42;
        int innerX = panelX + padding;
        int innerWidth = Math.max(20, panelWidth - padding * 2);
        int y = viewportTop + 8 - scrollOffset;
        boolean wide = innerWidth >= 600;

        context.method_44379(panelX + 1, viewportTop,
                panelX + panelWidth - 1, viewportBottom);

        if (wide) {
            int cardWidth = (innerWidth - GAP) / 2;
            int cardHeight = 166;
            drawFrameCard(context, innerX, y, cardWidth, cardHeight);
            drawSystemCard(context, innerX + cardWidth + GAP, y, cardWidth, cardHeight);
            y += cardHeight + GAP;
            int detailHeight = Math.max(cardHeight, bottleneckCardHeight(cardWidth));
            drawRenderCard(context, innerX, y, cardWidth, detailHeight);
            drawBottleneckCard(context, innerX + cardWidth + GAP, y, cardWidth, detailHeight);
            y += detailHeight + GAP;
        } else {
            int cardHeight = 166;
            drawFrameCard(context, innerX, y, innerWidth, cardHeight);
            y += cardHeight + GAP;
            drawSystemCard(context, innerX, y, innerWidth, cardHeight);
            y += cardHeight + GAP;
            drawRenderCard(context, innerX, y, innerWidth, cardHeight);
            y += cardHeight + GAP;
            int bottleneckHeight = bottleneckCardHeight(innerWidth);
            drawBottleneckCard(context, innerX, y, innerWidth, bottleneckHeight);
            y += bottleneckHeight + GAP;
        }

        contentHeight = Math.max(0, y - (viewportTop + 8 - scrollOffset));
        context.method_44380();
        clampScroll();
        renderScrollbar(context, panelX + panelWidth - 4);

        ButtonBounds reset = new ButtonBounds(
                panelX + padding, panelY + panelHeight - 34, 112, 24);
        drawButton(context, reset, "Reset samples", reset.contains(mouseX, mouseY));
    }

    private void drawFrameCard(class_332 context, int x, int y, int width, int height) {
        card(context, x, y, width, height, "FRAME HEALTH");
        int row = y + 30;
        int rowWidth = Math.max(20, width - 24);
        row = keyValue(context, "Current FPS", Integer.toString(snapshot.currentFps()), x + 12, row, rowWidth);
        row = keyValue(context, "Average FPS", number(snapshot.averageFps()), x + 12, row, rowWidth);
        row = keyValue(context, "Minimum FPS", number(snapshot.minimumFps()), x + 12, row, rowWidth);
        row = keyValue(context, "1% Low", number(snapshot.onePercentLowFps()), x + 12, row, rowWidth);
        row = keyValue(context, "Frame Time", number(snapshot.averageFrameTimeMs()) + " ms", x + 12, row, rowWidth);
        row = keyValue(context, "Variance", number(snapshot.frameTimeVariance()) + " ms²", x + 12, row, rowWidth);
        row = keyValue(context, "Stutters / Major", snapshot.stutters() + " / " + snapshot.majorStutters(), x + 12, row, rowWidth);
        keyValue(context, "Samples", Integer.toString(snapshot.samples()), x + 12, row, rowWidth);
    }

    private void drawSystemCard(class_332 context, int x, int y, int width, int height) {
        card(context, x, y, width, height, "SYSTEM / MEMORY");
        int row = y + 30;
        int rowWidth = Math.max(20, width - 24);
        row = keyValue(context, "CPU", hardware.cpuName(), x + 12, row, rowWidth);
        row = keyValue(context, "CPU threads", Integer.toString(hardware.logicalProcessors()), x + 12, row, rowWidth);
        row = keyValue(context, "GPU", hardware.gpuRenderer(), x + 12, row, rowWidth);
        row = keyValue(context, "Physical RAM", memory(hardware.physicalMemoryMb()), x + 12, row, rowWidth);
        row = keyValue(context, "Game RAM limit", memory(snapshot.gameMemoryLimitMb()), x + 12, row, rowWidth);
        row = keyValue(context, "Heap used", memory(snapshot.heapUsedMb()), x + 12, row, rowWidth);
        row = keyValue(context, "Heap committed", memory(snapshot.heapCommittedMb()), x + 12, row, rowWidth);
        keyValue(context, "Resolution", snapshot.resolution(), x + 12, row, rowWidth);
    }

    private void drawRenderCard(class_332 context, int x, int y, int width, int height) {
        card(context, x, y, width, height, "RENDER WORKLOAD");
        int row = y + 30;
        int rowWidth = Math.max(20, width - 24);
        row = keyValue(context, "Render Distance", snapshot.renderDistance() + " chunks", x + 12, row, rowWidth);
        row = keyValue(context, "Simulation Distance", snapshot.simulationDistance() + " chunks", x + 12, row, rowWidth);
        row = keyValue(context, "Entity Distance", number(snapshot.entityDistance()) + "x", x + 12, row, rowWidth);
        String fpsLimit = snapshot.fpsLimit() >= 260 ? "Unlimited" : Integer.toString(snapshot.fpsLimit());
        row = keyValue(context, "User FPS Limit", fpsLimit, x + 12, row, rowWidth);
        context.method_51433(field_22793,
                field_22793.method_27523("Yogi Essentials never changes the FPS limit.", rowWidth),
                x + 12, row + 5, GREEN, true);
        context.method_51433(field_22793,
                field_22793.method_27523("At 16+ chunks, compare 1% lows while moving through terrain.", rowWidth),
                x + 12, row + 23, MUTED, true);
    }

    private void drawBottleneckCard(class_332 context, int x, int y, int width, int height) {
        card(context, x, y, width, height, "DETECTED BOTTLENECKS");
        int textWidth = Math.max(20, width - 24);
        List<class_5481> bottleneckLines = field_22793.method_1728(
                class_2561.method_43470(snapshot.bottleneck()), textWidth);
        for (int i = 0; i < bottleneckLines.size(); i++) {
            context.method_51430(field_22793, bottleneckLines.get(i),
                    x + 12, y + 32 + i * 12, ORANGE, true);
        }

        List<class_5481> lines = field_22793.method_1728(
                class_2561.method_43470(snapshot.recommendation()), textWidth);
        int row = y + 38 + bottleneckLines.size() * 12;
        for (int i = 0; i < lines.size(); i++) {
            context.method_51430(field_22793, lines.get(i), x + 12, row + i * 12, TEXT, true);
        }
    }

    private int bottleneckCardHeight(int width) {
        int textWidth = Math.max(20, width - 24);
        int bottleneckLines = field_22793.method_1728(
                class_2561.method_43470(snapshot.bottleneck()), textWidth).size();
        int recommendationLines = field_22793.method_1728(
                class_2561.method_43470(snapshot.recommendation()), textWidth).size();
        return Math.max(166,
                50 + bottleneckLines * 12 + recommendationLines * 12);
    }

    @Override
    public boolean method_25402(class_11909 click, boolean doubled) {
        if (click.method_74245() != 0) {
            return super.method_25402(click, doubled);
        }

        int panelWidth = Math.max(1, Math.min(TARGET_WIDTH, field_22789 - 12));
        int panelHeight = Math.max(1, Math.min(TARGET_HEIGHT, field_22790 - 12));
        int panelX = (field_22789 - panelWidth) / 2;
        int panelY = (field_22790 - panelHeight) / 2;
        int padding = panelWidth < 430 ? 8 : 14;

        ButtonBounds back = new ButtonBounds(
                panelX + panelWidth - padding - 82, panelY + 12, 82, 24);
        if (back.contains(click.comp_4798(), click.comp_4799())) {
            UiSoundManager.click();
            if (field_22787 != null) {
                field_22787.method_1507(parent);
            }
            return true;
        }

        ButtonBounds reset = new ButtonBounds(
                panelX + padding, panelY + panelHeight - 34, 112, 24);
        if (reset.contains(click.comp_4798(), click.comp_4799())) {
            UiSoundManager.click();
            PerformanceTelemetry.reset();
            snapshot = PerformanceTelemetry.snapshot();
            scrollOffset = 0;
            return true;
        }
        return super.method_25402(click, doubled);
    }

    @Override
    public boolean method_25401(double mouseX, double mouseY,
                                 double horizontalAmount, double verticalAmount) {
        if (mouseY < viewportTop || mouseY > viewportBottom) {
            return super.method_25401(mouseX, mouseY, horizontalAmount, verticalAmount);
        }
        scrollOffset -= (int) Math.round(verticalAmount * 42.0);
        clampScroll();
        return true;
    }

    @Override
    public void method_25419() {
        if (field_22787 != null) {
            field_22787.method_1507(parent);
        }
    }

    @Override
    public boolean method_25421() {
        return false;
    }

    private int keyValue(class_332 context, String key, String value,
                         int x, int y, int width) {
        int keyWidth = Math.max(50, Math.min(100, width / 3));
        int gap = 8;
        int valueX = x + keyWidth + gap;
        int valueWidth = Math.max(16, width - keyWidth - gap);
        context.method_51433(field_22793, field_22793.method_27523(key, keyWidth),
                x, y, MUTED, true);
        drawRightFitted(context, value, valueX, y, valueWidth, TEXT);
        return y + 16;
    }

    private void drawRightFitted(class_332 context, String value,
                                 int x, int y, int maxWidth, int color) {
        String safe = value == null || value.isBlank() ? "Unavailable" : value;
        int rawWidth = Math.max(1, field_22793.method_1727(safe));
        float scale = Math.max(0.18F, Math.min(1.0F, maxWidth / (float) rawWidth));
        int scaledWidth = Math.round(rawWidth * scale);
        var matrices = context.method_51448();
        matrices.pushMatrix();
        matrices.translate(x + maxWidth - scaledWidth, y);
        matrices.scale(scale, scale);
        context.method_51433(field_22793, safe, 0, 0, color, true);
        matrices.popMatrix();
    }

    private void card(class_332 context, int x, int y, int width, int height, String title) {
        context.method_25294(x, y, x + width, y + height, CARD);
        outline(context, x, y, width, height, BORDER);
        context.method_25294(x, y, x + 3, y + height, ORANGE);
        context.method_25303(field_22793,
                field_22793.method_27523(title, Math.max(20, width - 24)),
                x + 12, y + 10, ORANGE);
    }

    private void drawButton(class_332 context, ButtonBounds button,
                            String label, boolean hovered) {
        context.method_25294(button.x(), button.y(), button.x() + button.width(),
                button.y() + button.height(), hovered ? 0xFF2C2C34 : 0xFF202027);
        outline(context, button.x(), button.y(), button.width(), button.height(),
                hovered ? ORANGE : BORDER);
        String safe = field_22793.method_27523(label, Math.max(8, button.width() - 10));
        context.method_25303(field_22793, safe,
                button.x() + (button.width() - field_22793.method_1727(safe)) / 2,
                button.y() + (button.height() - field_22793.field_2000) / 2,
                TEXT);
    }

    private void clampScroll() {
        int viewportHeight = Math.max(1, viewportBottom - viewportTop - 16);
        int maxScroll = Math.max(0, contentHeight - viewportHeight);
        scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset));
    }

    private void renderScrollbar(class_332 context, int x) {
        int viewportHeight = Math.max(1, viewportBottom - viewportTop);
        if (contentHeight <= viewportHeight) {
            return;
        }
        int thumbHeight = Math.max(18,
                Math.round(viewportHeight * (viewportHeight / (float) contentHeight)));
        int maxTrack = Math.max(1, viewportHeight - thumbHeight);
        int maxScroll = Math.max(1, contentHeight - viewportHeight);
        int thumbY = viewportTop + Math.round(maxTrack * (scrollOffset / (float) maxScroll));
        context.method_25294(x, viewportTop, x + 2, viewportBottom, 0x66303038);
        context.method_25294(x, thumbY, x + 2, thumbY + thumbHeight, ORANGE);
    }

    private static String number(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private static String memory(long mb) {
        if (mb <= 0) {
            return "Unavailable";
        }
        return mb >= 1024
                ? String.format(Locale.ROOT, "%.1f GB", mb / 1024.0)
                : mb + " MB";
    }

    private static void outline(class_332 context, int x, int y,
                                int width, int height, int color) {
        context.method_25294(x, y, x + width, y + 1, color);
        context.method_25294(x, y + height - 1, x + width, y + height, color);
        context.method_25294(x, y, x + 1, y + height, color);
        context.method_25294(x + width - 1, y, x + width, y + height, color);
    }

    private record ButtonBounds(int x, int y, int width, int height) {
        private boolean contains(double mouseX, double mouseY) {
            return mouseX >= x && mouseX < x + width
                    && mouseY >= y && mouseY < y + height;
        }
    }
}
