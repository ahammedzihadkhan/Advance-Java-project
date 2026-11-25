package cinema.dao;

import cinema.db.Database;
import cinema.model.Booking;
import cinema.model.Hall;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class BookingDao {

    public static final class Stats {
        public final int showsToday;
        public final int ticketsToday;
        public final int revenueToday;

        Stats(int showsToday, int ticketsToday, int revenueToday) {
            this.showsToday = showsToday;
            this.ticketsToday = ticketsToday;
            this.revenueToday = revenueToday;
        }
    }

    public Set<String> takenSeats(int showId) throws SQLException {
        Set<String> seats = new HashSet<>();
        try (Connection c = Database.connect();
             PreparedStatement p = c.prepareStatement("SELECT seat FROM booking_seats WHERE show_id = ?")) {
            p.setInt(1, showId);
            try (ResultSet r = p.executeQuery()) {
                while (r.next()) {
                    seats.add(r.getString(1));
                }
            }
        }
        return seats;
    }

    /**
     * Books all the seats or none of them, in one transaction. If another booking already holds
     * any seat, the (show_id, seat) primary key rejects the insert and everything is rolled back.
     */
    public int book(int showId, String customer, String phone, Collection<String> seats, LocalDateTime now)
            throws SQLException, CinemaException {
        if (seats.isEmpty()) {
            throw new CinemaException("Choose at least one seat.");
        }
        if (seats.size() > Hall.MAX_SEATS_PER_BOOKING) {
            throw new CinemaException("One booking can have at most " + Hall.MAX_SEATS_PER_BOOKING + " seats.");
        }
        for (String seat : seats) {
            if (!Hall.isValidSeat(seat)) {
                throw new CinemaException("There is no seat called " + seat + ".");
            }
        }
        try (Connection c = Database.connect()) {
            c.setAutoCommit(false);
            try {
                int price;
                try (PreparedStatement p = c.prepareStatement("SELECT starts_at, price FROM shows WHERE id = ?")) {
                    p.setInt(1, showId);
                    try (ResultSet r = p.executeQuery()) {
                        if (!r.next()) {
                            throw new CinemaException("That show no longer exists.");
                        }
                        if (!r.getObject(1, LocalDateTime.class).isAfter(now)) {
                            throw new CinemaException("This show has already started.");
                        }
                        price = r.getInt(2);
                    }
                }
                int bookingId;
                try (PreparedStatement p = c.prepareStatement(
                        "INSERT INTO bookings (show_id, customer, phone, booked_at, total) VALUES (?, ?, ?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    p.setInt(1, showId);
                    p.setString(2, customer);
                    p.setString(3, phone);
                    p.setObject(4, now);
                    p.setInt(5, price * seats.size());
                    p.executeUpdate();
                    try (ResultSet keys = p.getGeneratedKeys()) {
                        keys.next();
                        bookingId = keys.getInt(1);
                    }
                }
                try (PreparedStatement p = c.prepareStatement(
                        "INSERT INTO booking_seats (booking_id, show_id, seat) VALUES (?, ?, ?)")) {
                    for (String seat : seats) {
                        p.setInt(1, bookingId);
                        p.setInt(2, showId);
                        p.setString(3, seat);
                        p.addBatch();
                    }
                    p.executeBatch();
                }
                c.commit();
                return bookingId;
            } catch (CinemaException e) {
                c.rollback();
                throw e;
            } catch (SQLException e) {
                c.rollback();
                if ("23505".equals(e.getSQLState())) {
                    throw new CinemaException("One of those seats was just sold. Please pick again.");
                }
                throw e;
            }
        }
    }

    public List<Booking> search(String query) throws SQLException {
        String sql = "SELECT b.id, b.customer, b.phone, b.total, s.starts_at, m.title, "
            + "LISTAGG(bs.seat, ' ') WITHIN GROUP (ORDER BY LEFT(bs.seat, 1), CAST(SUBSTRING(bs.seat, 2) AS INT)) AS seats "
            + "FROM bookings b JOIN shows s ON s.id = b.show_id JOIN movies m ON m.id = s.movie_id "
            + "JOIN booking_seats bs ON bs.booking_id = b.id "
            + "WHERE LOWER(b.customer) LIKE ? ESCAPE '\\' OR b.phone LIKE ? ESCAPE '\\' "
            + "OR LOWER(m.title) LIKE ? ESCAPE '\\' OR CAST(b.id AS VARCHAR) = ? "
            + "GROUP BY b.id, b.customer, b.phone, b.total, s.starts_at, m.title "
            + "ORDER BY s.starts_at DESC, b.id DESC";
        String pattern = Database.likePattern(query);
        List<Booking> bookings = new ArrayList<>();
        try (Connection c = Database.connect(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setString(1, pattern);
            p.setString(2, pattern);
            p.setString(3, pattern);
            p.setString(4, query.trim().replace("#", ""));
            try (ResultSet r = p.executeQuery()) {
                while (r.next()) {
                    bookings.add(new Booking(r.getInt("id"), r.getString("customer"), r.getString("phone"),
                        r.getString("title"), r.getObject("starts_at", LocalDateTime.class),
                        r.getString("seats"), r.getInt("total")));
                }
            }
        }
        return bookings;
    }

    /** Cancels a booking for a show that hasn't started; its seats are freed by ON DELETE CASCADE. */
    public void cancel(int bookingId, LocalDateTime now) throws SQLException, CinemaException {
        try (Connection c = Database.connect();
             PreparedStatement p = c.prepareStatement("DELETE FROM bookings WHERE id = ? "
                 + "AND show_id IN (SELECT id FROM shows WHERE starts_at > ?)")) {
            p.setInt(1, bookingId);
            p.setObject(2, now);
            if (p.executeUpdate() == 0) {
                throw new CinemaException("This show has already started, so the booking can't be cancelled.");
            }
        }
    }

    public Stats stats(LocalDate day) throws SQLException {
        String sql = "SELECT (SELECT COUNT(*) FROM shows WHERE starts_at >= ? AND starts_at < ?), "
            + "(SELECT COUNT(*) FROM booking_seats bs JOIN shows s ON s.id = bs.show_id "
            + "WHERE s.starts_at >= ? AND s.starts_at < ?), "
            + "(SELECT COALESCE(SUM(b.total), 0) FROM bookings b JOIN shows s ON s.id = b.show_id "
            + "WHERE s.starts_at >= ? AND s.starts_at < ?)";
        try (Connection c = Database.connect(); PreparedStatement p = c.prepareStatement(sql)) {
            for (int i = 1; i <= 6; i += 2) {
                p.setObject(i, day.atStartOfDay());
                p.setObject(i + 1, day.plusDays(1).atStartOfDay());
            }
            try (ResultSet r = p.executeQuery()) {
                r.next();
                return new Stats(r.getInt(1), r.getInt(2), r.getInt(3));
            }
        }
    }
}
