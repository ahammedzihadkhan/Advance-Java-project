package cinema.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;

/** Opens JDBC connections and creates the schema. */
public final class Database {
    // DB_CLOSE_DELAY=-1 keeps the embedded engine open between DAO calls instead of reopening the file.
    private static String url = "jdbc:h2:./data/cinema;DB_CLOSE_DELAY=-1";

    private static final String[] SCHEMA = {
        "CREATE TABLE IF NOT EXISTS users ("
            + "id INT AUTO_INCREMENT PRIMARY KEY,"
            + "username VARCHAR(50) NOT NULL UNIQUE,"
            + "password_hash VARCHAR(200) NOT NULL)",
        "CREATE TABLE IF NOT EXISTS movies ("
            + "id INT AUTO_INCREMENT PRIMARY KEY,"
            + "title VARCHAR(120) NOT NULL,"
            + "genre VARCHAR(40) NOT NULL,"
            + "duration_min INT NOT NULL CHECK (duration_min BETWEEN 30 AND 300),"
            + "rating VARCHAR(8) NOT NULL)",
        "CREATE TABLE IF NOT EXISTS shows ("
            + "id INT AUTO_INCREMENT PRIMARY KEY,"
            + "movie_id INT NOT NULL REFERENCES movies(id),"
            + "starts_at TIMESTAMP NOT NULL,"
            + "price INT NOT NULL CHECK (price > 0))",
        "CREATE TABLE IF NOT EXISTS bookings ("
            + "id INT AUTO_INCREMENT PRIMARY KEY,"
            + "show_id INT NOT NULL REFERENCES shows(id),"
            + "customer VARCHAR(80) NOT NULL,"
            + "phone VARCHAR(20) NOT NULL,"
            + "booked_at TIMESTAMP NOT NULL,"
            + "total INT NOT NULL)",
        // The primary key (show_id, seat) is what makes double-selling a seat impossible.
        "CREATE TABLE IF NOT EXISTS booking_seats ("
            + "booking_id INT NOT NULL REFERENCES bookings(id) ON DELETE CASCADE,"
            + "show_id INT NOT NULL REFERENCES shows(id),"
            + "seat VARCHAR(3) NOT NULL,"
            + "PRIMARY KEY (show_id, seat))"
    };

    private Database() { }

    /** Lets the self-test run against an in-memory database. */
    public static void useUrl(String testUrl) {
        url = testUrl;
    }

    public static Connection connect() throws SQLException {
        return DriverManager.getConnection(url, "sa", "");
    }

    public static void init() throws SQLException {
        try (Connection c = connect(); Statement s = c.createStatement()) {
            for (String sql : SCHEMA) {
                s.execute(sql);
            }
        }
    }

    /** Builds a case-insensitive "contains" pattern, treating % and _ typed by the user literally. */
    public static String likePattern(String query) {
        String escaped = query.trim().toLowerCase(Locale.ROOT)
            .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + escaped + "%";
    }
}
