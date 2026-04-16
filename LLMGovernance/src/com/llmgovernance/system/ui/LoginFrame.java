package com.llmgovernance.system.ui;

import com.llmgovernance.system.model.UserSession;
import com.llmgovernance.system.user.AuthService;
import com.llmgovernance.system.user.AuthService.LoginResult;
import com.llmgovernance.system.user.AuthService.LoginStatus;

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

        JButton btnRegister = new JButton("Register");
        btnRegister.addActionListener(e -> openRegistration());

        buttonRow.add(btnLogin);
        buttonRow.add(btnRegister);

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

        LoginResult result = authService.authenticateDetailed(username, password);
        if (result.getStatus() == LoginStatus.DB_ERROR) {
            lblError.setText(result.getMessage());
            JOptionPane.showMessageDialog(
                    this,
                    result.getMessage() + "\n\nSet LLM_DB_USER and LLM_DB_PASSWORD for your MySQL server.",
                    "Database Connection Error",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        if (result.getStatus() != LoginStatus.SUCCESS) {
            lblError.setText(result.getMessage());
            pfPassword.selectAll();
            pfPassword.requestFocusInWindow();
            return;
        }

        UserSession session = result.getSession();

        MainFrame frame = new MainFrame(session.getUsername(), session.getRole());
        frame.setVisible(true);
        dispose();
    }

    private void openRegistration() {
        RegistrationFrame regFrame = new RegistrationFrame();
        regFrame.setVisible(true);
    }
}
