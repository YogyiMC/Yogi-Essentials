package dev.yogi.yogiessentials.client.gui;

import dev.yogi.yogiessentials.client.util.CrashAssistantManager;
import dev.yogi.yogiessentials.client.util.UiSoundManager;
import java.util.List;
import net.minecraft.class_11909;
import net.minecraft.class_2561;
import net.minecraft.class_332;
import net.minecraft.class_437;
import net.minecraft.class_5481;

public final class CrashAssistantScreen extends class_437 {
    private static final int ORANGE = 0xFFFF6A00;
    private static final int ORANGE_HOVER = 0xFFFF7B1A;
    private static final int BACKGROUND = 0xFF08080C;
    private static final int PANEL = 0xFF101014;
    private static final int CARD = 0xFF17171C;
    private static final int CARD_HOVER = 0xFF202027;
    private static final int BORDER = 0xFF303039;
    private static final int BORDER_SOFT = 0xFF24242B;
    private static final int TEXT = 0xFFF4F4F5;
    private static final int MUTED = 0xFF9999A3;
    private static final int GREEN = 0xFF68D391;
    private static final int RED = 0xFFFF7070;
    private static final int YELLOW = 0xFFFFC15A;

    private final class_437 parent;
    private String status = "";
    private int statusColor = MUTED;

    public CrashAssistantScreen(class_437 parent) {
        super(class_2561.method_43470("Crash Assistant"));
        this.parent = parent;
    }

