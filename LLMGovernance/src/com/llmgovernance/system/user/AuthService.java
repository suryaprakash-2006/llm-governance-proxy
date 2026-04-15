package com.llmgovernance.system.user;

import com.llmgovernance.system.db.DataDAO;
import com.llmgovernance.system.model.UserSession;
import com.llmgovernance.system.util.HashUtil;

/**
 * AuthService authenticates users against the users SQL table.
 */
public class AuthService {

    private final DataDAO dao = new DataDAO();

    public UserSession authenticate(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            return null;
        }

        DataDAO.DbUser dbUser = dao.getUserByUsername(username.trim());
        if (dbUser == null) {
            return null;
        }

        String storedPassword = dbUser.getPassword();
        if (storedPassword == null || storedPassword.isBlank()) {
            return null;
        }

        // Backward compatibility:
        // - new DBs store plaintext password (per current requirement)
        // - older DBs may still store SHA-256 values
        boolean plainMatch = password.equals(storedPassword);
        boolean hashMatch = HashUtil.sha256(password).equalsIgnoreCase(storedPassword);
        if (!plainMatch && !hashMatch) {
            return null;
        }

        return new UserSession(dbUser.getUsername(), dbUser.getRole());
    }
}
