package cinema.ui;

import cinema.dao.CinemaException;
import cinema.dao.MovieDao;
import cinema.dao.ShowDao;
import cinema.model.Movie;
import cinema.model.Show;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

final class ShowsPanel extends CrudPanel<Show> {
    private static final long serialVersionUID = 1L;

    private final transient ShowDao shows = new ShowDao();
    private final transient MovieDao movies = new MovieDao();

    ShowsPanel() {
        super("Shows", "The screening timetable for Hall 1. Shows can't overlap.", "Add show",
            "Starts", "Movie", "Runtime", "Price", "Seats sold");
        columnWidths(210, 300, 110, 90, 110);
    }

    @Override
    List<Show> load(String query) throws SQLException {
        String q = query.trim().toLowerCase(Locale.ROOT);
        // shortcut: filters in memory, fine for one hall's timetable; move to SQL if it grows to thousands of shows.
        return shows.all().stream()
            .filter(s -> s.title.toLowerCase(Locale.ROOT).contains(q) || s.startsAt.format(Ui.WHEN)
                .toLowerCase(Locale.ROOT).contains(q))
            .collect(Collectors.toList());
    }

    @Override
    Object[] toRow(Show s) {
        return new Object[] {s.startsAt, s.title, Ui.runtime(s.durationMin), s.price, s.sold};
    }

    @Override
    Show showForm(Show s, String action) {
        List<Movie> all;
        try {
            all = movies.search("");
        } catch (SQLException e) {
            Ui.error(this, e);
            return null;
        }
        if (all.isEmpty()) {
            Ui.warn(this, "Add a movie first.");
            return null;
        }
        JComboBox<Movie> movie = new JComboBox<>(all.toArray(new Movie[0]));
        for (Movie m : all) {
            if (s != null && m.id == s.movieId) {
                movie.setSelectedItem(m);
            }
        }
        LocalDateTime start = s != null ? s.startsAt : LocalDate.now().plusDays(1).atTime(19, 0);
        JTextField date = new JTextField(start.toLocalDate().toString(), 12);
        JTextField time = new JTextField(start.toLocalTime().toString(), 12);
        JSpinner price = new JSpinner(new SpinnerNumberModel(s != null ? s.price : 400, 50, 5000, 50));
        JComponent form = Ui.form(new String[] {"Movie", "Date (YYYY-MM-DD)", "Time (24-hour, HH:MM)", "Ticket price"},
            new JComponent[] {movie, date, time, price});

        while (JOptionPane.showConfirmDialog(this, form, action + " show", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            LocalDateTime when;
            try {
                when = LocalDateTime.of(LocalDate.parse(date.getText().trim()), LocalTime.parse(time.getText().trim()));
            } catch (DateTimeParseException e) {
                Ui.warn(this, "Use the date format 2026-12-01 and the time format 19:30.");
                continue;
            }
            boolean timeChanged = s == null || !when.equals(s.startsAt);
            if (timeChanged && !when.isAfter(LocalDateTime.now())) {
                Ui.warn(this, "A show must be scheduled in the future.");
                continue;
            }
            Movie m = (Movie) movie.getSelectedItem();
            return new Show(s == null ? 0 : s.id, m.id, m.title, m.genre, m.durationMin, m.rating, when,
                (Integer) price.getValue(), s == null ? 0 : s.sold);
        }
        return null;
    }

    @Override
    void insert(Show s) throws SQLException, CinemaException {
        shows.save(0, s.movieId, s.startsAt, s.price);
    }

    @Override
    void update(Show s) throws SQLException, CinemaException {
        shows.save(s.id, s.movieId, s.startsAt, s.price);
    }

    @Override
    void remove(Show s) throws SQLException {
        shows.delete(s.id);
    }
}
