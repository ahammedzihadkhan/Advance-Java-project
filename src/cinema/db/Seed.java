package cinema.db;

import cinema.dao.BookingDao;
import cinema.dao.CinemaException;
import cinema.dao.MovieDao;
import cinema.dao.ShowDao;
import cinema.dao.UserDao;
import cinema.model.Hall;
import cinema.model.Movie;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Fills an empty database with demo data on first run. */
public final class Seed {
    private static final String[][] MOVIES = {
        {"The Last Ferry", "Drama", "118", "PG-13"},
        {"Monsoon Letters", "Romance", "104", "PG"},
        {"Iron Orchard", "Action", "132", "PG-13"},
        {"Paper Planets", "Animation", "95", "G"},
        {"Quiet Harbour", "Thriller", "111", "R"},
        {"Small Hours", "Comedy", "99", "PG-13"},
    };
    private static final int[] SHOW_HOURS = {10, 13, 16, 19, 22};
    private static final String[] CUSTOMERS = {
        "Ayesha Rahman", "Tanvir Hasan", "Nusrat Jahan", "Rafiq Islam", "Sadia Karim", "Imran Hossain",
    };

    private Seed() { }

    public static void run() throws SQLException, CinemaException {
        UserDao users = new UserDao();
        if (users.hasUsers()) {
            return;
        }
        users.create("admin", "admin123".toCharArray());

        MovieDao movies = new MovieDao();
        int[] movieIds = new int[MOVIES.length];
        for (int i = 0; i < MOVIES.length; i++) {
            String[] m = MOVIES[i];
            movieIds[i] = movies.add(new Movie(0, m[0], m[1], Integer.parseInt(m[2]), m[3]));
        }

        ShowDao shows = new ShowDao();
        BookingDao bookings = new BookingDao();
        LocalDate today = LocalDate.now();
        int index = 0;
        for (int day = 0; day < 2; day++) {
            for (int slot = 0; slot < SHOW_HOURS.length; slot++, index++) {
                int hour = SHOW_HOURS[slot];
                LocalDateTime start = today.plusDays(day).atTime(hour, 0);
                int showId = shows.save(0, movieIds[(slot + day * 2) % MOVIES.length], start, hour >= 18 ? 450 : 350);
                seedBookings(bookings, showId, start, index);
            }
        }
    }

    // Booked "a day before" the show, so even today's earlier shows get realistic history.
    private static void seedBookings(BookingDao bookings, int showId, LocalDateTime start, int index)
            throws SQLException, CinemaException {
        boolean busy = index == 3;
        int groups = busy ? Hall.ROWS.length() : index % 4 + 1;
        for (int g = 0; g < groups; g++) {
            int row = busy ? g : 2 + g;
            int count = busy ? Hall.MAX_SEATS_PER_BOOKING : 2 + (g + index) % 3;
            int firstSeat = busy ? 1 : 3 + index % 4;
            List<String> seats = new ArrayList<>();
            for (int s = 0; s < count; s++) {
                seats.add(Hall.seat(row, firstSeat + s));
            }
            int person = (index + g) % CUSTOMERS.length;
            bookings.book(showId, CUSTOMERS[person], "01700-00000" + person, seats, start.minusDays(1));
        }
    }
}
