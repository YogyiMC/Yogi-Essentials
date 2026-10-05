package dev.yogi.yogiessentials.client.gui;

import dev.yogi.yogiessentials.client.config.ConfigManager;
import dev.yogi.yogiessentials.client.keybind.KeybindManager;
import dev.yogi.yogiessentials.client.util.UiSoundManager;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.class_11909;
import net.minecraft.class_2561;
import net.minecraft.class_304;
import net.minecraft.class_332;
import net.minecraft.class_437;
import net.minecraft.class_5481;







public class WelcomeScreen extends class_437 {

    private static final int ORANGE = 0xFFFF6A00;
    private static final int PANEL = 0xF20D0D10;
    private static final int CARD = 0xE817171C;
    private static final int CARD_HOVER = 0xF0202026;
    private static final int BORDER = 0xFF292930;
    private static final int TEXT = 0xFFF5F5F5;
    private static final int TEXT_MUTED = 0xFFA6A6AC;

    private static final int TARGET_WIDTH = 460;
    private static final int TARGET_HEIGHT = 360;

    private int scrollOffset;

    public WelcomeScreen() {
        super(class_2561.method_43470("Welcome to Yogi Essentials"));
    }

    private int panelWidth() {
        return Math.min(TARGET_WIDTH, Math.max(220, field_22789 - 40));
    }

    private int panelHeight() {
        return Math.min(TARGET_HEIGHT, Math.max(200, field_22790 - 40));
    }

    private int panelX() {
        return (field_22789 - panelWidth()) / 2;
    }

    private int panelY() {
        return (field_22790 - panelHeight()) / 2;
    }

    private String openKeyLabel() {
        class_304 key = KeybindManager.getOpenMenuKey();
        if (key == null) {
            return "the Yogi Essentials key";
        }
        return key.method_16007().getString();
    }

    private List<String> bulletLines() {
        List<String> lines = new ArrayList<>();
        lines.add("• Most optional HUD modules start disabled, so your screen stays clean.");
        lines.add("• Open Yogi Essentials with " + openKeyLabel() + " (rebindable in Controls).");
        lines.add("• Enable or disable each HUD module individually from its category.");
        lines.add("• The HUD Editor lets you drag to move and drag any edge to resize widgets.");
        lines.add("• Auto Optimize (Performance) tunes your game for FPS and frame-time.");
        lines.add("• Fixes holds reliability features; SMP holds survival/multiplayer tools.");
        return lines;
    }

    @Override
    public void method_25394(class_332 context, int mouseX, int mouseY, float deltaTicks) {
        context.method_25294(0, 0, field_22789, field_22790, 0x99000000);

        int px = panelX();
        int py = panelY();
        int pw = panelWidth();
        int ph = panelHeight();

        context.method_25294(px, py, px + pw, py + ph, PANEL);
        outline(context, px, py, pw, ph, ORANGE);
        context.method_25294(px, py, px + pw, py + 3, ORANGE);

        int cx = px + pw / 2;

        context.method_25300(field_22793, "Welcome to Yogi Essentials", cx, py + 14, ORANGE);
        context.method_25300(field_22793, dev.yogi.yogiessentials.client.util.ModVersionUtil.displayVersion() + "  •  client-side", cx, py + 27, TEXT_MUTED);

        int contentTop = py + 46;
        int buttonAreaHeight = pw < 340 ? 82 : 40;
        int contentBottom = py + ph - buttonAreaHeight - 8;

        
        
        int textX = px + 16;
        int textWidth = pw - 32;
        int lineY = contentTop - scrollOffset;
        int lineHeight = field_22793.field_2000 + 4;

        context.method_44379(px + 1, contentTop, px + pw - 1, contentBottom);
        int totalHeight = 0;
        for (String bullet : bulletLines()) {
            List<class_5481> wrapped = field_22793.method_1728(class_2561.method_43470(bullet), textWidth);
            for (class_5481 line : wrapped) {
                if (lineY + lineHeight >= contentTop && lineY <= contentBottom) {
                    context.method_51430(field_22793, line, textX, lineY, TEXT, false);
                }
                lineY += lineHeight;
                totalHeight += lineHeight;
            }
            lineY += 4;
            totalHeight += 4;
        }
        context.method_44380();

        
        int visible = contentBottom - contentTop;
        int maxScroll = Math.max(0, totalHeight - visible);
        if (scrollOffset > maxScroll) {
            scrollOffset = maxScroll;
        }
        if (scrollOffset < 0) {
            scrollOffset = 0;
        }

        
        int[] b = buttonRects();
        drawButton(context, b[0], b[1], b[2], b[3], "Open Yogi Essentials", mouseX, mouseY, true);
        drawButton(context, b[4], b[5], b[6], b[7], "Open HUD Editor", mouseX, mouseY, false);
        drawButton(context, b[8], b[9], b[10], b[11], "Got It", mouseX, mouseY, false);
    }

    




