package com.llmgovernance.system.ui;

import com.llmgovernance.system.user.ReplyService;
import com.llmgovernance.system.user.ReplyService.ProcessResult;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * MainFrame – Swing UI for the LLM Governance System.
 *
 * Local-first mode using Ollama (no paid API required).
 *
 * Layout:
 *  [HEADER]
 *  [LOCAL MODEL BAR]
 *  ┌─ Left: Input + Buttons ─┬─ Right: 5 Output Cards ──────────────┐
 *  │                         │  Detection | Filtered                 │
 *  │                         │  Local LLM Response (Ollama)         │
 *  │                         │  Governance Summary / Status         │
 *  └─────────────────────────┴──────────────────────────────────────┘
 *  [STATUS BAR]
 */
public class MainFrame extends JFrame {

    private final ReplyService service = new ReplyService();

    // Colours
    private static final Color CLR_BG      = new Color(13,  15,  23);
    private static final Color CLR_PANEL   = new Color(22,  25,  38);
    private static final Color CLR_CARD    = new Color(30,  34,  52);
    private static final Color CLR_ACCENT  = new Color(99,  179, 237);  // blue
    private static final Color CLR_FREE    = new Color(104, 211, 145);  // green = free!
    private static final Color CLR_WARN    = new Color(246, 173, 85);   // amber
    private static final Color CLR_OK      = new Color(104, 211, 145);  // green
    private static final Color CLR_TEXT    = new Color(226, 232, 240);
    private static final Color CLR_SUBTEXT = new Color(148, 163, 184);
    private static final Color CLR_BORDER  = new Color(55,  65,  81);
    private static final Color CLR_GEMINI  = new Color(138, 180, 248);  // Google blue

    // Fonts
    private static final Font FONT_TITLE  = new Font("Segoe UI", Font.BOLD,  17);
    private static final Font FONT_LABEL  = new Font("Segoe UI", Font.BOLD,  12);
    private static final Font FONT_BODY   = new Font("Monospaced", Font.PLAIN, 12);
    private static final Font FONT_BTN    = new Font("Segoe UI", Font.BOLD,  12);
    private static final Font FONT_STATUS = new Font("Segoe UI", Font.PLAIN, 11);

    // Widgets
    private JTextArea     taInput, taDetection, taFiltered;
    private JTextArea     taLlmResponse, taGovernance;
    private JLabel        lblStatus, lblHash, lblRecords, lblApiStatus;
    private JTextField tfLocalModel;

    public MainFrame() {
        super("LLM Governance System — Local Ollama Mode");
        initLAF();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1300, 840);
        setMinimumSize(new Dimension(980, 660));
        setLocationRelativeTo(null);
        getContentPane().setBackground(CLR_BG);
        setLayout(new BorderLayout());

        JPanel north = new JPanel(new BorderLayout());
        north.setOpaque(false);
        north.add(buildHeader(),    BorderLayout.NORTH);
        north.add(buildApiKeyBar(), BorderLayout.SOUTH);

        add(north,             BorderLayout.NORTH);
        add(buildCenterTabs(), BorderLayout.CENTER);
        add(buildStatusBar(),  BorderLayout.SOUTH);

