package com.llmgovernance.system.ui;

import com.llmgovernance.system.db.DataDAO;
import com.llmgovernance.system.model.Policy;
import com.llmgovernance.system.model.Prompt;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * AdminDashboard – comprehensive admin control panel.
 *
 * Features:
 * - Logs: View all user activities
 * - Blocked Prompts: Show flagged/blocked requests
 * - Policies: Add/Edit/Delete security rules
 * - Users: View system users
 */
public class AdminDashboard extends JTabbedPane {

    private static final Color CLR_BG = new Color(13, 15, 23);
    private static final Color CLR_PANEL = new Color(22, 25, 38);
    private static final Color CLR_CARD = new Color(30, 34, 52);
    private static final Color CLR_ACCENT = new Color(99, 179, 237);
    private static final Color CLR_TEXT = new Color(226, 232, 240);
    private static final Color CLR_WARN = new Color(246, 173, 85);
    private static final Color CLR_OK = new Color(104, 211, 145);
    private static final Color CLR_BORDER = new Color(55, 65, 81);

    private final DataDAO dao = new DataDAO();

    private JTable tblLogs;
    private JTable tblBlockedPrompts;
    private JTable tblPolicies;
    private JTable tblUsers;

    public AdminDashboard() {
        setBackground(CLR_BG);
        setForeground(CLR_TEXT);
        
        addTab("Logs", buildLogsPanel());
        addTab("Blocked Prompts", buildBlockedPromptsPanel());
        addTab("Policies", buildPoliciesPanel());
        addTab("Users", buildUsersPanel());

        // Refresh data on tab change
        addChangeListener(e -> refreshCurrentTab());
    }

    // ── Logs Tab ──────────────────────────────────────────────────────────────

    private JPanel buildLogsPanel() {
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setBackground(CLR_BG);
        p.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        top.setOpaque(false);

        JButton btnRefresh = new JButton("Refresh");
        btnRefresh.addActionListener(e -> loadLogs());
        top.add(btnRefresh);

        JLabel lblStatus = new JLabel("Total Prompts: " + dao.countRecords());
        lblStatus.setForeground(CLR_TEXT);
        top.add(lblStatus);

        tblLogs = new JTable();
        tblLogs.setBackground(new Color(18, 20, 33));
        tblLogs.setForeground(CLR_TEXT);
        tblLogs.setSelectionBackground(CLR_ACCENT);
        JScrollPane spLogs = new JScrollPane(tblLogs);
        spLogs.getViewport().setBackground(new Color(18, 20, 33));

        p.add(top, BorderLayout.NORTH);
        p.add(spLogs, BorderLayout.CENTER);

        loadLogs();
        return p;
    }

    private void loadLogs() {
        List<DataDAO.LogRecord> logs = dao.loadAllLogs();
        String[] columns = {"ID", "User", "Status", "Prompt", "Response", "Timestamp"};

        if (logs.isEmpty()) {
            Object[][] emptyData = {{"-", "No audit logs found", "-", "-", "-", "Run Analyze to create logs"}};
            tblLogs.setModel(new DefaultTableModel(emptyData, columns) {
                @Override public boolean isCellEditable(int row, int col) { return false; }
            });
            return;
        }

        Object[][] data = new Object[logs.size()][6];

        for (int i = 0; i < logs.size(); i++) {
            DataDAO.LogRecord log = logs.get(i);
            String prompt = log.getPrompt();
            if (prompt != null && prompt.length() > 48) {
                prompt = prompt.substring(0, 48) + "…";
            }
            data[i] = new Object[]{
                    log.getId(),
                    log.getUsername(),
                    log.getStatus(),
                    prompt,
                    log.getResponse(),
                    log.getTimestamp()
            };
        }

        DefaultTableModel model = new DefaultTableModel(data, columns) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        tblLogs.setModel(model);
    }

    // ── Blocked Prompts Tab ───────────────────────────────────────────────────

    private JPanel buildBlockedPromptsPanel() {
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setBackground(CLR_BG);
        p.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        top.setOpaque(false);

        JButton btnRefresh = new JButton("Refresh");
        btnRefresh.addActionListener(e -> loadBlockedPrompts());
        top.add(btnRefresh);

        JLabel lblInfo = new JLabel("Shows prompts that matched governance rules");
        lblInfo.setForeground(CLR_ACCENT);
        top.add(lblInfo);

        tblBlockedPrompts = new JTable();
        tblBlockedPrompts.setBackground(new Color(18, 20, 33));
        tblBlockedPrompts.setForeground(CLR_WARN);
        tblBlockedPrompts.setSelectionBackground(CLR_WARN);
        JScrollPane sp = new JScrollPane(tblBlockedPrompts);
        sp.getViewport().setBackground(new Color(18, 20, 33));

        p.add(top, BorderLayout.NORTH);
        p.add(sp, BorderLayout.CENTER);

        loadBlockedPrompts();
        return p;
    }

