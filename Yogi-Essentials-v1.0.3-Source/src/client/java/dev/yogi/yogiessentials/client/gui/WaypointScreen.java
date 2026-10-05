package dev.yogi.yogiessentials.client.gui;

import dev.yogi.yogiessentials.client.util.UiSoundManager;
import dev.yogi.yogiessentials.client.util.WaypointManager;
import java.util.List;
import net.minecraft.class_11909;
import net.minecraft.class_2561;
import net.minecraft.class_332;
import net.minecraft.class_342;
import net.minecraft.class_437;


public final class WaypointScreen extends class_437 {

    private static final int ORANGE = 0xFFFF6A00;
    private static final int BG = 0xFF09090D;
    private static final int PANEL = 0xFF101014;
    private static final int CARD = 0xFF17171C;
    private static final int HOVER = 0xFF202027;
    private static final int BORDER = 0xFF303038;
    private static final int TEXT = 0xFFF4F4F5;
    private static final int MUTED = 0xFF9999A1;

    private final class_437 parent;
    private String viewedDimension;
    private String selectedId;
    private int scroll;

    private class_342 nameField;
    private class_342 xField;
    private class_342 yField;
    private class_342 zField;

    private int draftColor = ORANGE;
    private boolean restoreDraftsAfterPicker;
    private String draftName = "";
    private String draftX = "";
    private String draftY = "";
    private String draftZ = "";

    private long saveFeedbackUntil;
    private boolean saveFailed;

    public WaypointScreen(class_437 parent) {
        super(class_2561.method_43470("Yogi Essentials Waypoints"));
        this.parent = parent;
        this.viewedDimension = WaypointManager.currentDimension();
    }

    @Override
    protected void method_25426() {
        int panelX = Math.max(12, field_22789 / 2 + 6);
        int fieldWidth = Math.max(120, field_22789 - panelX - 24);
        int y = 108;

        nameField = method_37063(new class_342(field_22793, panelX + 12, y, fieldWidth - 24, 22, class_2561.method_43470("Name")));
        xField = method_37063(new class_342(field_22793, panelX + 12, y + 42, fieldWidth / 3 - 14, 22, class_2561.method_43470("X")));
        yField = method_37063(new class_342(field_22793, panelX + fieldWidth / 3 + 4, y + 42, fieldWidth / 3 - 14, 22, class_2561.method_43470("Y")));
        zField = method_37063(new class_342(field_22793, panelX + fieldWidth * 2 / 3 - 4, y + 42, fieldWidth / 3 - 20, 22, class_2561.method_43470("Z")));

        nameField.method_1880(48);
        xField.method_1880(16);
        yField.method_1880(16);
        zField.method_1880(16);

        if (restoreDraftsAfterPicker && selected() != null) {
            setFieldsEditable(true);
            nameField.method_1852(draftName);
            xField.method_1852(draftX);
            yField.method_1852(draftY);
            zField.method_1852(draftZ);
            restoreDraftsAfterPicker = false;
        } else {
            loadSelection();
        }
    }

    private List<WaypointManager.Waypoint> list() {
        return WaypointManager.getForDimension(viewedDimension);
    }

    private WaypointManager.Waypoint selected() {
        if (selectedId == null) return null;
        for (WaypointManager.Waypoint waypoint : list()) {
            if (selectedId.equals(waypoint.id)) return waypoint;
        }
        return null;
    }

    private void loadSelection() {
        if (nameField == null) return;
        WaypointManager.Waypoint w = selected();
        boolean active = w != null;
        setFieldsEditable(active);
        clearSaveFeedback();

        if (!active) {
            nameField.method_1852("");
            xField.method_1852("");
            yField.method_1852("");
            zField.method_1852("");
            draftColor = ORANGE;
            return;
        }

        nameField.method_1852(w.name);
        xField.method_1852(formatCoord(w.x));
        yField.method_1852(formatCoord(w.y));
        zField.method_1852(formatCoord(w.z));
        draftColor = w.color | 0xFF000000;
    }