        updateRecordCount();
    }

    // ── Look & feel ───────────────────────────────────────────────────────────
    private void initLAF() {
        try { UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()); }
        catch (Exception ignored) {}
        UIManager.put("OptionPane.background",        CLR_PANEL);
        UIManager.put("Panel.background",             CLR_PANEL);
        UIManager.put("OptionPane.messageForeground", CLR_TEXT);
    }

    // ── Header ────────────────────────────────────────────────────────────────
    private JPanel buildHeader() {
        JPanel h = new JPanel(new BorderLayout());
        h.setBackground(CLR_PANEL);
        h.setBorder(new CompoundBorder(
                new MatteBorder(0,0,1,0,CLR_BORDER),
                new EmptyBorder(10,14,10,14)));

        JLabel title = new JLabel("🔒 LLM Governance System");
        title.setFont(FONT_TITLE);
        title.setForeground(CLR_ACCENT);

        JLabel tag = new JLabel("Local Ollama  •  Governance-first  •  Offline-friendly");
        tag.setFont(new Font("Segoe UI", Font.BOLD, 11));
        tag.setForeground(CLR_FREE);

        h.add(title, BorderLayout.WEST);
        h.add(tag,   BorderLayout.EAST);
        return h;
    }

    // ── Local model bar ──────────────────────────────────────────────────────
    private JPanel buildApiKeyBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 7));
        bar.setBackground(new Color(18, 21, 34));
        bar.setBorder(new MatteBorder(0,0,1,0,CLR_BORDER));

        JLabel lbl = new JLabel("🧠 Local Model (Ollama):");
        lbl.setFont(FONT_LABEL);
        lbl.setForeground(CLR_FREE);

        tfLocalModel = new JTextField(service.getLocalModel(), 24);
        tfLocalModel.setFont(new Font("Monospaced", Font.PLAIN, 12));
        tfLocalModel.setBackground(new Color(24, 27, 44));
        tfLocalModel.setForeground(CLR_TEXT);
        tfLocalModel.setCaretColor(CLR_FREE);
        tfLocalModel.setBorder(new CompoundBorder(
                new LineBorder(CLR_BORDER, 1, true),
                new EmptyBorder(4, 8, 4, 8)));
        tfLocalModel.setToolTipText("Example models: llama3.2, mistral, gemma:2b");

        JButton btnSet = makeButton("✔ Apply Model", CLR_FREE);
        btnSet.setForeground(new Color(5, 30, 15));
        btnSet.addActionListener(e -> {
            String model = tfLocalModel.getText().trim();
            if (model.isEmpty()) {
                showError("Please enter a local model name, e.g. llama3.2");
                return;
            }
            service.setLocalModel(model);
            lblApiStatus.setText("✅ Model set — " + model);
            lblApiStatus.setForeground(CLR_FREE);
            status("Local model configured. Type your text and click 'Analyze + Ask Local LLM'.");
        });

        lblApiStatus = new JLabel("ℹ️ Start Ollama first (localhost:11434)");
        lblApiStatus.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblApiStatus.setForeground(CLR_WARN);

        JLabel hint = new JLabel("  Example: ollama run llama3.2");
        hint.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        hint.setForeground(CLR_SUBTEXT);

        bar.add(lbl);
        bar.add(tfLocalModel);
        bar.add(btnSet);
        bar.add(lblApiStatus);
        bar.add(hint);
        return bar;
    }

    // ── Main split pane ───────────────────────────────────────────────────────
    private JTabbedPane buildCenterTabs() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Governance", buildMainPanel());
        tabs.addTab("Logs", new LogViewerPanel());
        tabs.setBackground(CLR_PANEL);
        tabs.setForeground(CLR_TEXT);
        return tabs;
    }

    private JSplitPane buildMainPanel() {
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                buildLeftPanel(), buildRightPanel());
        split.setDividerLocation(400);
        split.setDividerSize(4);
        split.setBackground(CLR_BG);
        split.setBorder(null);
        return split;
    }

    // ── Left panel ────────────────────────────────────────────────────────────
    private JPanel buildLeftPanel() {
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setBackground(CLR_BG);
        p.setBorder(new EmptyBorder(10, 12, 10, 6));

        taInput = new JTextArea(12, 28);
        taInput.setFont(FONT_BODY);
        taInput.setBackground(new Color(18, 20, 33));
        taInput.setForeground(CLR_TEXT);
        taInput.setCaretColor(CLR_ACCENT);
        taInput.setLineWrap(true);
        taInput.setWrapStyleWord(true);
        taInput.setBorder(new EmptyBorder(8,8,8,8));
        taInput.setToolTipText("Type or paste text. Sensitive data is masked before sending to local LLM.");
        JScrollPane spInput = new JScrollPane(taInput);
        spInput.setBorder(null);
        styleScroll(spInput);

        p.add(buildCard("📝 Input Text", spInput), BorderLayout.CENTER);

        JPanel south = new JPanel(new BorderLayout(0, 6));
        south.setOpaque(false);
        south.add(buildButtons(),   BorderLayout.NORTH);
        south.add(buildHashPanel(), BorderLayout.CENTER);
        p.add(south, BorderLayout.SOUTH);
        return p;
    }

    private JPanel buildButtons() {
        JPanel row = new JPanel(new GridLayout(2, 2, 8, 8));
        row.setOpaque(false);
        row.setBorder(new EmptyBorder(6, 0, 4, 0));

        JButton btnAnalyze    = makeButton("🔍 Analyze + Ask Local LLM", CLR_GEMINI);
        JButton btnLoad       = makeButton("📂 Load File",           CLR_SUBTEXT);
        JButton btnClear      = makeButton("🗑️  Clear All",           CLR_SUBTEXT);
        JButton btnHistory    = makeButton("📋 History",             CLR_SUBTEXT);

        row.add(btnAnalyze);
        row.add(btnLoad);
        row.add(btnClear);
        row.add(btnHistory);

        // ── ANALYZE ──────────────────────────────────────────────────────────
        btnAnalyze.addActionListener(e -> {
            String text = taInput.getText().trim();
            if (text.isEmpty()) {
                showError("Input is empty. Please type or paste some text first.");
                return;
            }
            btnAnalyze.setEnabled(false);
            btnAnalyze.setText("⏳ Asking Local LLM...");
            status("Masking sensitive data -> Sending to local Ollama model...");

            SwingWorker<ProcessResult, Void> worker = new SwingWorker<>() {
                @Override protected ProcessResult doInBackground() throws Exception {
                    return service.analyze(text);
                }
                @Override protected void done() {
                    btnAnalyze.setEnabled(true);
                    btnAnalyze.setText("🔍 Analyze + Ask Local LLM");
                    try {
                        ProcessResult r = get();
                        taDetection  .setText(r.detectionSummary);
                        taFiltered   .setText(r.filteredText);
                        taLlmResponse.setText(r.llmResponse);
                        taGovernance  .setText(
                                "Input policy: " + (r.llmCalled ? "allowed" : "processed") + "\n"
                                        + "Stored record: #" + r.savedId + "\n"
                                        + "Sensitive found: " + (r.sensitiveFound ? "yes" : "no") + "\n"
                                        + "Governance status: active");
                        lblHash.setText("SHA-256: " + r.originalHash.substring(0,16) + "…");

                        String llmTag = r.llmCalled ? "✅ Local model responded." : "⚠️ Local model not called.";
                        status(llmTag + "  |  Record ID=" + r.savedId);
                        updateRecordCount();
                    } catch (Exception ex) {
                        showError("Pipeline error:\n" + ex.getMessage());
                        status("❌ Error during analysis.");
                    }
                }
            };
            worker.execute();
        });

        btnLoad.addActionListener(e -> loadFile());
        btnClear.addActionListener(e -> clearAll());
        btnHistory.addActionListener(e -> showHistory());

        return row;
    }

    private JPanel buildHashPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(CLR_CARD);
        p.setBorder(new CompoundBorder(
                new LineBorder(CLR_BORDER, 1, true),
                new EmptyBorder(5, 10, 5, 10)));
        lblHash = new JLabel("SHA-256: —");
        lblHash.setFont(new Font("Monospaced", Font.PLAIN, 11));
        lblHash.setForeground(CLR_SUBTEXT);
        p.add(lblHash);
        return p;
    }

    // ── Right panel (5 output cards) ──────────────────────────────────────────
    private JPanel buildRightPanel() {
        JPanel p = new JPanel(new GridLayout(4, 1, 0, 8));
        p.setBackground(CLR_BG);
        p.setBorder(new EmptyBorder(10, 6, 10, 12));

        taDetection    = buildOutput(CLR_WARN);
        taFiltered     = buildOutput(CLR_TEXT);
        taLlmResponse  = buildOutput(CLR_GEMINI);
        taGovernance   = buildOutput(CLR_OK);

        p.add(buildCard("🔎 Detection Report",                      sp(taDetection)));
        p.add(buildCard("🛡️  Filtered / Masked (sent to local model)", sp(taFiltered)));
        p.add(buildCard("🤖 Local LLM Response  [Ollama]", sp(taLlmResponse)));
        p.add(buildCard("🛡️  Governance Summary",                    sp(taGovernance)));
        return p;
    }

    // ── Status bar ────────────────────────────────────────────────────────────
    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(new Color(16, 18, 28));
        bar.setBorder(new CompoundBorder(
                new MatteBorder(1,0,0,0,CLR_BORDER),
                new EmptyBorder(4,12,4,12)));

        lblStatus = new JLabel("Ready. Start Ollama locally and run a model (e.g. ollama run llama3.2).");
        lblStatus.setFont(FONT_STATUS);
        lblStatus.setForeground(CLR_SUBTEXT);

        lblRecords = new JLabel("DB Records: 0");
        lblRecords.setFont(FONT_STATUS);
        lblRecords.setForeground(CLR_SUBTEXT);

        bar.add(lblStatus,  BorderLayout.WEST);
        bar.add(lblRecords, BorderLayout.EAST);
        return bar;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private JTextArea buildOutput(Color fg) {
        JTextArea ta = new JTextArea();
        ta.setFont(FONT_BODY);
        ta.setBackground(new Color(18, 20, 33));
        ta.setForeground(fg);
        ta.setEditable(false);
        ta.setLineWrap(true);
        ta.setWrapStyleWord(true);
        ta.setBorder(new EmptyBorder(6, 8, 6, 8));
        return ta;
    }

    private JScrollPane sp(JTextArea ta) {
        JScrollPane s = new JScrollPane(ta);
        s.setBorder(null);
        styleScroll(s);
        return s;
    }

    private void styleScroll(JScrollPane sp) {
        sp.getViewport().setBackground(new Color(18, 20, 33));
        sp.setBackground(new Color(18, 20, 33));
    }

    private JPanel buildCard(String title, JComponent content) {
        JPanel card = new JPanel(new BorderLayout(0, 4));
        card.setBackground(CLR_CARD);
        card.setBorder(new CompoundBorder(
                new LineBorder(CLR_BORDER, 1, true),
                new EmptyBorder(6, 8, 8, 8)));
        JLabel lbl = new JLabel(title);
        lbl.setFont(FONT_LABEL);
        lbl.setForeground(CLR_ACCENT);
        lbl.setBorder(new EmptyBorder(0,0,4,0));
        card.add(lbl,     BorderLayout.NORTH);
        card.add(content, BorderLayout.CENTER);
        return card;
    }

    private JButton makeButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_BTN);
        btn.setForeground(CLR_BG);
        btn.setBackground(bg);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setOpaque(true);
        btn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { btn.setBackground(bg.brighter()); }
            @Override public void mouseExited (MouseEvent e) { btn.setBackground(bg); }
        });
        return btn;
    }

    private void status(String msg)      { lblStatus.setText(msg); }
    private void showError(String msg)   { JOptionPane.showMessageDialog(this, msg, "Error",   JOptionPane.ERROR_MESSAGE); }
    private void updateRecordCount()     { lblRecords.setText("DB Records: " + service.recordCount()); }

    private void clearAll() {
        taInput.setText(""); taDetection.setText(""); taFiltered.setText("");
        taLlmResponse.setText(""); taGovernance.setText("");
        lblHash.setText("SHA-256: —");
        status("Cleared.");
    }

    private void loadFile() {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new FileNameExtensionFilter("Text files (*.txt)", "txt"));
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                String content = new String(
                        Files.readAllBytes(fc.getSelectedFile().toPath()), StandardCharsets.UTF_8);
                taInput.setText(content);
                status("Loaded: " + fc.getSelectedFile().getName());
            } catch (IOException ex) { showError("Cannot read file: " + ex.getMessage()); }
        }
    }

    private void showHistory() {
        java.util.List<com.llmgovernance.system.model.Prompt> records =
                new com.llmgovernance.system.db.DataDAO().loadAll();
        if (records.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No records yet. Run Analyze first.",
                    "History", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-4s  %-22s  %-18s%n", "ID", "Timestamp", "Hash (first 16)"));
        sb.append("─".repeat(50)).append("\n");
        for (com.llmgovernance.system.model.Prompt pr : records) {
            String h = pr.getOriginalHash().length() >= 16
                    ? pr.getOriginalHash().substring(0,16) + "…" : pr.getOriginalHash();
            sb.append(String.format("%-4d  %-22s  %-18s%n",
                    pr.getId(), pr.getTimestamp(), h));
        }
        JTextArea ta = new JTextArea(sb.toString());
        ta.setFont(new Font("Monospaced", Font.PLAIN, 12));
        ta.setEditable(false);
        ta.setBackground(CLR_PANEL);
        ta.setForeground(CLR_TEXT);
        JScrollPane sp = new JScrollPane(ta);
        sp.setPreferredSize(new Dimension(560, 300));
        JOptionPane.showMessageDialog(this, sp, "Stored Records", JOptionPane.INFORMATION_MESSAGE);
    }
}
