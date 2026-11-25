package cinema.dao;

import cinema.db.Database;
import cinema.model.Movie;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public final class MovieDao {

    public List<Movie> search(String query) throws SQLException {
        String sql = "SELECT * FROM movies WHERE LOWER(title) LIKE ? ESCAPE '\\' "
            + "OR LOWER(genre) LIKE ? ESCAPE '\\' ORDER BY title";
        String pattern = Database.likePattern(query);
        List<Movie> movies = new ArrayList<>();
        try (Connection c = Database.connect(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, pattern);
            p.setString(2, pattern);
            try (ResultSet r = p.executeQuery()) {
                while (r.next()) {
                    movies.add(new Movie(r.getInt("id"), r.getString("title"), r.getString("genre"),
                        r.getInt("duration_min"), r.getString("rating")));
                }
            }
        }
        return movies;
    }

    public int add(Movie m) throws SQLException {
        try (Connection c = Database.connect();
             PreparedStatement p = c.prepareStatement(
                 "INSERT INTO movies (title, genre, duration_min, rating) VALUES (?, ?, ?, ?)",
                 Statement.RETURN_GENERATED_KEYS)) {
            p.setString(1, m.title);
            p.setString(2, m.genre);
            p.setInt(3, m.durationMin);
            p.setString(4, m.rating);
            p.executeUpdate();
            try (ResultSet keys = p.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    public void update(Movie m) throws SQLException {
        try (Connection c = Database.connect();
             PreparedStatement p = c.prepareStatement(
                 "UPDATE movies SET title = ?, genre = ?, duration_min = ?, rating = ? WHERE id = ?")) {
            p.setString(1, m.title);
            p.setString(2, m.genre);
            p.setInt(3, m.durationMin);
            p.setString(4, m.rating);
            p.setInt(5, m.id);
            p.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        try (Connection c = Database.connect();
             PreparedStatement p = c.prepareStatement("DELETE FROM movies WHERE id = ?")) {
            p.setInt(1, id);
            p.executeUpdate();
        }
    }
}
