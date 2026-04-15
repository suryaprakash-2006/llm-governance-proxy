package com.llmgovernance.system.db;

import com.llmgovernance.system.util.AppLogger;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Logger;

/**
 * DBConnection - MySQL connection manager.
 *
 * Connection settings are read from environment variables or system properties:
 *   LLM_DB_HOST / llm.db.host         default: localhost
 *   LLM_DB_PORT / llm.db.port         default: 3306
 *   LLM_DB_NAME / llm.db.name         default: llm_governance
 *   LLM_DB_USER / llm.db.user         default: root
 *   LLM_DB_PASSWORD / llm.db.password default: empty
 *
 * Tables created on startup:
 *   - users
 *   - prompts
 *   - logs
 */
public class DBConnection {

    private static final String DEFAULT_HOST = "localhost";
    private static final String DEFAULT_PORT = "3306";
    private static final String DEFAULT_DB_NAME = "llm_governance";
    private static final String DEFAULT_USER = "root";
    private static final String DEFAULT_PASSWORD = "";

    private static final String DB_HOST = resolveSetting("LLM_DB_HOST", "llm.db.host", DEFAULT_HOST);
    private static final String DB_PORT = resolveSetting("LLM_DB_PORT", "llm.db.port", DEFAULT_PORT);
    private static final String DB_NAME = resolveSetting("LLM_DB_NAME", "llm.db.name", DEFAULT_DB_NAME);
    private static final String DB_USER = resolveSetting("LLM_DB_USER", "llm.db.user", DEFAULT_USER);
    private static final String DB_PASSWORD = resolveSetting("LLM_DB_PASSWORD", "llm.db.password", DEFAULT_PASSWORD);
    private static final String JDBC_URL = "jdbc:mysql://" + DB_HOST + ":" + DB_PORT + "/" + DB_NAME
        + "?useUnicode=true&characterEncoding=utf8&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
        + "&createDatabaseIfNotExist=true";

    private static DBConnection instance;
    private static final Logger LOG = AppLogger.getLogger(DBConnection.class);
    private volatile boolean mysqlDriverLoaded;

    // ── Singleton ─────────────────────────────────────────────────────────────

    private DBConnection() {
        initSchema();
    }

    public static synchronized DBConnection getInstance() {
        if (instance == null) instance = new DBConnection();
        return instance;
    }

    private void initSchema() {
        final String createUsers =
                "CREATE TABLE IF NOT EXISTS users ("
                        + "id BIGINT PRIMARY KEY AUTO_INCREMENT,"
                        + "username VARCHAR(50) NOT NULL UNIQUE,"
                        + "password VARCHAR(255) NOT NULL,"
                        + "role ENUM('ADMIN','USER') NOT NULL,"
                        + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
                        + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";

        final String createPrompts =
                "CREATE TABLE IF NOT EXISTS prompts ("
                        + "id BIGINT PRIMARY KEY AUTO_INCREMENT,"
                        + "user_id VARCHAR(50) NOT NULL,"
                        + "user_role VARCHAR(20) NOT NULL,"
                        + "original_text TEXT,"
                        + "filtered_text TEXT,"
                        + "compressed_text TEXT,"
                        + "original_hash TEXT,"
                        + "decompressed_hash TEXT,"
                        + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
                        + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";

        final String createLogs =
                "CREATE TABLE IF NOT EXISTS logs ("
                        + "id BIGINT PRIMARY KEY AUTO_INCREMENT,"
                        + "user_id BIGINT,"
                        + "prompt TEXT,"
                        + "response TEXT,"
                        + "status TEXT,"
                        + "timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
                        + "FOREIGN KEY(user_id) REFERENCES users(id)"
                        + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";

        try (Connection conn = getConnection();
             Statement st = conn.createStatement()) {
            st.executeUpdate(createUsers);
            st.executeUpdate(createPrompts);
            st.executeUpdate(createLogs);
            seedDefaultUsers(conn);
        } catch (SQLException e) {
            LOG.warning("Schema init error: " + e.getMessage());
        }
    }

    private void seedDefaultUsers(Connection conn) throws SQLException {
        ensureDefaultUser(conn, "admin", "admin123", "ADMIN");
        ensureDefaultUser(conn, "user", "user123", "USER");
    }

    private void ensureDefaultUser(Connection conn, String username, String password, String role) throws SQLException {
        String existsSql = "SELECT 1 FROM users WHERE lower(username)=lower(?) LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(existsSql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return;
                }
            }
        }

        String insertSql = "INSERT INTO users(username, password, role, created_at) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            ps.setString(3, role);
            ps.setString(4, now());
            ps.executeUpdate();
        }
        LOG.info("Seeded default user: " + username + "/" + password);
    }

    // ── Public helpers ────────────────────────────────────────────────────────

    public String getDatabaseFile() { return DB_NAME; }
    public String getBaseDir()  { return DB_HOST + ":" + DB_PORT; }
    public String getJdbcUrl() { return JDBC_URL; }

    public Connection getConnection() throws SQLException {
        ensureMySqlDriver();
        return DriverManager.getConnection(JDBC_URL, DB_USER, DB_PASSWORD);
    }

    private synchronized void ensureMySqlDriver() throws SQLException {
        if (mysqlDriverLoaded) {
            return;
        }
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            mysqlDriverLoaded = true;
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL JDBC driver not found in runtime classpath.", e);
        }
    }

    private static String resolveSetting(String envKey, String propertyKey, String defaultValue) {
        String value = System.getenv(envKey);
        if (value == null || value.isBlank()) {
            value = System.getProperty(propertyKey);
        }
        return (value == null || value.isBlank()) ? defaultValue : value.trim();
    }

    public String now() {
        return LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}
