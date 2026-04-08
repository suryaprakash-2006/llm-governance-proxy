package com.llmgovernance.system.db;

import com.llmgovernance.system.model.Prompt;
import com.llmgovernance.system.util.AppLogger;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.logging.Logger;

/**
 * DataDAO – Data Access Object.
 * Persists and retrieves Prompt records using the flat-file store
 * managed by DBConnection.
 *
 * Index file  : records.tsv  →  ID | TIMESTAMP | ORIG_HASH | DECOMP_HASH
 * Blob files  : blobs/<ID>_original.txt
 *               blobs/<ID>_filtered.txt
 *               blobs/<ID>_compressed.txt
 */
public class DataDAO {

    private final DBConnection db = DBConnection.getInstance();
    private static final Logger LOG = AppLogger.getLogger(DataDAO.class);

    // ── Save ──────────────────────────────────────────────────────────────────

    /**
     * Saves a Prompt record to the flat-file store.
     * @return the assigned record ID, or -1 on failure.
     */
    public int savePrompt(Prompt prompt) {
        try {
            int id = db.nextId();
            prompt.setId(id);

            // Write blob files
            writeBlob(id, "original",    prompt.getOriginalText());
            writeBlob(id, "filtered",    prompt.getFilteredText());
            writeBlob(id, "compressed",  prompt.getCompressedText());

            // Append index line
            String line = id
                    + "\t" + prompt.getTimestamp()
                    + "\t" + prompt.getOriginalHash()
                    + "\t" + prompt.getDecompressedHash()
                    + "\n";
            Files.write(Paths.get(db.getIndexFile()),
                    line.getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.APPEND);

            LOG.info("Saved record ID=" + id);
            return id;

        } catch (IOException e) {
            LOG.warning("Save error: " + e.getMessage());
            return -1;
        }
    }

    // ── Load All ──────────────────────────────────────────────────────────────

    /**
     * Loads all stored Prompt records (summary only – blobs are separate).
     */
    public List<Prompt> loadAll() {
        List<Prompt> list = new ArrayList<>();
        try {
            List<String> lines = Files.readAllLines(Paths.get(db.getIndexFile()));
            // skip header (line 0)
            for (int i = 1; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                if (line.isEmpty()) continue;
                String[] parts = line.split("\t", -1);
                if (parts.length < 4) continue;

                Prompt p = new Prompt();
                p.setId(Integer.parseInt(parts[0].trim()));
                p.setTimestamp(parts[1].trim());
                p.setOriginalHash(parts[2].trim());
                p.setDecompressedHash(parts[3].trim());

                // load blobs
                p.setOriginalText(readBlob(p.getId(), "original"));
                p.setFilteredText(readBlob(p.getId(), "filtered"));
                p.setCompressedText(readBlob(p.getId(), "compressed"));

                list.add(p);
            }
        } catch (IOException e) {
            LOG.warning("Load error: " + e.getMessage());
        }
        return list;
    }

    // ── Load by ID ────────────────────────────────────────────────────────────

    public Prompt loadById(int id) {
        List<Prompt> all = loadAll();
        for (Prompt p : all) {
            if (p.getId() == id) return p;
        }
        return null;
    }

    // ── Count ─────────────────────────────────────────────────────────────────

    public int countRecords() {
        return loadAll().size();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void writeBlob(int id, String type, String content) throws IOException {
        String path = db.getBlobsDir() + File.separator + id + "_" + type + ".txt";
        Files.write(Paths.get(path),
                (content == null ? "" : content).getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    private String readBlob(int id, String type) {
        String path = db.getBlobsDir() + File.separator + id + "_" + type + ".txt";
        try {
            return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "";
        }
    }
}
