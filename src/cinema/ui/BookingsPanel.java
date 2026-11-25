package cinema.ui;

import cinema.dao.BookingDao;
import cinema.dao.CinemaException;
import cinema.model.Booking;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

final class BookingsPanel extends JPanel implements MainFrame.Page {
    private static final long serialVersionUID = 1L;

    private final transient BookingDao bookings = new BookingDao();
    private final DefaultTableModel model = Ui.model("#", "Customer", "Phone", "Movie", "Show time", "Seats", "Total");
    private final JTable table = Ui.table(model);
    private final JTextField search = Ui.flat(new JTextField(24));
    private transient List<Booking> rows = new ArrayList<>();

    BookingsPanel() {
        super(new BorderLayout(0, 18));
        setOpaque(false);
        Ui.columnWidths(table, 50, 170, 130, 190, 190, 170, 80);

        JButton cancel = Ui.button("Cancel booking", Ui.Style.DANGER);
        cancel.addActionListener(e -> cancelSelected());
        add(Ui.header("Bookings", Ui.subtitle("Find a booking by customer, phone, movie or booking number.")),
            BorderLayout.NORTH);

        JLabel searchLabel = new JLabel("Search");
        searchLabel.setLabelFor(search);
        Ui.onChange(search, this::refresh);
        JPanel left = Ui.clear(new FlowLayout(FlowLayout.LEFT, 10, 0));
        left.add(searchLabel);
        left.add(search);
        JPanel bar = Ui.clear(new BorderLayout());
        bar.add(left, BorderLayout.WEST);
        bar.add(cancel, BorderLayout.EAST);

        Glass body = new Glass(new BorderLayout(0, 14));
        body.add(bar, BorderLayout.NORTH);
        body.add(Ui.scroll(table), BorderLayout.CENTER);
        add(body, BorderLayout.CENTER);
    }

    @Override
    public void refresh() {
        try {
            rows = bookings.search(search.getText());
            model.setRowCount(0);
            for (Booking b : rows) {
                model.addRow(new Object[] {b.id, b.customer, b.phone, b.title, b.startsAt, b.seats, b.total});
            }
        } catch (SQLException e) {
            Ui.error(this, e);
        }
    }

    private void cancelSelected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            Ui.warn(this, "Select a booking first.");
            return;
        }
        Booking b = rows.get(table.convertRowIndexToModel(row));
        int answer = JOptionPane.showConfirmDialog(this, "Cancel " + b + "?\nIts seats will go back on sale.",
            "Cancel booking", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (answer != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            bookings.cancel(b.id, LocalDateTime.now());
            refresh();
        } catch (SQLException | CinemaException e) {
            Ui.error(this, e);
        }
    }
}