    @Override
    public void method_25394(class_332 context, int mouseX, int mouseY, float deltaTicks) {
        context.method_25294(0, 0, field_22789, field_22790, BACKGROUND);

        int panelWidth = Math.min(760, Math.max(340, field_22789 - 32));
        int panelHeight = Math.min(510, Math.max(420, field_22790 - 28));
        int x = (field_22789 - panelWidth) / 2;
        int y = (field_22790 - panelHeight) / 2;
        int innerX = x + 20;
        int innerWidth = panelWidth - 40;

        context.method_25294(x, y, x + panelWidth, y + panelHeight, PANEL);
        outline(context, x, y, panelWidth, panelHeight, ORANGE);
        context.method_25294(x, y, x + 4, y + panelHeight, ORANGE);
        context.method_25294(x + 4, y, x + panelWidth, y + 2, ORANGE);

        context.method_25303(field_22793, "CRASH ASSISTANT", innerX, y + 16, ORANGE);
        context.method_25303(field_22793, "Recovery, diagnostics and startup protection", innerX, y + 33, MUTED);
        String version = dev.yogi.yogiessentials.client.util.ModVersionUtil.displayVersion();
        context.method_25303(field_22793, version, x + panelWidth - 20 - field_22793.method_1727(version), y + 17, MUTED);

        CrashAssistantManager.WatcherStatus watcher = CrashAssistantManager.getWatcherStatus();
        CrashAssistantManager.EarlyRecoveryStatus early = CrashAssistantManager.getEarlyRecoveryStatus();

        int cardGap = 10;
        int statusCardWidth = (innerWidth - cardGap) / 2;
        int statusY = y + 58;
        drawRuntimeCard(context, innerX, statusY, statusCardWidth, 66, watcher);
        drawEarlyCard(context, innerX + statusCardWidth + cardGap, statusY, statusCardWidth, 66, early);

        CrashAssistantManager.LastAnalysis last = CrashAssistantManager.getLastAnalysis();
        int analysisY = y + 136;
        context.method_25303(field_22793, "LAST RECOVERY ANALYSIS", innerX, analysisY, MUTED);
        int analysisCardY = analysisY + 16;
        context.method_25294(innerX, analysisCardY, innerX + innerWidth, analysisCardY + 82, CARD);
        outline(context, innerX, analysisCardY, innerWidth, 82, BORDER_SOFT);
        context.method_25294(innerX, analysisCardY, innerX + 3, analysisCardY + 82, confidenceColor(last.confidence()));
        context.method_25303(field_22793, last.title(), innerX + 14, analysisCardY + 13, confidenceColor(last.confidence()));
        drawWrapped(context, last.summary(), innerX + 14, analysisCardY + 31, innerWidth - 28, MUTED, 3);

        int actionsLabelY = analysisCardY + 96;
        context.method_25303(field_22793, "RECOVERY TOOLS", innerX, actionsLabelY, MUTED);
        int gap = 8;
        int buttonWidth = (innerWidth - gap * 2) / 3;
        int firstRowY = actionsLabelY + 16;
        int secondRowY = firstRowY + 38;

        drawButton(context, innerX, firstRowY, buttonWidth, 30, "Analyze Current Logs", mouseX, mouseY, true);
        drawButton(context, innerX + buttonWidth + gap, firstRowY, buttonWidth, 30, "Startup Preflight", mouseX, mouseY, false);
        drawButton(context, innerX + (buttonWidth + gap) * 2, firstRowY, buttonWidth, 30, "Run Diagnosis Tests", mouseX, mouseY, false);

        drawButton(context, innerX, secondRowY, buttonWidth, 30, "Test Full Fix Flow", mouseX, mouseY, false);
        drawButton(context, innerX + buttonWidth + gap, secondRowY, buttonWidth, 30, "Crash History", mouseX, mouseY, false);
        drawButton(context, innerX + (buttonWidth + gap) * 2, secondRowY, buttonWidth, 30, "Disabled Mods", mouseX, mouseY, false);

        int protectionY = secondRowY + 48;
        context.method_25303(field_22793, "PROTECTION", innerX, protectionY, MUTED);
        int protectionButtonY = protectionY + 16;
        int wideWidth = (innerWidth - gap) * 2 / 3;
        int smallWidth = innerWidth - gap - wideWidth;
        String earlyLabel = early.enabled() ? "Disable Early Startup Recovery" : "Enable Early Startup Recovery";
        drawButton(context, innerX, protectionButtonY, wideWidth, 32, earlyLabel, mouseX, mouseY, early.supported() && !early.enabled());
        drawButton(context, innerX + wideWidth + gap, protectionButtonY, smallWidth, 32, "Recovery Folder", mouseX, mouseY, false);

        int footerY = y + panelHeight - 38;
        int backWidth = 82;
        drawButton(context, x + panelWidth - 20 - backWidth, footerY, backWidth, 24, "Back", mouseX, mouseY, false);
        if (!status.isBlank()) {
            int statusWidth = innerWidth - backWidth - 14;
            context.method_25294(innerX, footerY, innerX + statusWidth, footerY + 24, 0xFF15151A);
            context.method_25294(innerX, footerY, innerX + 3, footerY + 24, statusColor);
            String fitted = field_22793.method_27523(status, Math.max(10, statusWidth - 18));
            context.method_25303(field_22793, fitted, innerX + 10, footerY + 8, statusColor);
        }
    }

    private void drawRuntimeCard(class_332 context, int x, int y, int width, int height, CrashAssistantManager.WatcherStatus watcher) {
        int color = watcher.active() ? GREEN : RED;
        context.method_25294(x, y, x + width, y + height, CARD);
        outline(context, x, y, width, height, BORDER_SOFT);
        context.method_25294(x, y, x + 3, y + height, color);
        context.method_25303(field_22793, "RUNTIME PROTECTION", x + 14, y + 11, MUTED);
        String state = watcher.active() ? "ACTIVE" : watcher.state();
        context.method_25303(field_22793, state, x + 14, y + 28, color);
        String detail = watcher.active() && watcher.helperPid() > 0L
                ? "Minecraft " + watcher.watchedPid() + "  •  Helper " + watcher.helperPid()
                : "Watches crashes after Yogi Essentials initializes";
        context.method_25303(field_22793, field_22793.method_27523(detail, width - 28), x + 14, y + 46, MUTED);
    }

