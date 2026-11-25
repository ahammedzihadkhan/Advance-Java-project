package cinema.dao;

import cinema.db.Database;
import cinema.model.Hall;
import cinema.model.Show;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ShowDao {
    private static final String SELECT = "SELECT s.id, s.movie_id, s.starts_at, s.price, "
        + "m.title, m.genre, m.duration_min, m.rating, "
        + "(SELECT COUNT(*) FROM booking_seats bs WHERE bs.show_id = s.id) AS sold "
        + "FROM shows s JOIN movies m ON m.id = s.movie_id ";

    /** Shows starting after the given time, soonest first. */
    public List<Show> upcoming(LocalDateTime now) throws SQLException {
        try (Connection c = Database.connect();
             PreparedStatement p = c.prepareStatement(SELECT + "WHERE s.starts_at > ? ORDER BY s.starts_at")) {
            p.setObject(1, now);
            return read(p);
        }
    }

    public List<Show> all() throws SQLException {
        try (Connection c = Database.connect();
             PreparedStatement p = c.prepareStatement(SELECT + "ORDER BY s.starts_at")) {
            return read(p);
        }
    }

    /** Inserts when id is 0, otherwise updates. Rejects times that clash with another show in the hall. */
    public int save(int id, int movieId, LocalDateTime startsAt, int price) throws SQLException, CinemaException {
        try (Connection c = Database.connect()) {
            c.setAutoCommit(false);
            try {
                checkHallFree(c, id, movieId, startsAt);
                int savedId = id;
                if (id == 0) {
                    try (PreparedStatement p = c.prepareStatement(
                            "INSERT INTO shows (movie_id, starts_at, price) VALUES (?, ?, ?)",
                            Statement.RETURN_GENERATED_KEYS)) {
                        p.setInt(1, movieId);
                        p.setObject(2, startsAt);
                        p.setInt(3, price);
                        p.executeUpdate();
                        try (ResultSet keys = p.getGeneratedKeys()) {
                            keys.next();
                            savedId = keys.getInt(1);
                        }
                    }
                } else {
                    try (PreparedStatement p = c.prepareStatement(
                            "UPDATE shows SET movie_id = ?, starts_at = ?, price = ? WHERE id = ?")) {
                        p.setInt(1, movieId);
                        p.setObject(2, startsAt);
                        p.setInt(3, price);
                        p.setInt(4, id);
                        p.executeUpdate();
                    }
                }
                c.commit();
                return savedId;
            } catch (SQLException | CinemaException e) {
                c.rollback();
                throw e;
            }
        }
    }

    public void delete(int id) throws SQLException {
        try (Connection c = Database.connect();
             PreparedStatement p = c.prepareStatement("DELETE FROM shows WHERE id = ?")) {
            p.setInt(1, id);
            p.executeUpdate();
        }
    }

    private static void checkHallFree(Connection c, int showId, int movieId, LocalDateTime start)
            throws SQLException, CinemaException {
        int duration;
        try (PreparedStatement p = c.prepareStatement("SELECT duration_min FROM movies WHERE id = ?")) {
            p.setInt(1, movieId);
            try (ResultSet r = p.executeQuery()) {
                if (!r.next()) {
                    throw new CinemaException("That movie no longer exists.");
                }
                duration = r.getInt(1);
            }
        }
        LocalDateTime end = start.plusMinutes(duration + Hall.CLEANING_MIN);
        // Two shows clash when each one starts before the other one ends.
        String sql = "SELECT m.title, s.starts_at FROM shows s JOIN movies m ON m.id = s.movie_id "
            + "WHERE s.id <> ? AND s.starts_at < ? "
            + "AND DATEADD(MINUTE, m.duration_min + " + Hall.CLEANING_MIN + ", s.starts_at) > ?";
        try (PreparedStatement p = c.prepareStatement(sql)) {
            p.setInt(1, showId);
            p.setObject(2, end);
            p.setObject(3, start);
            try (ResultSet r = p.executeQuery()) {
                if (r.next()) {
                    LocalDateTime other = r.getObject(2, LocalDateTime.class);
                    throw new CinemaException("The hall is busy then: \"" + r.getString(1) + "\" starts at "
                        + other.format(DateTimeFormatter.ofPattern("d MMM, h:mm a", Locale.ENGLISH))
                        + " (shows need " + Hall.CLEANING_MIN + " minutes between them).");
                }
            }
        }
    }

    private static List<Show> read(PreparedStatement p) throws SQLException {
        List<Show> shows = new ArrayList<>();
        try (ResultSet r = p.executeQuery()) {
            while (r.next()) {
                shows.add(new Show(r.getInt("id"), r.getInt("movie_id"), r.getString("title"),
                    r.getString("genre"), r.getInt("duration_min"), r.getString("rating"),
                    r.getObject("starts_at", LocalDateTime.class), r.getInt("price"), r.getInt("sold")));
            }
        }
        return shows;
    }
}