    private void loadBlockedPrompts() {
        List<Prompt> prompts = dao.loadAllBlockedPrompts();
        String[] columns = {"ID", "User", "Original Text (first 50)", "Timestamp"};

        if (prompts.isEmpty()) {
            Object[][] emptyData = {{"-", "No blocked prompts", "-", "No blocked records yet"}};
            tblBlockedPrompts.setModel(new DefaultTableModel(emptyData, columns) {
                @Override public boolean isCellEditable(int row, int col) { return false; }
            });
            return;
        }

        Object[][] data = new Object[prompts.size()][4];

        for (int count = 0; count < prompts.size(); count++) {
            Prompt p = prompts.get(count);
            String text = p.getOriginalText();
            if (text != null && text.length() > 50) {
                text = text.substring(0, 50) + "…";
            }
            data[count] = new Object[]{
                    p.getId(),
                    p.getUserId(),
                    text,
                    p.getTimestamp()
            };
        }

        DefaultTableModel model = new DefaultTableModel(data, columns) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        tblBlockedPrompts.setModel(model);
    }

    // ── Policies Tab ──────────────────────────────────────────────────────────

    private JPanel buildPoliciesPanel() {
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setBackground(CLR_BG);
        p.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        top.setOpaque(false);

        JButton btnAdd = new JButton("+ Add Policy");
        btnAdd.addActionListener(e -> addPolicy());
        top.add(btnAdd);

        JButton btnEdit = new JButton("✏ Edit");
        btnEdit.addActionListener(e -> editPolicy());
        top.add(btnEdit);

        JButton btnDelete = new JButton("🗑 Delete");
        btnDelete.addActionListener(e -> deletePolicy());
        top.add(btnDelete);

        JButton btnRefresh = new JButton("Refresh");
        btnRefresh.addActionListener(e -> loadPolicies());
        top.add(btnRefresh);

        tblPolicies = new JTable();
        tblPolicies.setBackground(new Color(18, 20, 33));
        tblPolicies.setForeground(CLR_TEXT);
        tblPolicies.setSelectionBackground(CLR_ACCENT);
        tblPolicies.setRowHeight(30);
        JScrollPane sp = new JScrollPane(tblPolicies);
        sp.getViewport().setBackground(new Color(18, 20, 33));

        p.add(top, BorderLayout.NORTH);
        p.add(sp, BorderLayout.CENTER);

        loadPolicies();
        return p;
    }

    private void loadPolicies() {
        List<Policy> policies = dao.loadAllPolicies();
        String[] columns = {"ID", "Keyword", "Action", "Description"};
        Object[][] data = new Object[policies.size()][4];

        for (int i = 0; i < policies.size(); i++) {
            Policy policy = policies.get(i);
            data[i] = new Object[]{
                    policy.getId(),
                    policy.getKeyword(),
                    policy.getAction(),
                    policy.getDescription()
            };
        }

        DefaultTableModel model = new DefaultTableModel(data, columns) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        tblPolicies.setModel(model);
    }

