package dev.yogi.yogiessentials.client.gui;

import dev.yogi.yogiessentials.client.util.FrameTelemetry;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.class_11909;
import net.minecraft.class_2561;
import net.minecraft.class_310;
import net.minecraft.class_332;
import net.minecraft.class_437;

public final class PerformanceProfilerScreen extends class_437 {
    private final class_437 parent;
    private int scroll;

    public PerformanceProfilerScreen(class_437 parent) {
        super(class_2561.method_43470("Yogi Essentials Performance Profiler"));
        this.parent = parent;
    }

    @Override
    public void method_25394(class_332 context, int mouseX, int mouseY, float deltaTicks) {
        int left = Math.max(6, (field_22789 - 520) / 2);
        int top = Math.max(6, (field_22790 - 360) / 2);
        int right = Math.min(field_22789 - 6, left + 520);
        int bottom = Math.min(field_22790 - 6, top + 360);
        context.method_25294(0, 0, field_22789, field_22790, 0xB0000000);
        context.method_25294(left, top, right, bottom, 0xF2101016);
        context.method_25294(left, top, right, top + 3, 0xFFFF6A00);
        context.method_25294(left, bottom - 1, right, bottom, 0xFFFF6A00);
        context.method_25294(left, top, left + 1, bottom, 0xFFFF6A00);
        context.method_25294(right - 1, top, right, bottom, 0xFFFF6A00);
        context.method_25303(field_22793, field_22793.method_27523(
                "PERFORMANCE PROFILER", Math.max(12, right - left - 24)),
                left + 12, top + 14, 0xFFFF6A00);
        FrameTelemetry.Sample sample = FrameTelemetry.snapshot();
        List<String> rows = new ArrayList<>();
        rows.add("F3 FPS (live): " + class_310.method_1551().method_47599());
        rows.add(sample.available() ? "Sample average (this view): " + format(sample.averageFps()) : "Collecting world frames...");
        rows.add("1% low FPS: " + (sample.available() ? format(sample.onePercentLow()) : "Collecting"));
        rows.add("Average frame time: " + (sample.available() ? format(sample.averageMs()) + " ms" : "Collecting"));
        rows.add("Frame-time variance: " + (sample.available() ? format(sample.varianceMsSquared()) + " ms²" : "Collecting"));
        rows.add("Major frame spikes: " + sample.stutters() + " in " + sample.frames() + " sampled frames");
        rows.add("JVM memory: " + sample.usedMemoryMb() + " MB used / "
                + sample.allocatedMemoryMb() + " MB allocated / " + sample.maxMemoryMb() + " MB max");
        rows.add("Resolution: " + sample.scaledWidth() + " x " + sample.scaledHeight());
        rows.add("Render distance: " + sample.renderDistance() + " chunks");
        rows.add("Simulation distance: " + sample.simulationDistance() + " chunks");
        rows.add("Possible contributors (heuristics, not hardware utilization):");
        rows.addAll(sample.bottlenecks().isEmpty() ? List.of("None detected from available data")
                : sample.bottlenecks());
        rows.add("Compare in the same world, view and workload.");
        int clipTop = Math.min(bottom - 1, top + 42);
        int clipBottom = Math.max(clipTop + 1, bottom - 36);
        context.method_44379(left + 4, clipTop, right - 4, clipBottom);
        for (int i = 0; i < rows.size(); i++) {
            int y = clipTop + 6 + i * 20 - scroll;
            if (y + 10 >= clipTop && y < clipBottom) {
                context.method_51433(field_22793, field_22793.method_27523(rows.get(i),
                        Math.max(12, right - left - 24)), left + 12, y, 0xFFE5E5E8, true);
            }
        }
        context.method_44380();
        context.method_25294(left + 12, bottom - 29, left + 91, bottom - 8, 0xFF202027);
        context.method_25303(field_22793, "Back", left + 36, bottom - 23, 0xFFF5F5F5);
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }

    @Override
    public boolean method_25402(class_11909 click, boolean doubled) {
        int left = Math.max(6, (field_22789 - 520) / 2);
        int bottom = Math.min(field_22790 - 6, Math.max(6, (field_22790 - 360) / 2) + 360);
        if (click.method_74245() == 0 && click.comp_4798() >= left + 12 && click.comp_4798() < left + 91
                && click.comp_4799() >= bottom - 29 && click.comp_4799() < bottom - 8) {
            method_25419();
            return true;
        }
        return super.method_25402(click, doubled);
    }

    @Override
    public boolean method_25401(double mouseX, double mouseY, double horizontal, double vertical) {
        scroll = Math.max(0, Math.min(180, scroll - (int) Math.round(vertical * 30)));
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
}
