package com.llmgovernance.system.main;

import com.llmgovernance.system.ui.MainFrame;
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

            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
}
