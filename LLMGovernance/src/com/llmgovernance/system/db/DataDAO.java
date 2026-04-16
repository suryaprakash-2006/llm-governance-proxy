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
        private final String createdAt;

        public DbUser(int id, String username, String password, String role) {
            this(id, username, password, role, null);
        }

        public DbUser(int id, String username, String password, String role, String createdAt) {
            this.id = id;
            this.username = username;
            this.password = password;
            this.role = role;
            this.createdAt = createdAt;
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

        public String getCreatedAt() {
            return createdAt;
        }
    }

    public enum RegisterStatus {
        SUCCESS,
        USER_EXISTS,
        INVALID_INPUT,
        DB_ERROR
    }

    public static class RegisterResult {
        private final RegisterStatus status;
        private final int userId;
        private final String message;

        public RegisterResult(RegisterStatus status, int userId, String message) {
            this.status = status;
            this.userId = userId;
            this.message = message;
        }

        public RegisterStatus getStatus() {
            return status;
        }

        public int getUserId() {
            return userId;
        }

        public String getMessage() {
            return message;
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
            + "original_hash, decompressed_hash, status, created_at"
            + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, safe(prompt.getUserId(), "anonymous"));
            ps.setString(2, safe(prompt.getUserRole(), "USER"));
            ps.setString(3, safe(prompt.getOriginalText(), ""));
            ps.setString(4, safe(prompt.getFilteredText(), ""));
            ps.setString(5, safe(prompt.getCompressedText(), ""));
            ps.setString(6, safe(prompt.getOriginalHash(), ""));
            ps.setString(7, safe(prompt.getDecompressedHash(), ""));
            ps.setString(8, safe(prompt.getStatus(), "ALLOWED"));
            ps.setString(9, safe(prompt.getTimestamp(), db.now()));
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
            + "original_hash, decompressed_hash, status, created_at "
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
                p.setStatus(rs.getString("status"));
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

    public List<DbUser> loadAllUsers() {
        List<DbUser> users = new ArrayList<>();
        String sql = "SELECT id, username, password, role, created_at FROM users ORDER BY id ASC";

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                users.add(new DbUser(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("password"),
                        rs.getString("role"),
                        rs.getString("created_at")
                ));
            }
        } catch (SQLException e) {
            LOG.warning("Load users error: " + e.getMessage());
        }

        return users;
    }

    public DbUser getUserByUsernameOrThrow(String username) throws SQLException {
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
        }

        return null;
    }

    public DbUser getUserByUsername(String username) {
        try {
            return getUserByUsernameOrThrow(username);
        } catch (SQLException e) {
            LOG.warning("User lookup error: " + e.getMessage());
        }

        return null;
    }

    /**
     * Registers a new user with the given username, password, and role.
     * @return the new user ID, or -1 on failure.
     */
    public int registerUser(String username, String password, String role) {
        RegisterResult result = registerUserDetailed(username, password, role);
        return result.getStatus() == RegisterStatus.SUCCESS ? result.getUserId() : -1;
    }

    public RegisterResult registerUserDetailed(String username, String password, String role) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            return new RegisterResult(RegisterStatus.INVALID_INPUT, -1, "Username and password are required.");
        }

        String normalizedUser = username.trim();
        String normalizedRole = role == null ? "USER" : role.trim().toUpperCase();
        if (!"ADMIN".equals(normalizedRole) && !"USER".equals(normalizedRole)) {
            normalizedRole = "USER";
        }

        final String existsSql = "SELECT 1 FROM users WHERE lower(username)=lower(?) LIMIT 1";
        final String insertSql = "INSERT INTO users(username, password, role, created_at) VALUES (?, ?, ?, ?)";

        try (Connection conn = db.getConnection()) {
            try (PreparedStatement existsPs = conn.prepareStatement(existsSql)) {
                existsPs.setString(1, normalizedUser);
                try (ResultSet rs = existsPs.executeQuery()) {
                    if (rs.next()) {
                        return new RegisterResult(RegisterStatus.USER_EXISTS, -1, "Username already taken");
                    }
                }
            }

            try (PreparedStatement insertPs = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                insertPs.setString(1, normalizedUser);
                insertPs.setString(2, password);
                insertPs.setString(3, normalizedRole);
                insertPs.setString(4, db.now());
                insertPs.executeUpdate();

                try (ResultSet keys = insertPs.getGeneratedKeys()) {
                    if (keys.next()) {
                        int id = keys.getInt(1);
                        LOG.info("Registered new user: " + normalizedUser + " with ID=" + id);
                        return new RegisterResult(RegisterStatus.SUCCESS, id, "Registration successful");
                    }
                }
            }

            return new RegisterResult(RegisterStatus.DB_ERROR, -1, "Registration failed. No generated key returned.");
        } catch (SQLException e) {
            String message = e.getMessage() == null ? "Database error" : e.getMessage();
            LOG.warning("Register user error: " + message);
            if (message.toLowerCase().contains("access denied")) {
                message = "Database access denied. Please verify MySQL username/password.";
            }
            return new RegisterResult(RegisterStatus.DB_ERROR, -1, message);
        }
    }

    private String safe(String value, String fallback) {
        return value == null ? fallback : value;
    }

    // ── Policy Management ──────────────────────────────────────────────────────

    /**
     * Saves a new policy rule to the policies table.
     */
    public int savePolicy(com.llmgovernance.system.model.Policy policy) {
        String sql = "INSERT INTO policies(keyword, action, description, created_at) VALUES (?, ?, ?, ?)";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, safe(policy.getKeyword(), ""));
            ps.setString(2, safe(policy.getAction(), "BLOCK"));
            ps.setString(3, safe(policy.getDescription(), ""));
            ps.setString(4, safe(policy.getCreatedAt(), db.now()));
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    policy.setId(id);
                    LOG.info("Saved policy ID=" + id);
                    return id;
                }
            }
            return -1;
        } catch (SQLException e) {
            LOG.warning("Save policy error: " + e.getMessage());
            return -1;
        }
    }

    /**
     * Loads all policy rules.
     */
    public List<com.llmgovernance.system.model.Policy> loadAllPolicies() {
        List<com.llmgovernance.system.model.Policy> list = new ArrayList<>();
        String sql = "SELECT id, keyword, action, description, created_at FROM policies ORDER BY id ASC";

        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                com.llmgovernance.system.model.Policy p = new com.llmgovernance.system.model.Policy();
                p.setId(rs.getInt("id"));
                p.setKeyword(rs.getString("keyword"));
                p.setAction(rs.getString("action"));
                p.setDescription(rs.getString("description"));
                p.setCreatedAt(rs.getString("created_at"));
                list.add(p);
            }
        } catch (SQLException e) {
            LOG.warning("Load policies error: " + e.getMessage());
        }
        return list;
    }

    /**
     * Updates an existing policy rule.
     */
    public boolean updatePolicy(com.llmgovernance.system.model.Policy policy) {
        String sql = "UPDATE policies SET keyword=?, action=?, description=? WHERE id=?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, safe(policy.getKeyword(), ""));
            ps.setString(2, safe(policy.getAction(), "BLOCK"));
            ps.setString(3, safe(policy.getDescription(), ""));
            ps.setInt(4, policy.getId());
            int rowsAffected = ps.executeUpdate();
            LOG.info("Updated policy ID=" + policy.getId() + " (" + rowsAffected + " rows)");
            return rowsAffected > 0;
        } catch (SQLException e) {
            LOG.warning("Update policy error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Deletes a policy rule by ID.
     */
    public boolean deletePolicy(int policyId) {
        String sql = "DELETE FROM policies WHERE id=?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, policyId);
            int rowsAffected = ps.executeUpdate();
            LOG.info("Deleted policy ID=" + policyId + " (" + rowsAffected + " rows)");
            return rowsAffected > 0;
        } catch (SQLException e) {
            LOG.warning("Delete policy error: " + e.getMessage());
            return false;
        }
    }

    /**
     * Gets policy count for admin dashboard.
     */
    public int getPolicyCount() {
        String sql = "SELECT COUNT(1) FROM policies";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            LOG.warning("Count policies error: " + e.getMessage());
        }
        return 0;
    }
}

