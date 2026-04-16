package com.llmgovernance.system.ui;

import com.llmgovernance.system.db.DataDAO;
import com.llmgovernance.system.db.DataDAO.RegisterResult;
import com.llmgovernance.system.db.DataDAO.RegisterStatus;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * RegistrationFrame allows new users to self-register.
 */
public class RegistrationFrame extends JFrame {

    private final JTextField tfUsername = new JTextField(18);
    private final JPasswordField pfPassword = new JPasswordField(18);
    private final JPasswordField pfConfirm = new JPasswordField(18);
    private final JLabel lblError = new JLabel(" ");

    private final DataDAO dao = new DataDAO();

    public RegistrationFrame() {
        super("Register - LLM Governance");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
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

        gbc.gridx = 0;
        gbc.gridy = 2;
        form.add(new JLabel("Confirm:"), gbc);

        gbc.gridx = 1;
        form.add(pfConfirm, gbc);

        JLabel hint = new JLabel("New users register as common USER role.");
        hint.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        hint.setForeground(new Color(80, 80, 80));

        JPanel south = new JPanel(new BorderLayout(0, 8));
        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));

        JButton btnRegister = new JButton("Register");
        btnRegister.addActionListener(e -> tryRegister());

        JButton btnCancel = new JButton("Cancel");
        btnCancel.addActionListener(e -> dispose());

        buttonRow.add(btnRegister);
        buttonRow.add(btnCancel);

        lblError.setForeground(new Color(180, 25, 25));
        south.add(lblError, BorderLayout.NORTH);
        south.add(hint, BorderLayout.CENTER);
        south.add(buttonRow, BorderLayout.SOUTH);

        root.add(form, BorderLayout.NORTH);
        root.add(south, BorderLayout.SOUTH);

        getRootPane().setDefaultButton(btnRegister);
        return root;
    }

    private void tryRegister() {
        String username = tfUsername.getText() == null ? "" : tfUsername.getText().trim();
        String password = new String(pfPassword.getPassword());
        String confirm = new String(pfConfirm.getPassword());

        if (username.isEmpty()) {
            lblError.setText("Username cannot be empty");
            tfUsername.requestFocusInWindow();
            return;
        }

        if (username.length() < 3) {
            lblError.setText("Username must be at least 3 characters");
            tfUsername.selectAll();
            tfUsername.requestFocusInWindow();
            return;
        }

        if (password.isEmpty()) {
            lblError.setText("Password cannot be empty");
            pfPassword.requestFocusInWindow();
            return;
        }

        if (password.length() < 6) {
            lblError.setText("Password must be at least 6 characters");
            pfPassword.selectAll();
            pfPassword.requestFocusInWindow();
            return;
        }

        if (!password.equals(confirm)) {
            lblError.setText("Passwords do not match");
            pfConfirm.selectAll();
            pfConfirm.requestFocusInWindow();
            return;
        }

        RegisterResult result = dao.registerUserDetailed(username, password, "USER");
        if (result.getStatus() == RegisterStatus.SUCCESS) {
            JOptionPane.showMessageDialog(this,
                    "Registration successful!\nYou can now login with your credentials.",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE);
            dispose();
            return;
        }

        if (result.getStatus() == RegisterStatus.USER_EXISTS) {
            lblError.setText("Username already taken");
            tfUsername.selectAll();
            tfUsername.requestFocusInWindow();
            return;
        }

        if (result.getStatus() == RegisterStatus.DB_ERROR) {
            lblError.setText("Database connection failed.");
            JOptionPane.showMessageDialog(
                    this,
                    "Registration failed due to database error:\n"
                            + result.getMessage()
                            + "\n\nSet LLM_DB_USER and LLM_DB_PASSWORD for your MySQL server.",
                    "Database Connection Error",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        lblError.setText(result.getMessage());
    }
}