    private void addPolicy() {
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        JTextField tfKeyword = new JTextField(20);
        JComboBox<String> cbAction = new JComboBox<>(new String[]{"BLOCK", "ALLOW", "MASK"});
        JTextField tfDescription = new JTextField(20);

        gbc.gridx = 0; gbc.gridy = 0; form.add(new JLabel("Keyword:"), gbc);
        gbc.gridx = 1; form.add(tfKeyword, gbc);

        gbc.gridx = 0; gbc.gridy = 1; form.add(new JLabel("Action:"), gbc);
        gbc.gridx = 1; form.add(cbAction, gbc);

        gbc.gridx = 0; gbc.gridy = 2; form.add(new JLabel("Description:"), gbc);
        gbc.gridx = 1; form.add(tfDescription, gbc);

        int result = JOptionPane.showConfirmDialog(this, form, "Add Policy", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            Policy p = new Policy();
            p.setKeyword(tfKeyword.getText().trim());
            p.setAction((String) cbAction.getSelectedItem());
            p.setDescription(tfDescription.getText().trim());

            int id = dao.savePolicy(p);
            if (id > 0) {
                JOptionPane.showMessageDialog(this, "Policy added successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                loadPolicies();
            } else {
                JOptionPane.showMessageDialog(this, "Failed to add policy", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void editPolicy() {
        int selectedRow = tblPolicies.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Select a policy to edit.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int policyId = (int) tblPolicies.getValueAt(selectedRow, 0);
        List<Policy> policies = dao.loadAllPolicies();
        Policy selected = policies.stream().filter(p -> p.getId() == policyId).findFirst().orElse(null);

        if (selected == null) return;

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        JTextField tfKeyword = new JTextField(selected.getKeyword(), 20);
        JComboBox<String> cbAction = new JComboBox<>(new String[]{"BLOCK", "ALLOW", "MASK"});
        cbAction.setSelectedItem(selected.getAction());
        JTextField tfDescription = new JTextField(selected.getDescription(), 20);

        gbc.gridx = 0; gbc.gridy = 0; form.add(new JLabel("Keyword:"), gbc);
        gbc.gridx = 1; form.add(tfKeyword, gbc);

        gbc.gridx = 0; gbc.gridy = 1; form.add(new JLabel("Action:"), gbc);
        gbc.gridx = 1; form.add(cbAction, gbc);

        gbc.gridx = 0; gbc.gridy = 2; form.add(new JLabel("Description:"), gbc);
        gbc.gridx = 1; form.add(tfDescription, gbc);

        int result = JOptionPane.showConfirmDialog(this, form, "Edit Policy", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            selected.setKeyword(tfKeyword.getText().trim());
            selected.setAction((String) cbAction.getSelectedItem());
            selected.setDescription(tfDescription.getText().trim());

            if (dao.updatePolicy(selected)) {
                JOptionPane.showMessageDialog(this, "Policy updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                loadPolicies();
            } else {
                JOptionPane.showMessageDialog(this, "Failed to update policy", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void deletePolicy() {
        int selectedRow = tblPolicies.getSelectedRow();
        if (selectedRow < 0) {
            JOptionPane.showMessageDialog(this, "Select a policy to delete.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int policyId = (int) tblPolicies.getValueAt(selectedRow, 0);
        int confirm = JOptionPane.showConfirmDialog(this, "Delete this policy?", "Confirm", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            if (dao.deletePolicy(policyId)) {
                JOptionPane.showMessageDialog(this, "Policy deleted successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                loadPolicies();
            } else {
                JOptionPane.showMessageDialog(this, "Failed to delete policy", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // ── Users Tab ─────────────────────────────────────────────────────────────

    private JPanel buildUsersPanel() {
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setBackground(CLR_BG);
        p.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        top.setOpaque(false);

        JButton btnRefresh = new JButton("Refresh");
        btnRefresh.addActionListener(e -> loadUsers());
        top.add(btnRefresh);

        JLabel lblInfo = new JLabel("System users and their roles");
        lblInfo.setForeground(CLR_ACCENT);
        top.add(lblInfo);

        tblUsers = new JTable();
        tblUsers.setBackground(new Color(18, 20, 33));
        tblUsers.setForeground(CLR_TEXT);
        tblUsers.setSelectionBackground(CLR_ACCENT);
        JScrollPane sp = new JScrollPane(tblUsers);
        sp.getViewport().setBackground(new Color(18, 20, 33));

        p.add(top, BorderLayout.NORTH);
        p.add(sp, BorderLayout.CENTER);

        loadUsers();
        return p;
    }

    private void loadUsers() {
        List<DataDAO.DbUser> users = dao.loadAllUsers();
        String[] columns = {"ID", "Username", "Role", "Created At"};
        Object[][] data = new Object[users.size()][4];

        for (int i = 0; i < users.size(); i++) {
            DataDAO.DbUser user = users.get(i);
            data[i] = new Object[]{
                    user.getId(),
                    user.getUsername(),
                    user.getRole(),
                    user.getCreatedAt()
            };
        }

        DefaultTableModel model = new DefaultTableModel(data, columns) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        tblUsers.setModel(model);
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private void refreshCurrentTab() {
        int selected = getSelectedIndex();
        String title = getTitleAt(selected);

        if ("Logs".equals(title)) loadLogs();
        else if ("Blocked Prompts".equals(title)) loadBlockedPrompts();
        else if ("Policies".equals(title)) loadPolicies();
        else if ("Users".equals(title)) loadUsers();
    }
}
