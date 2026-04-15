package com.llmgovernance.system.main;

import com.llmgovernance.system.db.DBConnection;
import com.llmgovernance.system.ui.LoginFrame;
import com.llmgovernance.system.util.AppLogger;

import javax.swing.*;
import java.util.logging.Logger;

/**
 * MainApp – application entry point.
 *
 * Launches the LLM Data Leak Prevention System Swing UI on the
 * Event Dispatch Thread (EDT) as required by Swing threading rules.
 */
public class MainApp {

    private static final Logger LOG = AppLogger.getLogger(MainApp.class);

    public static void main(String[] args) {
        // Ensure Swing components are created on the EDT
        SwingUtilities.invokeLater(() -> {
            LOG.info("=================================================");
            LOG.info("  LLM Data Leak Prevention System");
            LOG.info("  Package: com.llmgovernance.system");
            LOG.info("  Storage : " + System.getProperty("user.home") + "/llm_governance/");
            LOG.info("=================================================");

            // Ensure DB schema and default users are ready before login.
            DBConnection.getInstance();

            LoginFrame loginFrame = new LoginFrame();
            loginFrame.setVisible(true);
        });
    }
}
