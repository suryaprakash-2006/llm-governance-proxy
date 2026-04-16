package com.llmgovernance.system.user;

import com.llmgovernance.system.db.DataDAO;
import com.llmgovernance.system.model.UserSession;
import com.llmgovernance.system.util.HashUtil;

import java.sql.SQLException;

/**
 * AuthService authenticates users against the users SQL table.
 */
public class AuthService {

    private final DataDAO dao = new DataDAO();

    public enum LoginStatus {
        SUCCESS,
        INVALID_CREDENTIALS,
        DB_ERROR
    }

    public static class LoginResult {
        private final LoginStatus status;
        private final UserSession session;
        private final String message;

        public LoginResult(LoginStatus status, UserSession session, String message) {
            this.status = status;
            this.session = session;
            this.message = message;
        }

        public LoginStatus getStatus() {
            return status;
        }

        public UserSession getSession() {
            return session;
        }

        public String getMessage() {
            return message;
        }
    }

    public UserSession authenticate(String username, String password) {
        LoginResult result = authenticateDetailed(username, password);
        return result.getStatus() == LoginStatus.SUCCESS ? result.getSession() : null;
    }

    public LoginResult authenticateDetailed(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            return new LoginResult(LoginStatus.INVALID_CREDENTIALS, null, "Username and password are required.");
        }

        DataDAO.DbUser dbUser;
        try {
            dbUser = dao.getUserByUsernameOrThrow(username.trim());
        } catch (SQLException ex) {
            if (isDemoModeEnabled()) {
                UserSession demoSession = tryDemoLogin(username, password);
                if (demoSession != null) {
                    return new LoginResult(
                            LoginStatus.SUCCESS,
                            demoSession,
                            "Demo login successful (database unavailable)."
                    );
                }
            }
            return new LoginResult(
                    LoginStatus.DB_ERROR,
                    null,
                    "Database connection failed. Please check MySQL credentials and server status."
            );
        }

        if (dbUser == null) {
            return new LoginResult(LoginStatus.INVALID_CREDENTIALS, null, "Invalid credentials");
        }

        String storedPassword = dbUser.getPassword();
        if (storedPassword == null || storedPassword.isBlank()) {
            return new LoginResult(LoginStatus.INVALID_CREDENTIALS, null, "Invalid credentials");
        }

        // Backward compatibility:
        // - new DBs store plaintext password (per current requirement)
        // - older DBs may still store SHA-256 values
        boolean plainMatch = password.equals(storedPassword);
        boolean hashMatch = HashUtil.sha256(password).equalsIgnoreCase(storedPassword);
        if (!plainMatch && !hashMatch) {
            return new LoginResult(LoginStatus.INVALID_CREDENTIALS, null, "Invalid credentials");
        }

        return new LoginResult(
                LoginStatus.SUCCESS,
                new UserSession(dbUser.getUsername(), dbUser.getRole()),
                "Login successful"
        );
    }

    private boolean isDemoModeEnabled() {
        String env = System.getenv("LLM_DEMO_MODE");
        String prop = System.getProperty("llm.demo.mode");
        return "true".equalsIgnoreCase(env) || "true".equalsIgnoreCase(prop);
    }

    private UserSession tryDemoLogin(String username, String password) {
        String u = username == null ? "" : username.trim();
        if ("admin".equalsIgnoreCase(u) && "admin123".equals(password)) {
            return new UserSession("admin", "ADMIN");
        }
        if ("user".equalsIgnoreCase(u) && "user123".equals(password)) {
            return new UserSession("user", "USER");
        }
        return null;
    }
}