    private void setFieldsEditable(boolean editable) {
        nameField.method_1888(editable);
        xField.method_1888(editable);
        yField.method_1888(editable);
        zField.method_1888(editable);
    }

    private void captureDraftFields() {
        draftName = nameField.method_1882();
        draftX = xField.method_1882();
        draftY = yField.method_1882();
        draftZ = zField.method_1882();
    }

    private void clearSaveFeedback() {
        saveFeedbackUntil = 0L;
        saveFailed = false;
    }

    private static String formatCoord(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.0001) return Long.toString(Math.round(value));
        return String.format(java.util.Locale.ROOT, "%.2f", value);
    }

    @Override
    public void method_25394(class_332 context, int mouseX, int mouseY, float deltaTicks) {
        context.method_25294(0, 0, field_22789, field_22790, BG);
        context.method_25300(field_22793, "WAYPOINTS", field_22789 / 2, 16, ORANGE);
        context.method_25300(field_22793,
                "Dimension: " + WaypointManager.prettyDimension(viewedDimension), field_22789 / 2, 32, TEXT);
        context.method_25300(field_22793,
                "Waypoints from different dimensions are stored and rendered separately.", field_22789 / 2, 47, MUTED);

        int leftX = 12;
        int leftW = Math.max(220, field_22789 / 2 - 18);
        int top = 76;
        int bottom = field_22790 - 42;
        context.method_25294(leftX, top, leftX + leftW, bottom, PANEL);
        outline(context, leftX, top, leftW, bottom - top, ORANGE);

        int rightX = field_22789 / 2 + 6;
        int rightW = field_22789 - rightX - 12;
        context.method_25294(rightX, top, rightX + rightW, bottom, PANEL);
        outline(context, rightX, top, rightW, bottom - top, ORANGE);
        context.method_25303(field_22793, "EDIT SELECTED", rightX + 12, top + 12, ORANGE);

        drawButton(context, leftX + 10, top + 10, Math.max(96, leftW - 160), 24,
                "Add Current Position", mouseX, mouseY);
        drawButton(context, leftX + leftW - 140, top + 10, 62, 24, "< Dim", mouseX, mouseY);
        drawButton(context, leftX + leftW - 72, top + 10, 62, 24, "Dim >", mouseX, mouseY);

        List<WaypointManager.Waypoint> waypoints = list();
        int rowY = top + 44 - scroll;
        for (WaypointManager.Waypoint waypoint : waypoints) {
            if (rowY + 34 >= top + 40 && rowY <= bottom - 4) {
                boolean selected = waypoint.id.equals(selectedId);
                boolean hovered = inside(mouseX, mouseY, leftX + 10, rowY, leftW - 20, 32);
                context.method_25294(leftX + 10, rowY, leftX + leftW - 10, rowY + 32,
                        selected ? 0xFF2B1D15 : hovered ? HOVER : CARD);
                outline(context, leftX + 10, rowY, leftW - 20, 32, selected ? ORANGE : BORDER);
                context.method_25294(leftX + 14, rowY + 5, leftX + 18, rowY + 27, waypoint.color | 0xFF000000);
                String name = field_22793.method_27523(waypoint.name, Math.max(40, leftW - 176));
                context.method_25303(field_22793, name, leftX + 24, rowY + 6,
                        waypoint.enabled ? TEXT : MUTED);
                String coords = Math.round(waypoint.x) + ", " + Math.round(waypoint.y) + ", " + Math.round(waypoint.z);
                context.method_51433(field_22793, coords, leftX + 24, rowY + 19, MUTED, true);
                drawButton(context, leftX + leftW - 128, rowY + 5, 52, 22,
                        waypoint.enabled ? "ON" : "OFF", mouseX, mouseY);
                drawButton(context, leftX + leftW - 70, rowY + 5, 52, 22, "Delete", mouseX, mouseY);
            }
            rowY += 38;
        }

        WaypointManager.Waypoint selected = selected();
        if (selected == null) {
            context.method_25303(field_22793, "Select a waypoint from the left.", rightX + 12, top + 40, MUTED);
        } else {
            int labelX = rightX + 12;
            context.method_25303(field_22793, "Name", labelX, 96, MUTED);
            context.method_25303(field_22793, "X", labelX, 138, MUTED);
            context.method_25303(field_22793, "Y", rightX + rightW / 3 + 4, 138, MUTED);
            context.method_25303(field_22793, "Z", rightX + rightW * 2 / 3 - 4, 138, MUTED);
            context.method_25303(field_22793, "Color", labelX, 180, MUTED);
            context.method_25294(labelX, 194, labelX + 28, 216, draftColor | 0xFF000000);
            outline(context, labelX, 194, 28, 22, 0xFFFFFFFF);
            drawButton(
                    context,
                    labelX + 36,
                    192,
                    Math.max(80, rightW - 60),
                    26,
                    "Choose Color",
                    mouseX,
                    mouseY
            );

            long now = System.currentTimeMillis();
            String saveLabel = now < saveFeedbackUntil
                    ? (saveFailed ? "Invalid values" : "Saved!")
                    : "Save Changes";
            drawButton(context, rightX + 12, 226, Math.max(80, rightW - 24), 26, saveLabel, mouseX, mouseY);
            context.method_51433(field_22793,
                    "Enabled: " + (selected.enabled ? "Yes" : "No") + "  •  " + WaypointManager.prettyDimension(selected.dimension),
                    rightX + 12, 262, MUTED, true);
        }

        drawButton(context, 12, field_22790 - 32, 90, 22, "Back", mouseX, mouseY);
        super.method_25394(context, mouseX, mouseY, deltaTicks);
    }

    @Override
    public boolean method_25402(class_11909 click, boolean doubled) {
        if (click.method_74245() != 0) return super.method_25402(click, doubled);

        int leftX = 12;
        int leftW = Math.max(220, field_22789 / 2 - 18);
        int top = 76;
        int bottom = field_22790 - 42;
        int rightX = field_22789 / 2 + 6;
        int rightW = field_22789 - rightX - 12;

        if (inside(click.comp_4798(), click.comp_4799(), leftX + 10, top + 10, Math.max(96, leftW - 160), 24)) {
            WaypointManager.Waypoint added = WaypointManager.addCurrent(null);
            if (added != null) {
                viewedDimension = added.dimension;
                selectedId = added.id;
                restoreDraftsAfterPicker = false;
                loadSelection();
                UiSoundManager.click();
            }
            return true;
        }
        if (inside(click.comp_4798(), click.comp_4799(), leftX + leftW - 140, top + 10, 62, 24)) {
            cycleDimension(-1); return true;
        }
        if (inside(click.comp_4798(), click.comp_4799(), leftX + leftW - 72, top + 10, 62, 24)) {
            cycleDimension(1); return true;
        }
        if (inside(click.comp_4798(), click.comp_4799(), 12, field_22790 - 32, 90, 22)) {
            method_25419(); return true;
        }

        List<WaypointManager.Waypoint> waypoints = list();
        int rowY = top + 44 - scroll;
        for (WaypointManager.Waypoint waypoint : waypoints) {
            if (inside(click.comp_4798(), click.comp_4799(), leftX + leftW - 128, rowY + 5, 52, 22)) {
                WaypointManager.toggle(waypoint.id);
                if (waypoint.id.equals(selectedId)) loadSelection();
                UiSoundManager.click();
                return true;
            }
            if (inside(click.comp_4798(), click.comp_4799(), leftX + leftW - 70, rowY + 5, 52, 22)) {
                WaypointManager.remove(waypoint.id);
                if (waypoint.id.equals(selectedId)) selectedId = null;
                restoreDraftsAfterPicker = false;
                loadSelection();
                UiSoundManager.click();
                return true;
            }
            if (inside(click.comp_4798(), click.comp_4799(), leftX + 10, rowY, leftW - 20, 32)) {
                selectedId = waypoint.id;
                restoreDraftsAfterPicker = false;
                loadSelection();
                UiSoundManager.click();
                return true;
            }
            rowY += 38;
        }

        if (selected() != null && inside(click.comp_4798(), click.comp_4799(), rightX + 48, 192, Math.max(80, rightW - 60), 26)) {
            captureDraftFields();
            restoreDraftsAfterPicker = true;
            clearSaveFeedback();
            UiSoundManager.click();
            if (field_22787 != null) {
                field_22787.method_1507(
                        new ColorPickerScreen(
                                this,
                                "Waypoint Color",
                                draftColor,
                                rgb -> draftColor = 0xFF000000 | (rgb & 0xFFFFFF)
                        )
                );
            }
            return true;
        }

        if (selected() != null && inside(click.comp_4798(), click.comp_4799(), rightX + 12, 226, Math.max(80, rightW - 24), 26)) {
            saveFields();
            return true;
        }

        return super.method_25402(click, doubled);
    }

    private void saveFields() {
        WaypointManager.Waypoint waypoint = selected();
        if (waypoint == null) return;

        try {
            waypoint.name = nameField.method_1882().isBlank() ? "Waypoint" : nameField.method_1882().trim();
            waypoint.x = Double.parseDouble(xField.method_1882().trim());
            waypoint.y = Double.parseDouble(yField.method_1882().trim());
            waypoint.z = Double.parseDouble(zField.method_1882().trim());
            waypoint.color = draftColor | 0xFF000000;

            WaypointManager.upsert(waypoint);
            UiSoundManager.click();

            draftName = waypoint.name;
            draftX = formatCoord(waypoint.x);
            draftY = formatCoord(waypoint.y);
            draftZ = formatCoord(waypoint.z);
            nameField.method_1852(draftName);
            xField.method_1852(draftX);
            yField.method_1852(draftY);
            zField.method_1852(draftZ);

            saveFailed = false;
            saveFeedbackUntil = System.currentTimeMillis() + 1800L;
        } catch (NumberFormatException ignored) {
            
            saveFailed = true;
            saveFeedbackUntil = System.currentTimeMillis() + 1800L;
        }
    }

    private void cycleDimension(int direction) {
        List<String> dimensions = WaypointManager.knownDimensions();
        if (dimensions.isEmpty()) return;
        int index = dimensions.indexOf(viewedDimension);
        if (index < 0) index = 0;
        index = Math.floorMod(index + direction, dimensions.size());
        viewedDimension = dimensions.get(index);
        selectedId = null;
        scroll = 0;
        restoreDraftsAfterPicker = false;
        loadSelection();
        UiSoundManager.click();
    }

    @Override
    public boolean method_25401(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int max = Math.max(0, list().size() * 38 - Math.max(0, field_22790 - 170));
        scroll -= (int) Math.round(verticalAmount * 38.0);
        scroll = Math.max(0, Math.min(max, scroll));
        return true;
    }

    @Override
    public void method_25419() {
        if (field_22787 != null) field_22787.method_1507(parent);
    }

    @Override
    public boolean method_25421() {
        return false;
    }

    private void drawButton(class_332 context, int x, int y, int w, int h, String label, int mouseX, int mouseY) {
        boolean hovered = inside(mouseX, mouseY, x, y, w, h);
        context.method_25294(x, y, x + w, y + h, hovered ? HOVER : CARD);
        outline(context, x, y, w, h, hovered ? ORANGE : BORDER);
        context.method_25300(field_22793, field_22793.method_27523(label, Math.max(8, w - 8)), x + w / 2, y + (h - field_22793.field_2000) / 2, TEXT);
    }

    private static void outline(class_332 context, int x, int y, int w, int h, int color) {
        context.method_25294(x, y, x + w, y + 1, color);
        context.method_25294(x, y + h - 1, x + w, y + h, color);
        context.method_25294(x, y, x + 1, y + h, color);
        context.method_25294(x + w - 1, y, x + w, y + h, color);
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }
}
