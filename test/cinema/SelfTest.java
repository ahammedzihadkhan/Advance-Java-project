package cinema;

import cinema.dao.BookingDao;
import cinema.dao.CinemaException;
import cinema.dao.MovieDao;
import cinema.dao.ShowDao;
import cinema.dao.UserDao;
import cinema.db.Database;
import cinema.db.Seed;
import cinema.model.Booking;
import cinema.model.Movie;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Checks the JDBC layer against a throwaway in-memory database. Run with test.bat. */
public final class SelfTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 3, 10, 9, 0);
    private static final LocalDateTime EVENING = NOW.plusDays(1).withHour(18);
    private static final MovieDao MOVIES = new MovieDao();
    private static final ShowDao SHOWS = new ShowDao();
    private static final BookingDao BOOKINGS = new BookingDao();
    private static int passed;

    private interface Action {
        void run() throws Exception;
    }

    private SelfTest() { }

    public static void main(String[] args) throws Exception {
        Database.useUrl("jdbc:h2:mem:selftest;DB_CLOSE_DELAY=-1");
        Database.init();

        UserDao users = new UserDao();
        users.create("admin", "secret".toCharArray());
        check("correct password accepted", users.authenticate("admin", "secret".toCharArray()));
        check("wrong password rejected", !users.authenticate("admin", "nope".toCharArray()));
        check("SQL injection login rejected", !users.authenticate("' OR '1'='1", "x".toCharArray()));

        int longFilm = MOVIES.add(new Movie(0, "Long Film", "Drama", 120, "PG"));
        int shortFilm = MOVIES.add(new Movie(0, "Short Film", "Comedy", 90, "G"));
        check("movie search is case-insensitive", MOVIES.search("LONG").size() == 1);
        check("% is searched literally", MOVIES.search("%").isEmpty());

        int show = SHOWS.save(0, longFilm, EVENING, 400);
        rejectedRule("show starting during another", () -> SHOWS.save(0, shortFilm, EVENING.plusHours(1), 300));
        rejectedRule("show running into another", () -> SHOWS.save(0, shortFilm, EVENING.minusHours(1), 300));
        int later = SHOWS.save(0, shortFilm, EVENING.plusMinutes(120 + 15), 300);
        check("show right after cleaning time is allowed", later > 0);
        check("a show can be re-saved at its own time", SHOWS.save(show, longFilm, EVENING, 400) == show);

        int first = BOOKINGS.book(show, "Ayesha", "01700000001", List.of("E5", "E6"), NOW);
        check("booking takes the seats", BOOKINGS.takenSeats(show).containsAll(List.of("E5", "E6")));
        check("booking total is seats x price", BOOKINGS.search("ayesha").get(0).total == 800);

        rejectedRule("booking a sold seat", () -> BOOKINGS.book(show, "Tanvir", "01700000002", List.of("E7", "E6"), NOW));
        check("failed booking saved no seats (rollback)", BOOKINGS.takenSeats(show).size() == 2);
        check("failed booking saved no booking row", BOOKINGS.search("tanvir").isEmpty());

        rejectedRule("unknown seat", () -> BOOKINGS.book(show, "X", "01700000003", List.of("Z99"), NOW));
        rejectedRule("empty seat list", () -> BOOKINGS.book(show, "X", "01700000003", List.of(), NOW));
        List<String> eleven = new ArrayList<>();
        for (int i = 1; i <= 11; i++) {
            eleven.add("A" + Math.min(i, 12));
        }
        rejectedRule("more than 10 seats", () -> BOOKINGS.book(show, "X", "01700000003", eleven, NOW));
        rejectedRule("booking a show that started", () -> BOOKINGS.book(show, "X", "01700000003", List.of("A1"), EVENING));

        BOOKINGS.book(show, "Nusrat", "01700000004", List.of("C10", "C2"), NOW);
        Booking nusrat = BOOKINGS.search("nusrat").get(0);
        check("seats are listed in hall order", "C2 C10".equals(nusrat.seats));
        check("search by booking number", BOOKINGS.search("#" + nusrat.id).size() == 1);
        check("show list counts sold seats", SHOWS.all().get(0).sold == 4);

        BookingDao.Stats stats = BOOKINGS.stats(EVENING.toLocalDate());
        check("stats count shows of the day", stats.showsToday == 2);
        check("stats count tickets of the day", stats.ticketsToday == 4);
        check("stats add up revenue of the day", stats.revenueToday == 1600);
        check("stats for another day are zero", BOOKINGS.stats(LocalDate.of(2020, 1, 1)).ticketsToday == 0);

        rejectedRule("cancelling after the show started", () -> BOOKINGS.cancel(first, EVENING.plusMinutes(1)));
        BOOKINGS.cancel(first, NOW);
        check("cancelling frees the seats", !BOOKINGS.takenSeats(show).contains("E5"));

        rejectedSql("deleting a movie that has shows", "23503", () -> MOVIES.delete(longFilm));
        rejectedSql("deleting a show that has bookings", "23503", () -> SHOWS.delete(show));
        SHOWS.delete(later);
        check("a show without bookings can be deleted", SHOWS.all().size() == 1);

        Database.useUrl("jdbc:h2:mem:seedtest;DB_CLOSE_DELAY=-1");
        Database.init();
        Seed.run();
        Seed.run();
        check("demo data loads once", MOVIES.search("").size() == 6 && SHOWS.all().size() == 10);
        check("demo data has bookings", !BOOKINGS.search("").isEmpty());

        System.out.println("All " + passed + " checks passed.");
    }

    private static void check(String name, boolean ok) {
        if (!ok) {
            throw new AssertionError("FAILED: " + name);
        }
        passed++;
        System.out.println("ok - " + name);
    }

    private static void rejectedRule(String name, Action action) throws Exception {
        try {
            action.run();
        } catch (CinemaException expected) {
            check(name + " is rejected", true);
            return;
        }
        check(name + " is rejected", false);
    }

    private static void rejectedSql(String name, String sqlState, Action action) throws Exception {
        try {
            action.run();
        } catch (SQLException e) {
            check(name + " is rejected (" + sqlState + ")", sqlState.equals(e.getSQLState()));
            return;
        }
        check(name + " is rejected", false);
    }
}
