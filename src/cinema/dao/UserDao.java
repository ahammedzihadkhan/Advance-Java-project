package cinema.dao;

import cinema.db.Database;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** Stores staff logins as salted PBKDF2 hashes, never as plain text. */
public final class UserDao {
    private static final int ITERATIONS = 600_000;

    public boolean hasUsers() throws SQLException {
        try (Connection c = Database.connect();
             Statement s = c.createStatement();
             ResultSet r = s.executeQuery("SELECT COUNT(*) FROM users")) {
            r.next();
            return r.getInt(1) > 0;
        }
    }

    public void create(String username, char[] password) throws SQLException {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        String stored = ITERATIONS + ":" + Base64.getEncoder().encodeToString(salt) + ":"
            + Base64.getEncoder().encodeToString(derive(password, salt, ITERATIONS));
        try (Connection c = Database.connect();
             PreparedStatement p = c.prepareStatement(
                 "INSERT INTO users (username, password_hash) VALUES (?, ?)")) {
            p.setString(1, username);
            p.setString(2, stored);
            p.executeUpdate();
        }
    }

    public boolean authenticate(String username, char[] password) throws SQLException {
        try (Connection c = Database.connect();
             PreparedStatement p = c.prepareStatement(
                 "SELECT password_hash FROM users WHERE username = ?")) {
            p.setString(1, username);
            try (ResultSet r = p.executeQuery()) {
                if (!r.next()) {
                    return false;
                }
                String[] parts = r.getString(1).split(":");
                byte[] salt = Base64.getDecoder().decode(parts[1]);
                byte[] expected = Base64.getDecoder().decode(parts[2]);
                return MessageDigest.isEqual(expected, derive(password, salt, Integer.parseInt(parts[0])));
            }
        }
    }

    private static byte[] derive(char[] password, byte[] salt, int iterations) {
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(new PBEKeySpec(password, salt, iterations, 256)).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
