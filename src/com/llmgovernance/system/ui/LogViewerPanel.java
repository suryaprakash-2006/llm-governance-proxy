package com.llmgovernance.system.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * LogViewerPanel provides a read-only audit log viewer.
 *
 * Features:
 * - Background loading using SwingWorker
 * - Refresh button
 * - Optional keyword filter
 */
public class LogViewerPanel extends JPanel {

    private static final int MAX_LINES = 500;
    private static final Path LOG_PATH = Path.of(System.getProperty("user.home"), "llm_governance", "logs", "app.log");

    private final JTextArea taLogs;
    private final JTextField tfFilter;
    private final JLabel lblStatus;
    private volatile List<String> cachedLines = List.of();

    public LogViewerPanel() {
        setLayout(new BorderLayout(8, 8));
        setBorder(new EmptyBorder(10, 10, 10, 10));
        setBackground(new Color(13, 15, 23));

        JPanel topBar = new JPanel(new BorderLayout(8, 0));
        topBar.setOpaque(false);

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.setOpaque(false);

        JLabel lblFilter = new JLabel("Filter:");
        lblFilter.setForeground(new Color(226, 232, 240));
        tfFilter = new JTextField(24);
        tfFilter.addActionListener(e -> applyFilterFromCache());

        JButton btnRefresh = new JButton("Refresh Logs");
        btnRefresh.addActionListener(e -> loadLogsAsync());

        JButton btnApplyFilter = new JButton("Apply Filter");
        btnApplyFilter.addActionListener(e -> applyFilterFromCache());

        left.add(lblFilter);
        left.add(tfFilter);
        left.add(btnApplyFilter);
        left.add(btnRefresh);

        lblStatus = new JLabel("Ready.");
        lblStatus.setForeground(new Color(148, 163, 184));

        topBar.add(left, BorderLayout.WEST);
        topBar.add(lblStatus, BorderLayout.EAST);

        taLogs = new JTextArea();
        taLogs.setEditable(false);
        taLogs.setFont(new Font("Monospaced", Font.PLAIN, 12));
        taLogs.setLineWrap(false);
        taLogs.setBackground(new Color(18, 20, 33));
        taLogs.setForeground(new Color(226, 232, 240));
        taLogs.setCaretColor(new Color(226, 232, 240));

        JScrollPane scrollPane = new JScrollPane(taLogs);
        scrollPane.getViewport().setBackground(new Color(18, 20, 33));

        add(topBar, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        loadLogsAsync();
    }

    public void loadLogsAsync() {
        lblStatus.setText("Loading...");

        SwingWorker<List<String>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<String> doInBackground() {
                if (!Files.exists(LOG_PATH)) {
                    return List.of();
                }
                try {
                    List<String> lines = Files.readAllLines(LOG_PATH, StandardCharsets.UTF_8);
                    if (lines.size() <= MAX_LINES) {
                        return lines;
                    }
                    return new ArrayList<>(lines.subList(lines.size() - MAX_LINES, lines.size()));
                } catch (IOException e) {
                    return List.of("Failed to read logs: " + e.getMessage());
                }
            }

            @Override
            protected void done() {
                try {
                    cachedLines = get();
                    applyFilterFromCache();
                    lblStatus.setText("Loaded " + cachedLines.size() + " line(s).");
                } catch (Exception e) {
                    taLogs.setText("Failed to load logs: " + e.getMessage());
                    lblStatus.setText("Error.");
                }
            }
        };

        worker.execute();
    }

    private void applyFilterFromCache() {
        String filter = tfFilter.getText() == null ? "" : tfFilter.getText().trim().toLowerCase();
        List<String> source = cachedLines;

        if (source == null || source.isEmpty()) {
            taLogs.setText("No logs available yet");
            taLogs.setCaretPosition(0);
            lblStatus.setText("No logs.");
            return;
        }

        List<String> filtered;
        if (filter.isEmpty()) {
            filtered = source;
        } else {
            filtered = source.stream()
                    .filter(line -> line.toLowerCase().contains(filter))
                    .collect(Collectors.toList());
        }

        if (filtered.isEmpty()) {
            taLogs.setText("No log entries matched filter: " + filter);
        } else {
            taLogs.setText(String.join("\n", filtered));
        }
        taLogs.setCaretPosition(0);
    }
}
