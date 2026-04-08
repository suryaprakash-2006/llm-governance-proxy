package com.llmgovernance.system.db;

import com.llmgovernance.system.util.AppLogger;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Logger;

/**
 * DBConnection – lightweight flat-file "database" stored as a CSV-like file
 * in the user's home directory.  No external JDBC driver required.
 *
 * Storage location: <user.home>/llm_governance/records.db  (tab-separated)
 * Format per line:
 *   ID\tTIMESTAMP\tORIGINAL_HASH\tDECOMPRESSED_HASH\tFILTERED_LEN\tCOMPRESSED_LEN
 *   (full text blobs stored in separate per-ID files to avoid delimiter clashes)
 */
public class DBConnection {

    private static final String BASE_DIR =
            System.getProperty("user.home") + File.separator + "llm_governance";
    private static final String INDEX_FILE = BASE_DIR + File.separator + "records.tsv";
    private static final String BLOBS_DIR  = BASE_DIR + File.separator + "blobs";

    private static DBConnection instance;
    private static final Logger LOG = AppLogger.getLogger(DBConnection.class);

    // ── Singleton ─────────────────────────────────────────────────────────────

    private DBConnection() {
        initDirectories();
    }

    public static synchronized DBConnection getInstance() {
        if (instance == null) instance = new DBConnection();
        return instance;
    }

    // ── Init ──────────────────────────────────────────────────────────────────

    private void initDirectories() {
        try {
            Files.createDirectories(Paths.get(BLOBS_DIR));
            if (!Files.exists(Paths.get(INDEX_FILE))) {
                // write header
                Files.write(Paths.get(INDEX_FILE),
                        "ID\tTIMESTAMP\tORIGINAL_HASH\tDECOMPRESSED_HASH\n".getBytes(),
                        StandardOpenOption.CREATE_NEW);
            }
        } catch (IOException e) {
            LOG.warning("Init error: " + e.getMessage());
        }
    }

    // ── Public helpers ────────────────────────────────────────────────────────

    public String getBaseDir()  { return BASE_DIR; }
    public String getIndexFile(){ return INDEX_FILE; }
    public String getBlobsDir() { return BLOBS_DIR; }

    public String now() {
        return LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    /**
     * Generates the next integer ID by counting existing non-header lines.
     */
    public synchronized int nextId() {
        try {
            List<String> lines = Files.readAllLines(Paths.get(INDEX_FILE));
            return lines.size(); // header is line 0, so records start at 1
        } catch (IOException e) {
            return (int)(System.currentTimeMillis() % 100000);
        }
    }
}