    private void drawEarlyCard(class_332 context, int x, int y, int width, int height, CrashAssistantManager.EarlyRecoveryStatus early) {
        int color;
        String state;
        if (!early.supported()) {
            color = MUTED;
            state = "UNSUPPORTED";
        } else if (!early.enabled()) {
            color = ORANGE;
            state = "DISABLED";
        } else if (early.active()) {
            color = GREEN;
            state = "ACTIVE";
        } else {
            color = YELLOW;
            state = early.state();
        }
        context.method_25294(x, y, x + width, y + height, CARD);
        outline(context, x, y, width, height, BORDER_SOFT);
        context.method_25294(x, y, x + 3, y + height, color);
        context.method_25303(field_22793, "EARLY STARTUP PROTECTION", x + 14, y + 11, MUTED);
        context.method_25303(field_22793, state, x + 14, y + 28, color);
        String detail = early.active() && early.pid() > 0L
                ? "Helper " + early.pid() + "  •  Launch-triggered preflight"
                : "Checks early blockers when Minecraft launches";
        context.method_25303(field_22793, field_22793.method_27523(detail, width - 28), x + 14, y + 46, MUTED);
    }

    @Override
    public boolean method_25402(class_11909 click, boolean doubled) {
        if (click.method_74245() != 0) {
            return super.method_25402(click, doubled);
        }

        int panelWidth = Math.min(760, Math.max(340, field_22789 - 32));
        int panelHeight = Math.min(510, Math.max(420, field_22790 - 28));
        int x = (field_22789 - panelWidth) / 2;
        int y = (field_22790 - panelHeight) / 2;
        int innerX = x + 20;
        int innerWidth = panelWidth - 40;
        int analysisCardY = y + 152;
        int actionsLabelY = analysisCardY + 96;
        int gap = 8;
        int buttonWidth = (innerWidth - gap * 2) / 3;
        int firstRowY = actionsLabelY + 16;
        int secondRowY = firstRowY + 38;
        int protectionY = secondRowY + 48;
        int protectionButtonY = protectionY + 16;
        int wideWidth = (innerWidth - gap) * 2 / 3;
        int smallWidth = innerWidth - gap - wideWidth;

        if (inside(click.comp_4798(), click.comp_4799(), innerX, firstRowY, buttonWidth, 30)) {
            UiSoundManager.click();
            setLaunchStatus(CrashAssistantManager.analyzeCurrentLogs(), "Current logs opened in the desktop assistant.", "Could not launch log analysis.");
            return true;
        }
        if (inside(click.comp_4798(), click.comp_4799(), innerX + buttonWidth + gap, firstRowY, buttonWidth, 30)) {
            UiSoundManager.click();
            setLaunchStatus(CrashAssistantManager.runStartupPreflight(), "Startup compatibility preflight opened.", "Could not launch startup preflight.");
            return true;
        }
        if (inside(click.comp_4798(), click.comp_4799(), innerX + (buttonWidth + gap) * 2, firstRowY, buttonWidth, 30)) {
            UiSoundManager.click();
            setLaunchStatus(CrashAssistantManager.runDiagnosisTests(), "Diagnosis test suite opened.", "Could not launch diagnosis tests.");
            return true;
        }
        if (inside(click.comp_4798(), click.comp_4799(), innerX, secondRowY, buttonWidth, 30)) {
            UiSoundManager.click();
            setLaunchStatus(CrashAssistantManager.launchTestAssistant(), "Test recovery flow opened.", "Could not launch the desktop assistant.");
            return true;
        }
        if (inside(click.comp_4798(), click.comp_4799(), innerX + buttonWidth + gap, secondRowY, buttonWidth, 30)) {
            UiSoundManager.click();
            setLaunchStatus(CrashAssistantManager.openCrashHistory(), "Crash history opened.", "Could not open crash history.");
            return true;
        }
        if (inside(click.comp_4798(), click.comp_4799(), innerX + (buttonWidth + gap) * 2, secondRowY, buttonWidth, 30)) {
            UiSoundManager.click();
            setLaunchStatus(CrashAssistantManager.openDisabledModsManager(), "Disabled Mods manager opened.", "Could not open Disabled Mods manager.");
            return true;
        }
        if (inside(click.comp_4798(), click.comp_4799(), innerX, protectionButtonY, wideWidth, 32)) {
            UiSoundManager.click();
            if (!CrashAssistantManager.isEarlyStartupRecoverySupported()) {
                setStatus("Early Startup Recovery is currently supported on Windows.", RED);
                return true;
            }
            boolean wasEnabled = CrashAssistantManager.isEarlyStartupRecoveryEnabled();
            boolean changed = wasEnabled
                    ? CrashAssistantManager.disableEarlyStartupRecovery()
                    : CrashAssistantManager.enableEarlyStartupRecovery();
            if (changed) {
                setStatus(
                        wasEnabled
                                ? "Early Startup Recovery disabled."
                                : "Early Startup Recovery enabled and running in the background.",
                        GREEN
                );
            } else {
                String detail = CrashAssistantManager.getLastEarlyRecoveryError();
                String fallback = wasEnabled ? "Could not fully disable Early Startup Recovery." : "Could not enable Early Startup Recovery.";
                setStatus(detail == null || detail.isBlank() ? fallback : detail, RED);
            }
            return true;
        }
        if (inside(click.comp_4798(), click.comp_4799(), innerX + wideWidth + gap, protectionButtonY, smallWidth, 32)) {
            UiSoundManager.click();
            setLaunchStatus(CrashAssistantManager.openRecoveryFolder(), "Opened the recovery folder.", "Could not open the recovery folder.");
            return true;
        }

        int backWidth = 82;
        int footerY = y + panelHeight - 38;
        if (inside(click.comp_4798(), click.comp_4799(), x + panelWidth - 20 - backWidth, footerY, backWidth, 24)) {
            UiSoundManager.click();
            method_25419();
            return true;
        }
        return super.method_25402(click, doubled);
    }