    private int[] buttonRects() {
        int px = panelX();
        int py = panelY();
        int pw = panelWidth();
        int ph = panelHeight();
        int h = 20;
        int gap = 6;

        boolean stacked = pw < 340;
        if (stacked) {
            int bw = pw - 32;
            int x = px + 16;
            int y = py + ph - (h * 3 + gap * 2) - 8;
            return new int[] {
                    x, y, bw, h,
                    x, y + h + gap, bw, h,
                    x, y + (h + gap) * 2, bw, h
            };
        }

        int y = py + ph - h - 10;
        int bw = (pw - 32 - gap * 2) / 3;
        int x0 = px + 16;
        int x1 = x0 + bw + gap;
        int x2 = x1 + bw + gap;
        return new int[] {
                x0, y, bw, h,
                x1, y, bw, h,
                x2, y, bw, h
        };
    }

    private void drawButton(class_332 context, int x, int y, int w, int h,
                            String label, int mouseX, int mouseY, boolean primary) {
        boolean hovered = inside(mouseX, mouseY, x, y, w, h);
        int fill = hovered ? CARD_HOVER : CARD;
        context.method_25294(x, y, x + w, y + h, fill);
        outline(context, x, y, w, h, hovered || primary ? ORANGE : BORDER);
        if (primary) {
            context.method_25294(x, y, x + 3, y + h, ORANGE);
        }
        String trimmed = field_22793.method_27523(label, Math.max(8, w - 8));
        context.method_25300(field_22793, trimmed, x + w / 2,
                y + (h - field_22793.field_2000) / 2 + 1, hovered ? TEXT : 0xFFD6D6DA);
    }

    @Override
    public boolean method_25402(class_11909 click, boolean doubled) {
        if (click.method_74245() == 0) {
            int[] b = buttonRects();
            if (inside(click.comp_4798(), click.comp_4799(), b[0], b[1], b[2], b[3])) {
                dismiss();
                UiSoundManager.click();
                if (field_22787 != null) {
                    field_22787.method_1507(new YogiEssentialsScreen());
                }
                return true;
            }
            if (inside(click.comp_4798(), click.comp_4799(), b[4], b[5], b[6], b[7])) {
                dismiss();
                UiSoundManager.click();
                if (field_22787 != null) {
                    field_22787.method_1507(new HudEditorScreen());
                }
                return true;
            }
            if (inside(click.comp_4798(), click.comp_4799(), b[8], b[9], b[10], b[11])) {
                dismiss();
                UiSoundManager.click();
                if (field_22787 != null) {
                    field_22787.method_1507(null);
                }
                return true;
            }
        }
        return super.method_25402(click, doubled);
    }

    @Override
    public boolean method_25401(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scrollOffset -= (int) Math.round(verticalAmount * 16.0);
        if (scrollOffset < 0) {
            scrollOffset = 0;
        }
        return true;
    }

    private void dismiss() {
        ConfigManager.markWelcomeSeen();
    }

    @Override
    public void method_25419() {
        dismiss();
        if (field_22787 != null) {
            field_22787.method_1507(null);
        }
    }

    @Override
    public boolean method_25421() {
        return false;
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
    }

    private static void outline(class_332 context, int x, int y, int w, int h, int color) {
        context.method_25294(x, y, x + w, y + 1, color);
        context.method_25294(x, y + h - 1, x + w, y + h, color);
        context.method_25294(x, y, x + 1, y + h, color);
        context.method_25294(x + w - 1, y, x + w, y + h, color);
    }
}
