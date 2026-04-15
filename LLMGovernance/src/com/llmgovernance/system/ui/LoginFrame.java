package com.llmgovernance.system.ui;

import com.llmgovernance.system.model.UserSession;
import com.llmgovernance.system.user.AuthService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * LoginFrame provides DB-backed authentication before opening MainFrame.
 */
public class LoginFrame extends JFrame {

    private final JTextField tfUsername = new JTextField(18);
    private final JPasswordField pfPassword = new JPasswordField(18);
    private final JLabel lblError = new JLabel(" ");

    private final AuthService authService = new AuthService();

    public LoginFrame() {
        super("Login - LLM Governance");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        setContentPane(buildContent());
        pack();
        setLocationRelativeTo(null);
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
        btnLogin.addActionListener(e -> tryLogin());

        buttonRow.add(btnLogin);

        lblError.setForeground(new Color(180, 25, 25));
        south.add(lblError, BorderLayout.NORTH);
        south.add(hint, BorderLayout.CENTER);
        south.add(buttonRow, BorderLayout.SOUTH);

        root.add(form, BorderLayout.NORTH);
        root.add(south, BorderLayout.SOUTH);

        getRootPane().setDefaultButton(btnLogin);
        return root;
    }

    private void tryLogin() {
        String username = tfUsername.getText() == null ? "" : tfUsername.getText().trim();
        String password = new String(pfPassword.getPassword());

        UserSession session = authService.authenticate(username, password);
        if (session == null) {
            lblError.setText("Invalid credentials");
            pfPassword.selectAll();
            pfPassword.requestFocusInWindow();
            return;
        }

        MainFrame frame = new MainFrame(session.getUsername(), session.getRole());
        frame.setVisible(true);
        dispose();
    }
}
