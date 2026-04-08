package com.llmgovernance.system.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.*;

/**
 * AppLogger - centralized logger for file + console logging.
 */
public class AppLogger {

    private static volatile boolean initialized = false;

    public static Logger getLogger(Class<?> clazz) {
        initializeOnce();
        return Logger.getLogger(clazz.getName());
    }

    private static synchronized void initializeOnce() {
        if (initialized) return;

        try {
            String baseDir = System.getProperty("user.home") + File.separator + "llm_governance";
            Path logDir = Path.of(baseDir, "logs");
            Files.createDirectories(logDir);

            Logger root = Logger.getLogger("");
            root.setLevel(Level.INFO);

            // Keep existing handlers and add file handler once for compatibility.
            FileHandler fh = new FileHandler(logDir.resolve("app.log").toString(), 1_000_000, 3, true);
            fh.setFormatter(new SimpleFormatter());
            fh.setLevel(Level.INFO);
            root.addHandler(fh);

            initialized = true;

        } catch (IOException e) {
            System.err.println("[AppLogger] Failed to init file logger: " + e.getMessage());
            initialized = true;
        }
    }
}
