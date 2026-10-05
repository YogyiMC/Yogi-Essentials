package dev.yogi.yogiessentials.recovery;

import dev.yogi.yogiessentials.recovery.RecoveryAnalysis.Confidence;
import dev.yogi.yogiessentials.recovery.RecoveryAnalysis.FixAction;
import dev.yogi.yogiessentials.recovery.RecoveryAnalysis.Suspect;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class RecoveryWindow {
    private static final Color BACKGROUND = new Color(12, 12, 15);
    private static final Color PANEL = new Color(20, 20, 24);
    private static final Color PANEL_ALT = new Color(27, 27, 32);
    private static final Color ORANGE = new Color(255, 106, 0);
    private static final Color TEXT = new Color(244, 244, 245);
    private static final Color MUTED = new Color(165, 165, 173);
    private static final Color GREEN = new Color(100, 211, 145);
    private static final Color RED = new Color(255, 105, 105);

    private final Path gameDir;
    private final RecoveryAnalysis analysis;
    private final JFrame frame;
    private final JLabel statusLabel;
    private final JButton fixButton;
    private final JButton deleteButton;
    private final JButton restoreButton;
    private RecoveryFixManager.ApplyResult lastApplyResult;

    private RecoveryWindow(Path gameDir, RecoveryAnalysis analysis) {
        this.gameDir = gameDir;
        this.analysis = analysis;
        this.frame = new JFrame("Yogi Essentials — Crash Assistant");
        this.statusLabel = new JLabel(" ");
        this.fixButton = button("Fix All Detected", true);
        this.deleteButton = button("Delete Top Suspect", false);
        this.restoreButton = button("Restore Changes", false);
        build();
    }

    public static void show(Path gameDir, RecoveryAnalysis analysis) {
        SwingUtilities.invokeLater(() -> {
            try {
                applyLookAndFeel();
                presentFrame(new RecoveryWindow(gameDir, analysis).frame);
            } catch (Throwable throwable) {
                writeUiFailure(gameDir, throwable);
            }
        });
    }

    public static void showTextReport(String title, String report) {
        SwingUtilities.invokeLater(() -> {
            applyLookAndFeel();
            JFrame frame = baseFrame(title, 820, 600);
            JTextArea area = textArea(report);
            area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
            area.setBackground(new Color(9, 9, 12));
            area.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
            JScrollPane scroll = new JScrollPane(area);
            scroll.setBorder(BorderFactory.createLineBorder(new Color(48, 48, 56)));
            scroll.getViewport().setBackground(new Color(9, 9, 12));
            frame.add(scroll, BorderLayout.CENTER);
            JButton close = button("Close", false);
            close.addActionListener(event -> frame.dispose());
            JPanel footer = new JPanel(new BorderLayout());
            footer.setBackground(PANEL);
            footer.setBorder(BorderFactory.createEmptyBorder(8, 12, 10, 12));
            footer.add(close, BorderLayout.EAST);
            frame.add(footer, BorderLayout.SOUTH);
            presentFrame(frame);
        });
    }

    public static void showDisabledManager(Path gameDir) {
        SwingUtilities.invokeLater(() -> {
            applyLookAndFeel();
            JFrame frame = baseFrame("Yogi Essentials — Disabled Mods", 780, 520);
            JPanel root = new JPanel(new BorderLayout(10, 10));
            root.setBackground(BACKGROUND);
            root.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

            JLabel heading = new JLabel("DISABLED MODS");
            heading.setForeground(ORANGE);
            heading.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
            root.add(heading, BorderLayout.NORTH);

            DefaultListModel<String> model = new DefaultListModel<>();
            JList<String> list = new JList<>(model);
            list.setBackground(PANEL_ALT);
            list.setForeground(TEXT);
            list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            list.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
            JScrollPane scroll = new JScrollPane(list);
            scroll.setBorder(BorderFactory.createLineBorder(new Color(48, 48, 56)));
            root.add(scroll, BorderLayout.CENTER);

            JLabel status = new JLabel(" ");
            status.setForeground(MUTED);
            List<RecoveryFixManager.DisabledEntry> entries = new ArrayList<>();
            Runnable refresh = () -> {
                entries.clear();
                entries.addAll(RecoveryFixManager.listDisabled(gameDir));
                model.clear();
                for (RecoveryFixManager.DisabledEntry entry : entries) {
                    model.addElement(entry.displayName() + " — " + RecoveryAnalyzer.timestamp(entry.timestamp()));
                }
                if (entries.isEmpty()) {
                    model.addElement("No mods are currently disabled by Crash Assistant.");
                }
            };
            refresh.run();

            JButton restore = button("Restore Selected", true);
            restore.addActionListener(event -> {
                int index = list.getSelectedIndex();
                if (index < 0 || index >= entries.size()) {
                    status.setForeground(MUTED);
                    status.setText("Select a disabled mod first.");
                    return;
                }
                RecoveryFixManager.DisabledEntry entry = entries.get(index);
                try {
                    Path restored = RecoveryFixManager.restoreDisabled(gameDir, entry);
                    status.setForeground(GREEN);
                    status.setText("Restored to " + restored.getFileName());
                    refresh.run();
                } catch (IOException exception) {
                    status.setForeground(RED);
                    status.setText("Restore failed: " + exception.getMessage());
                }
            });

            JButton delete = button("Delete Permanently", false);
            delete.addActionListener(event -> {
                int index = list.getSelectedIndex();
                if (index < 0 || index >= entries.size()) {
                    status.setForeground(MUTED);
                    status.setText("Select a disabled mod first.");
                    return;
                }
                RecoveryFixManager.DisabledEntry entry = entries.get(index);
                int answer = JOptionPane.showConfirmDialog(
                        frame,
                        "Permanently delete this disabled mod?\n\n" + entry.disabledPath() + "\n\nThis action cannot be undone by Crash Assistant.",
                        "Confirm permanent deletion",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE
                );
                if (answer != JOptionPane.YES_OPTION) {
                    return;
                }
                try {
                    RecoveryFixManager.deleteDisabled(gameDir, entry);
                    status.setForeground(GREEN);
                    status.setText("Deleted " + entry.displayName() + ".");
                    refresh.run();
                } catch (IOException exception) {
                    status.setForeground(RED);
                    status.setText("Delete failed: " + exception.getMessage());
                }
            });

            JButton folder = button("Open Disabled Folder", false);
            folder.addActionListener(event -> openDesktopPath(gameDir.resolve("yogiessentials").resolve("disabled-mods"), status));
            JButton close = button("Close", false);
            close.addActionListener(event -> frame.dispose());

            JPanel buttons = new JPanel();
            buttons.setBackground(BACKGROUND);
            buttons.setLayout(new BoxLayout(buttons, BoxLayout.X_AXIS));
            buttons.add(restore);
            buttons.add(Box.createHorizontalStrut(8));
            buttons.add(delete);
            buttons.add(Box.createHorizontalStrut(8));
            buttons.add(folder);
            buttons.add(Box.createHorizontalGlue());
            buttons.add(close);

            JPanel bottom = new JPanel(new BorderLayout(6, 6));
            bottom.setBackground(BACKGROUND);
            bottom.add(status, BorderLayout.NORTH);
            bottom.add(buttons, BorderLayout.CENTER);
            root.add(bottom, BorderLayout.SOUTH);
            frame.add(root, BorderLayout.CENTER);
            presentFrame(frame);
        });
    }

    public static void showHistory(Path gameDir) {
        SwingUtilities.invokeLater(() -> {
            applyLookAndFeel();
            JFrame frame = baseFrame("Yogi Essentials — Crash History", 900, 590);
            List<RecoveryHistory.HistoryEntry> entries = RecoveryHistory.list(gameDir);
            DefaultListModel<String> model = new DefaultListModel<>();
            for (RecoveryHistory.HistoryEntry entry : entries) {
                String suspect = entry.suspect() == null || entry.suspect().isBlank() ? "No suspect" : entry.suspect();
                model.addElement(RecoveryAnalyzer.timestamp(entry.timestamp()) + " — " + entry.title() + " — " + suspect);
            }
            if (entries.isEmpty()) {
                model.addElement("No real crashes have been recorded yet.");
            }

            JList<String> list = new JList<>(model);
            list.setBackground(PANEL_ALT);
            list.setForeground(TEXT);
            list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            list.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
            JTextArea details = textArea(entries.isEmpty() ? "Crash history is empty." : entries.get(0).details());
            details.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
            details.setBackground(new Color(9, 9, 12));
            list.addListSelectionListener(event -> {
                int index = list.getSelectedIndex();
                if (!event.getValueIsAdjusting() && index >= 0 && index < entries.size()) {
                    details.setText(entries.get(index).details());
                    details.setCaretPosition(0);
                }
            });
            if (!entries.isEmpty()) {
                list.setSelectedIndex(0);
            }

            JScrollPane left = new JScrollPane(list);
            JScrollPane right = new JScrollPane(details);
            left.setBorder(BorderFactory.createLineBorder(new Color(48, 48, 56)));
            right.setBorder(BorderFactory.createLineBorder(new Color(48, 48, 56)));
            JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
            split.setResizeWeight(0.38);
            split.setDividerLocation(340);
            split.setBorder(BorderFactory.createEmptyBorder(12, 12, 8, 12));
            split.setBackground(BACKGROUND);
            frame.add(split, BorderLayout.CENTER);

            JButton close = button("Close", false);
            close.addActionListener(event -> frame.dispose());
            JPanel footer = new JPanel(new BorderLayout());
            footer.setBackground(PANEL);
            footer.setBorder(BorderFactory.createEmptyBorder(8, 12, 10, 12));
            footer.add(close, BorderLayout.EAST);
            frame.add(footer, BorderLayout.SOUTH);
            presentFrame(frame);
        });
    }

    private void build() {
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setMinimumSize(new Dimension(760, 560));
        frame.setSize(900, 690);
        frame.setLocationRelativeTo(null);
        frame.getContentPane().setBackground(BACKGROUND);
        frame.setLayout(new BorderLayout(0, 0));
        setFrameIcon(frame);

        JPanel header = new JPanel();
        header.setBackground(PANEL);
        header.setBorder(BorderFactory.createMatteBorder(3, 0, 1, 0, ORANGE));
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));

        JLabel title = new JLabel(analysis.title());
        title.setForeground(TEXT);
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 22));
        title.setBorder(BorderFactory.createEmptyBorder(16, 18, 3, 18));
        header.add(title);

        JLabel confidence = new JLabel("Confidence: " + analysis.confidence());
        confidence.setForeground(confidenceColor(analysis.confidence()));
        confidence.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        confidence.setBorder(BorderFactory.createEmptyBorder(0, 18, analysis.testMode() ? 3 : 12, 18));
        header.add(confidence);

        if (analysis.testMode()) {
            JLabel test = new JLabel("TEST MODE — only harmless sandbox files can be changed");
            test.setForeground(ORANGE);
            test.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
            test.setBorder(BorderFactory.createEmptyBorder(0, 18, 12, 18));
            header.add(test);
        }
        frame.add(header, BorderLayout.NORTH);

        JPanel center = new JPanel();
        center.setBackground(BACKGROUND);
        center.setBorder(BorderFactory.createEmptyBorder(14, 16, 10, 16));
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));

        JTextArea summary = textArea(analysis.summary());
        summary.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        summary.setMaximumSize(new Dimension(Integer.MAX_VALUE, 62));
        center.add(summary);
        center.add(Box.createVerticalStrut(8));

        if (!analysis.suspects().isEmpty()) {
            JPanel suspectPanel = new JPanel(new BorderLayout(8, 4));
            Suspect primary = analysis.suspects().get(0);
            suspectPanel.setBackground(PANEL_ALT);
            suspectPanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(confidenceColor(primary.confidence())),
                    BorderFactory.createEmptyBorder(10, 12, 10, 12)
            ));
            JLabel suspectTitle = new JLabel("Top suspect: " + primary.displayName() + "  (" + primary.id() + ") — " + primary.confidence());
            suspectTitle.setForeground(TEXT);
            suspectTitle.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
            suspectPanel.add(suspectTitle, BorderLayout.NORTH);

            StringBuilder ranking = new StringBuilder(primary.reason());
            for (int i = 1; i < Math.min(analysis.suspects().size(), 4); i++) {
                Suspect suspect = analysis.suspects().get(i);
                ranking.append("\n#").append(i + 1).append(" ").append(suspect.displayName()).append(" — ").append(suspect.confidence()).append(" — ").append(suspect.reason());
            }
            JTextArea reason = textArea(ranking.toString());
            reason.setForeground(MUTED);
            reason.setRows(Math.min(5, analysis.suspects().size() + 1));
            suspectPanel.add(reason, BorderLayout.CENTER);
            center.add(suspectPanel);
            center.add(Box.createVerticalStrut(10));
        }

        JTextArea details = textArea(analysis.details());
        details.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        details.setBackground(new Color(9, 9, 12));
        details.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        JScrollPane scroll = new JScrollPane(details);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(48, 48, 56)));
        scroll.getViewport().setBackground(new Color(9, 9, 12));
        center.add(scroll);
        frame.add(center, BorderLayout.CENTER);

        JPanel footer = new JPanel();
        footer.setBackground(PANEL);
        footer.setBorder(BorderFactory.createEmptyBorder(8, 14, 12, 14));
        footer.setLayout(new BoxLayout(footer, BoxLayout.Y_AXIS));

        statusLabel.setForeground(MUTED);
        statusLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        statusLabel.setAlignmentX(0.0f);
        footer.add(statusLabel);
        footer.add(Box.createVerticalStrut(7));

        fixButton.setEnabled(!analysis.fixes().isEmpty());
        deleteButton.setEnabled(!analysis.suspects().isEmpty() && analysis.suspects().get(0).jarPath() != null);
        restoreButton.setEnabled(false);
        fixButton.addActionListener(event -> fixAutomatically());
        deleteButton.addActionListener(event -> deletePrimarySuspect());
        restoreButton.addActionListener(event -> restoreChanges());

        JPanel rowOne = actionRow();
        rowOne.add(fixButton);
        rowOne.add(Box.createHorizontalStrut(7));
        rowOne.add(deleteButton);
        rowOne.add(Box.createHorizontalStrut(7));
        rowOne.add(restoreButton);
        rowOne.add(Box.createHorizontalGlue());
        JButton reportButton = button("Open Report", false);
        reportButton.setEnabled(analysis.sourcePath() != null && Files.exists(analysis.sourcePath()));
        reportButton.addActionListener(event -> openPath(analysis.sourcePath()));
        JButton copyButton = button("Copy Details", false);
        copyButton.addActionListener(event -> copyDetails());
        rowOne.add(reportButton);
        rowOne.add(Box.createHorizontalStrut(7));
        rowOne.add(copyButton);
        footer.add(rowOne);
        footer.add(Box.createVerticalStrut(7));

        JPanel rowTwo = actionRow();
        JButton disabledButton = button("Disabled Mods", false);
        disabledButton.addActionListener(event -> showDisabledManager(gameDir));
        JButton historyButton = button("Crash History", false);
        historyButton.addActionListener(event -> showHistory(gameDir));
        JButton modsButton = button("Open Mods Folder", false);
        modsButton.addActionListener(event -> openPath(gameDir.resolve("mods")));
        JButton closeButton = button("Close", false);
        closeButton.addActionListener(event -> frame.dispose());
        rowTwo.add(disabledButton);
        rowTwo.add(Box.createHorizontalStrut(7));
        rowTwo.add(historyButton);
        rowTwo.add(Box.createHorizontalStrut(7));
        rowTwo.add(modsButton);
        rowTwo.add(Box.createHorizontalGlue());
        rowTwo.add(closeButton);
        footer.add(rowTwo);
        frame.add(footer, BorderLayout.SOUTH);
    }

    private void fixAutomatically() {
        if (analysis.fixes().isEmpty()) {
            setStatus("No safe automatic repair is available for this analysis.", RED);
            return;
        }

        JPanel plan = new JPanel();
        plan.setLayout(new BoxLayout(plan, BoxLayout.Y_AXIS));
        plan.add(new JLabel("Select the changes Crash Assistant should apply:"));
        plan.add(Box.createVerticalStrut(8));
        List<JCheckBox> boxes = new ArrayList<>();
        for (FixAction fix : analysis.fixes()) {
            JCheckBox box = new JCheckBox(fix.label() + "  [" + fix.confidence() + "]", fix.autoSelected());
            box.setToolTipText(fix.description());
            boxes.add(box);
            plan.add(box);
        }
        plan.add(Box.createVerticalStrut(8));
        plan.add(new JLabel("Mod disables are reversible. Config resets are backed up before the active file is removed."));

        int result = JOptionPane.showConfirmDialog(
                frame,
                plan,
                "Review automatic repair plan",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        if (result != JOptionPane.OK_OPTION) {
            return;
        }

        List<FixAction> selected = new ArrayList<>();
        for (int i = 0; i < boxes.size(); i++) {
            if (boxes.get(i).isSelected()) {
                selected.add(analysis.fixes().get(i));
            }
        }
        if (selected.isEmpty()) {
            setStatus("No repairs were selected.", MUTED);
            return;
        }

        java.util.Map<String, Long> selectedChoiceGroups = selected.stream()
                .filter(fix -> fix.choiceGroup() != null && !fix.choiceGroup().isBlank())
                .collect(java.util.stream.Collectors.groupingBy(FixAction::choiceGroup, java.util.stream.Collectors.counting()));
        boolean conflictingChoices = selectedChoiceGroups.values().stream().anyMatch(count -> count > 1L);
        if (conflictingChoices) {
            JOptionPane.showMessageDialog(
                    frame,
                    "Choose only one fix from each conflict group. Conflict choices are alternatives, while fixes for separate problems can be applied together.",
                    "Choose one fix per conflict",
                    JOptionPane.WARNING_MESSAGE
            );
            setStatus("Choose one fix from each conflicting pair/group, then run Fix All Detected again.", ORANGE);
            return;
        }

        lastApplyResult = RecoveryFixManager.apply(gameDir, analysis, selected);
        restoreButton.setEnabled(lastApplyResult.changedAnything());
        if (!lastApplyResult.errors().isEmpty()) {
            setStatus("Applied " + lastApplyResult.changes().size() + " change(s), but " + lastApplyResult.errors().size() + " failed. See details after copying if needed.", RED);
            return;
        }
        fixButton.setEnabled(false);
        deleteButton.setEnabled(false);
        setStatus("Repair complete. Applied " + lastApplyResult.changes().size() + " reversible change" + (lastApplyResult.changes().size() == 1 ? "" : "s") + ".", GREEN);
    }

    private void restoreChanges() {
        if (lastApplyResult == null || !lastApplyResult.changedAnything()) {
            setStatus("There are no changes from this window to restore.", MUTED);
            restoreButton.setEnabled(false);
            return;
        }
        RecoveryFixManager.RestoreResult result = RecoveryFixManager.restoreChanges(gameDir, lastApplyResult);
        if (!result.errors().isEmpty()) {
            setStatus("Restored " + result.restored() + " change(s); " + result.errors().size() + " restore(s) failed.", RED);
            return;
        }
        restoreButton.setEnabled(false);
        setStatus("Restored " + result.restored() + " change" + (result.restored() == 1 ? "" : "s") + ".", GREEN);
    }

    private void deletePrimarySuspect() {
        if (analysis.suspects().isEmpty()) {
            return;
        }
        Suspect suspect = analysis.suspects().get(0);
        Path path = suspect.jarPath();
        if (path == null || !Files.exists(path)) {
            setStatus("The suspect jar is no longer present.", RED);
            return;
        }
        String message = analysis.testMode()
                ? "Delete the harmless Crash Assistant sandbox file?"
                : "Permanently delete " + suspect.displayName() + "?\n\n" + path + "\n\nUse Fix All instead if you want a reversible disable. This deletion cannot be restored by Crash Assistant.";
        int result = JOptionPane.showConfirmDialog(
                frame,
                message,
                "Confirm deletion",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        if (result != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            RecoveryFixManager.deleteActiveMod(gameDir, path, analysis.testMode());
            fixButton.setEnabled(false);
            deleteButton.setEnabled(false);
            setStatus("Deleted " + suspect.displayName() + ".", GREEN);
        } catch (IOException exception) {
            setStatus("Delete failed: " + exception.getMessage(), RED);
        }
    }

    private void copyDetails() {
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(analysis.details()), null);
        setStatus("Crash details copied to clipboard.", GREEN);
    }

    private void openPath(Path path) {
        openDesktopPath(path, statusLabel);
    }

    private void setStatus(String text, Color color) {
        statusLabel.setText(text);
        statusLabel.setForeground(color);
    }

    private static JPanel actionRow() {
        JPanel panel = new JPanel();
        panel.setBackground(PANEL);
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        panel.setAlignmentX(0.0f);
        return panel;
    }


    private static void writeUiFailure(Path gameDir, Throwable throwable) {
        if (gameDir == null || throwable == null) {
            return;
        }
        try {
            Path file = gameDir.resolve("yogiessentials").resolve("recovery").resolve("recovery-ui-error.log");
            Files.createDirectories(file.getParent());
            String message = java.time.Instant.now() + " " + throwable.getClass().getName() + ": " + String.valueOf(throwable.getMessage()) + System.lineSeparator();
            Files.writeString(file, message, java.nio.charset.StandardCharsets.UTF_8, java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
        } catch (IOException ignored) {
        }
    }

    private static void presentFrame(JFrame frame) {
        frame.setState(JFrame.NORMAL);
        frame.setAutoRequestFocus(true);
        frame.setAlwaysOnTop(true);
        frame.setVisible(true);
        frame.toFront();
        frame.requestFocus();
        javax.swing.Timer timer = new javax.swing.Timer(1800, event -> {
            frame.setAlwaysOnTop(false);
            frame.toFront();
        });
        timer.setRepeats(false);
        timer.start();
    }

    private static JFrame baseFrame(String title, int width, int height) {
        JFrame frame = new JFrame(title);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setMinimumSize(new Dimension(Math.min(width, 640), Math.min(height, 420)));
        frame.setSize(width, height);
        frame.setLocationRelativeTo(null);
        frame.getContentPane().setBackground(BACKGROUND);
        frame.setLayout(new BorderLayout());
        setFrameIcon(frame);
        return frame;
    }

    private static void setFrameIcon(JFrame frame) {
        try {
            var resource = RecoveryWindow.class.getResource("/assets/yogiessentials/icon.png");
            if (resource != null) {
                frame.setIconImage(ImageIO.read(resource));
            }
        } catch (IOException ignored) {
        }
    }

    private static void applyLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception ignored) {
        }
    }

    private static void openDesktopPath(Path path, JLabel status) {
        if (path == null) {
            return;
        }
        try {
            if (Files.notExists(path) && path.getFileName() != null && !path.getFileName().toString().contains(".")) {
                Files.createDirectories(path);
            }
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(path.toFile());
            }
        } catch (IOException exception) {
            if (status != null) {
                status.setForeground(RED);
                status.setText("Could not open: " + path);
            }
        }
    }

    private static JTextArea textArea(String value) {
        JTextArea area = new JTextArea(value == null ? "" : value);
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setOpaque(false);
        area.setForeground(TEXT);
        area.setCaretColor(TEXT);
        area.setBorder(null);
        return area;
    }

    private static JButton button(String text, boolean primary) {
        JButton button = new JButton(text);
        button.setFocusPainted(false);
        button.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
        button.setForeground(TEXT);
        button.setBackground(primary ? ORANGE : PANEL_ALT);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(primary ? ORANGE : new Color(62, 62, 72)),
                BorderFactory.createEmptyBorder(7, 10, 7, 10)
        ));
        button.setHorizontalAlignment(SwingConstants.CENTER);
        return button;
    }

    private static Color confidenceColor(Confidence confidence) {
        if (confidence == Confidence.HIGH) {
            return GREEN;
        }
        if (confidence == Confidence.MEDIUM) {
            return ORANGE;
        }
        if (confidence == Confidence.LOW) {
            return new Color(255, 190, 90);
        }
        return MUTED;
    }
}
