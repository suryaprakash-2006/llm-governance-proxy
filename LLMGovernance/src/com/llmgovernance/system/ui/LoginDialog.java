package com.llmgovernance.system.ui;

import com.llmgovernance.system.model.UserSession;
import com.llmgovernance.system.user.AuthService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * LoginDialog validates username/password from SQL users table.
 */
public class LoginDialog extends JDialog {

    private final JTextField tfUsername = new JTextField(18);
    private final JPasswordField pfPassword = new JPasswordField(18);
    private final JLabel lblStatus = new JLabel("Enter credentials to continue");

    private final AuthService authService = new AuthService();
    private UserSession authenticatedUser;

    public LoginDialog(Frame owner) {
        super(owner, "Login - LLM Governance", true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);
        setContentPane(buildContent());
        pack();
        setLocationRelativeTo(owner);
    }

    private JPanel buildContent() {
        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setBorder(new EmptyBorder(14, 14, 10, 14));

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0;
        gbc.gridy = 0;
        form.add(new JLabel("Username:"), gbc);

        gbc.gridx = 1;
        form.add(tfUsername, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        form.add(new JLabel("Password:"), gbc);

        gbc.gridx = 1;
        form.add(pfPassword, gbc);

        JLabel hint = new JLabel("Default users: admin/admin123, user/user123");
        hint.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        hint.setForeground(new Color(80, 80, 80));

        JPanel south = new JPanel(new BorderLayout(0, 8));
        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton btnLogin = new JButton("Login");
        JButton btnCancel = new JButton("Cancel");

        btnLogin.addActionListener(e -> tryLogin());
        btnCancel.addActionListener(e -> {
            authenticatedUser = null;
            dispose();
        });

        buttonRow.add(btnCancel);
        buttonRow.add(btnLogin);

        lblStatus.setForeground(new Color(60, 60, 60));
        south.add(lblStatus, BorderLayout.NORTH);
        south.add(hint, BorderLayout.CENTER);
        south.add(buttonRow, BorderLayout.SOUTH);

        root.add(form, BorderLayout.NORTH);
        root.add(south, BorderLayout.SOUTH);

        getRootPane().setDefaultButton(btnLogin);
        return root;
    }

    private void tryLogin() {
        String username = tfUsername.getText();
        String password = new String(pfPassword.getPassword());

        UserSession session = authService.authenticate(username, password);
        if (session == null) {
            lblStatus.setText("Invalid credentials. Try again.");
            lblStatus.setForeground(new Color(180, 25, 25));
            pfPassword.selectAll();
            pfPassword.requestFocusInWindow();
            return;
        }

        authenticatedUser = session;
        dispose();
    }

    public UserSession getAuthenticatedUser() {
        return authenticatedUser;
    }
}