    @Override
    public void method_25419() {
        if (field_22787 != null) {
            field_22787.method_1507(parent);
        }
    }

    private void setLaunchStatus(boolean launched, String success, String failure) {
        setStatus(launched ? success : failure, launched ? GREEN : RED);
    }

    private void setStatus(String value, int color) {
        status = value;
        statusColor = color;
    }

    private void drawWrapped(class_332 context, String value, int x, int y, int maxWidth, int color, int maxLines) {
        List<class_5481> lines = field_22793.method_1728(class_2561.method_43470(value == null ? "" : value), Math.max(1, maxWidth));
        for (int i = 0; i < Math.min(maxLines, lines.size()); i++) {
            context.method_51430(field_22793, lines.get(i), x, y + i * 11, color, true);
        }
    }

    private void drawButton(class_332 context, int x, int y, int buttonWidth, int buttonHeight, String label, int mouseX, int mouseY, boolean primary) {
        boolean hovered = inside(mouseX, mouseY, x, y, buttonWidth, buttonHeight);
        int fill = primary
                ? hovered ? ORANGE_HOVER : ORANGE
                : hovered ? CARD_HOVER : CARD;
        int border = primary ? ORANGE : hovered ? 0xFF50505C : BORDER;
        context.method_25294(x, y, x + buttonWidth, y + buttonHeight, fill);
        outline(context, x, y, buttonWidth, buttonHeight, border);
        if (!primary) {
            context.method_25294(x, y, x + 2, y + buttonHeight, hovered ? ORANGE : 0xFF383840);
        }
        String fitted = field_22793.method_27523(label, Math.max(10, buttonWidth - 16));
        context.method_25303(
                field_22793,
                fitted,
                x + (buttonWidth - field_22793.method_1727(fitted)) / 2,
                y + (buttonHeight - field_22793.field_2000) / 2,
                TEXT
        );
    }

    private int confidenceColor(String confidence) {
        return switch (confidence == null ? "" : confidence) {
            case "HIGH" -> GREEN;
            case "MEDIUM" -> ORANGE;
            case "LOW" -> YELLOW;
            default -> MUTED;
        };
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private static void outline(class_332 context, int x, int y, int width, int height, int color) {
        context.method_25294(x, y, x + width, y + 1, color);
        context.method_25294(x, y + height - 1, x + width, y + height, color);
        context.method_25294(x, y, x + 1, y + height, color);
        context.method_25294(x + width - 1, y, x + width, y + height, color);
    }
}
