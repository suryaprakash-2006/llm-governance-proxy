package com.llmgovernance.system.db;

import com.llmgovernance.system.model.Prompt;
import com.llmgovernance.system.util.AppLogger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * DataDAO – Data Access Object.
 * Persists and retrieves Prompt records using MySQL tables.
 */
public class DataDAO {

    private final DBConnection db = DBConnection.getInstance();
    private static final Logger LOG = AppLogger.getLogger(DataDAO.class);

    public static class DbUser {
        private final int id;
        private final String username;
        private final String password;
        private final String role;

        public DbUser(int id, String username, String password, String role) {
            this.id = id;
            this.username = username;
            this.password = password;
            this.role = role;
        }

        public int getId() {
            return id;
        }

        public String getUsername() {
            return username;
        }

        public String getPassword() {
            return password;
        }

        public String getRole() {
            return role;
        }
    }

    // ── Save ──────────────────────────────────────────────────────────────────

    /**
    * Saves a Prompt record to the database.
     * @return the assigned record ID, or -1 on failure.
     */
    public int savePrompt(Prompt prompt) {
        String sql = "INSERT INTO prompts("
                + "user_id, user_role, original_text, filtered_text, compressed_text, "
                + "original_hash, decompressed_hash, created_at"
                + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, safe(prompt.getUserId(), "anonymous"));
            ps.setString(2, safe(prompt.getUserRole(), "USER"));
            ps.setString(3, safe(prompt.getOriginalText(), ""));
            ps.setString(4, safe(prompt.getFilteredText(), ""));
            ps.setString(5, safe(prompt.getCompressedText(), ""));
            ps.setString(6, safe(prompt.getOriginalHash(), ""));
            ps.setString(7, safe(prompt.getDecompressedHash(), ""));
            ps.setString(8, safe(prompt.getTimestamp(), db.now()));
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    prompt.setId(id);
                    LOG.info("Saved record ID=" + id);
                    return id;
                }
            }
            return -1;

        } catch (SQLException e) {
            LOG.warning("Save error: " + e.getMessage());
            return -1;
        }
    }

    // ── Load All ──────────────────────────────────────────────────────────────

    /**
     * Loads all stored Prompt records.
     */
    public List<Prompt> loadAll() {
        List<Prompt> list = new ArrayList<>();
        String sql = "SELECT id, user_id, user_role, original_text, filtered_text, compressed_text, "
                + "original_hash, decompressed_hash, created_at "
                + "FROM prompts ORDER BY id DESC";

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Prompt p = new Prompt();
                p.setId(rs.getInt("id"));
                p.setUserId(rs.getString("user_id"));
                p.setUserRole(rs.getString("user_role"));
                p.setOriginalText(rs.getString("original_text"));
                p.setFilteredText(rs.getString("filtered_text"));
                p.setCompressedText(rs.getString("compressed_text"));
                p.setOriginalHash(rs.getString("original_hash"));
                p.setDecompressedHash(rs.getString("decompressed_hash"));
                p.setTimestamp(rs.getString("created_at"));
                list.add(p);
            }
        } catch (SQLException e) {
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
        String sql = "SELECT COUNT(1) FROM prompts";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            LOG.warning("Count error: " + e.getMessage());
        }
        return 0;
    }

    // ── User Auth Lookup ─────────────────────────────────────────────────────

    public DbUser getUserByUsername(String username) {
        if (username == null || username.isBlank()) {
            return null;
        }

        String sql = "SELECT id, username, password, role FROM users WHERE lower(username)=lower(?) LIMIT 1";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new DbUser(
                            rs.getInt("id"),
                            rs.getString("username"),
                            rs.getString("password"),
                            rs.getString("role")
                    );
                }
            }
        } catch (SQLException e) {
            LOG.warning("User lookup error: " + e.getMessage());
        }

        return null;
    }

    private String safe(String value, String fallback) {
        return value == null ? fallback : value;
    }
}
