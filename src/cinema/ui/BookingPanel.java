package cinema.ui;

import cinema.dao.BookingDao;
import cinema.dao.CinemaException;
import cinema.model.Hall;
import cinema.model.Show;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.MatteBorder;

/** Seat selection for one show, with the customer's details and the running total. */
final class BookingPanel extends JPanel implements MainFrame.Page {
    private static final long serialVersionUID = 1L;

    private final transient BookingDao bookings = new BookingDao();
    private final transient Runnable onDone;
    private transient Show show;
    private final JLabel subtitle = Ui.subtitle(" ");
    private final SeatMap map = new SeatMap(this::updateSummary);
    private final JLabel seatList = Ui.label(" ", Ui.sans(Font.BOLD, 15f), Ui.TEXT);
    private final JLabel total = Ui.label("0", Ui.sans(Font.BOLD, 34f), Ui.TEXT);
    private final JLabel perSeat = Ui.label(" ", Ui.sans(Font.PLAIN, 12.5f), Ui.MUTED);
    private final JTextField name = new JTextField(16);
    private final JTextField phone = new JTextField(16);
    private final JButton confirm = Ui.button("Confirm booking", Ui.Style.PRIMARY);

    BookingPanel(Runnable onDone) {
        super(new BorderLayout(18, 18));
        this.onDone = onDone;
        setOpaque(false);

        JButton back = Ui.button("Back to shows", Ui.Style.GHOST);
        back.addActionListener(e -> onDone.run());
        add(Ui.header("Choose seats", subtitle, back), BorderLayout.NORTH);

        Glass hall = new Glass(new GridBagLayout());
        hall.add(map);
        add(hall, BorderLayout.CENTER);

        JPanel details = Ui.form(new String[] {"Customer", "Phone"}, new JComponent[] {name, phone});
        details.setMaximumSize(new Dimension(Integer.MAX_VALUE, details.getPreferredSize().height));
        confirm.setMaximumSize(new Dimension(Integer.MAX_VALUE, confirm.getPreferredSize().height));
        confirm.addActionListener(e -> confirm());
        JLabel hint = Ui.label("<html><div style='width:240px'>Up to " + Hall.MAX_SEATS_PER_BOOKING + " seats per booking. Click a seat, "
            + "or use the arrow keys and Space.</div></html>", Ui.sans(Font.PLAIN, 12f), Ui.MUTED);
        JPanel rule = Ui.clear(null);
        rule.setBorder(new MatteBorder(1, 0, 0, 0, Ui.LINE));
        rule.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));

        Glass summary = new Glass(null);
        summary.setLayout(new BoxLayout(summary, BoxLayout.Y_AXIS));
        summary.setPreferredSize(new Dimension(310, 0));
        JLabel seatsCaption = Ui.caps("Your seats");
        JLabel totalCaption = Ui.caps("Total");
        for (JComponent c : new JComponent[] {seatsCaption, seatList, totalCaption, total, perSeat, rule,
                details, confirm, hint}) {
            c.setAlignmentX(Component.LEFT_ALIGNMENT);
        }
        summary.add(seatsCaption);
        summary.add(Box.createVerticalStrut(6));
        summary.add(seatList);
        summary.add(Box.createVerticalStrut(22));
        summary.add(totalCaption);
        summary.add(total);
        summary.add(perSeat);
        summary.add(Box.createVerticalStrut(20));
        summary.add(rule);
        summary.add(Box.createVerticalStrut(16));
        summary.add(details);
        summary.add(Box.createVerticalStrut(14));
        summary.add(confirm);
        summary.add(Box.createVerticalStrut(12));
        summary.add(hint);
        summary.add(Box.createVerticalGlue());
        add(summary, BorderLayout.EAST);
    }

    void load(Show s) {
        show = s;
        name.setText("");
        phone.setText("");
        refresh();
    }

    @Override
    public void refresh() {
        if (show == null) {
            return;
        }
        subtitle.setText(show.title + "  ·  " + show.startsAt.format(Ui.WHEN) + "  ·  "
            + Ui.money(show.price) + " per seat");
        try {
            map.load(bookings.takenSeats(show.id));
        } catch (SQLException e) {
            Ui.error(this, e);
        }
        map.requestFocusInWindow();
    }

    private void updateSummary() {
        Set<String> seats = map.selected();
        int price = show == null ? 0 : show.price;
        seatList.setText(seats.isEmpty() ? "None yet"
            : "<html><div style='width:230px'>" + String.join("  ", seats) + "</div></html>");
        total.setText(Ui.money(seats.size() * price));
        perSeat.setText(seats.size() + (seats.size() == 1 ? " seat" : " seats") + " × " + Ui.money(price));
        confirm.setEnabled(!seats.isEmpty());
    }

    private void confirm() {
        String customer = name.getText().trim();
        String tel = phone.getText().trim();
        if (customer.isEmpty() || customer.length() > 80) {
            Ui.warn(this, "Enter the customer's name (up to 80 characters).");
            name.requestFocusInWindow();
            return;
        }
        if (!tel.matches("[+0-9 ()-]{6,20}")) {
            Ui.warn(this, "Enter a phone number using digits, spaces and + ( ) -.");
            phone.requestFocusInWindow();
            return;
        }
        List<String> seats = new ArrayList<>(map.selected());
        try {
            int id = bookings.book(show.id, customer, tel, seats, LocalDateTime.now());
            TicketDialog.show(this, id, show, customer, seats);
            onDone.run();
        } catch (CinemaException e) {
            Ui.error(this, e);
            refresh(); // reload so seats sold in the meantime show as taken
        } catch (SQLException e) {
            Ui.error(this, e);
        }
    }
}
