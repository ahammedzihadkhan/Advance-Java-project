package cinema.ui;

import cinema.dao.MovieDao;
import cinema.model.Movie;
import java.sql.SQLException;
import java.util.List;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

final class MoviesPanel extends CrudPanel<Movie> {
    private static final long serialVersionUID = 1L;
    private static final String[] GENRES = {"Action", "Animation", "Comedy", "Drama", "Horror", "Romance", "Thriller"};
    private static final String[] RATINGS = {"G", "PG", "PG-13", "R"};

    private final transient MovieDao movies = new MovieDao();

    MoviesPanel() {
        super("Movies", "The films the cinema can schedule.", "Add movie", "Title", "Genre", "Minutes", "Rating");
        columnWidths(380, 180, 100, 100);
    }

    @Override
    List<Movie> load(String query) throws SQLException {
        return movies.search(query);
    }

    @Override
    Object[] toRow(Movie m) {
        return new Object[] {m.title, m.genre, m.durationMin, m.rating};
    }

    @Override
    Movie showForm(Movie m, String action) {
        JTextField title = new JTextField(m == null ? "" : m.title, 24);
        JComboBox<String> genre = new JComboBox<>(GENRES);
        genre.setEditable(true);
        JSpinner minutes = new JSpinner(new SpinnerNumberModel(m == null ? 110 : m.durationMin, 30, 300, 5));
        JComboBox<String> rating = new JComboBox<>(RATINGS);
        if (m != null) {
            genre.setSelectedItem(m.genre);
            rating.setSelectedItem(m.rating);
        }
        JComponent form = Ui.form(new String[] {"Title", "Genre", "Running time (minutes)", "Rating"},
            new JComponent[] {title, genre, minutes, rating});

        while (JOptionPane.showConfirmDialog(this, form, action + " movie", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            String cleanTitle = title.getText().trim();
            String cleanGenre = String.valueOf(genre.getSelectedItem()).trim();
            if (cleanTitle.isEmpty() || cleanTitle.length() > 120) {
                Ui.warn(this, "Enter a title (up to 120 characters).");
            } else if (cleanGenre.isEmpty() || cleanGenre.length() > 40) {
                Ui.warn(this, "Enter a genre (up to 40 characters).");
            } else {
                return new Movie(m == null ? 0 : m.id, cleanTitle, cleanGenre, (Integer) minutes.getValue(),
                    (String) rating.getSelectedItem());
            }
        }
        return null;
    }

    @Override
    void insert(Movie m) throws SQLException {
        movies.add(m);
    }

    @Override
    void update(Movie m) throws SQLException {
        movies.update(m);
    }

    @Override
    void remove(Movie m) throws SQLException {
        movies.delete(m.id);
    }
}
