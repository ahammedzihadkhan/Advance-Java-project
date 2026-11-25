package cinema.ui;

import cinema.dao.BookingDao;
import cinema.dao.ShowDao;
import cinema.model.Hall;
import cinema.model.Show;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Scrollable;

/** Upcoming shows as cards, each with a seat-occupancy bar and a button to start a booking. */
final class NowShowingPanel extends JPanel implements MainFrame.Page {
    private static final long serialVersionUID = 1L;

    private final transient ShowDao shows = new ShowDao();
    private final transient BookingDao bookings = new BookingDao();
    private final transient Consumer<Show> onChooseSeats;
    private final JLabel summary = Ui.subtitle(" ");
    private final JPanel grid = Ui.clear(new GridLayout(0, 3, 16, 16));

    NowShowingPanel(Consumer<Show> onChooseSeats) {
        super(new BorderLayout(0, 18));
        this.onChooseSeats = onChooseSeats;
        setOpaque(false);
        add(Ui.header("Now Showing", summary), BorderLayout.NORTH);
        WidthTracking holder = new WidthTracking();
        holder.add(grid, BorderLayout.NORTH);
        add(Ui.scroll(holder), BorderLayout.CENTER);
    }

    @Override
    public void refresh() {
        LocalDateTime now = LocalDateTime.now();
        try {
            BookingDao.Stats today = bookings.stats(now.toLocalDate());
            summary.setText("Today: " + today.showsToday + " shows, " + today.ticketsToday + " tickets sold, "
                + Ui.money(today.revenueToday) + " in ticket sales.");
            List<Show> upcoming = shows.upcoming(now);
            grid.removeAll();
            if (upcoming.isEmpty()) {
                grid.add(Ui.label("No upcoming shows. Add one on the Shows page.", Ui.sans(Font.PLAIN, 14f), Ui.MUTED));
            }
            // Cards arrive one after another, 60 ms apart.
            long first = System.nanoTime() + 120_000_000L;
            for (int i = 0; i < upcoming.size(); i++) {
                grid.add(card(upcoming.get(i), now.toLocalDate(), first + i * 60_000_000L));
            }
            grid.revalidate();
            grid.repaint();
        } catch (SQLException e) {
            Ui.error(this, e);
        }
    }

    private JComponent card(Show show, LocalDate today, long enterAt) {
        LocalDate date = show.startsAt.toLocalDate();
        String day = date.equals(today) ? "Today" : date.equals(today.plusDays(1)) ? "Tomorrow" : date.format(Ui.DAY);

        JLabel dayLabel = Ui.caps(day);
        JLabel time = Ui.label(show.startsAt.format(Ui.TIME), Ui.sans(Font.BOLD, 28f), Ui.TEXT);
        JLabel title = Ui.label(show.title, Ui.sans(Font.BOLD, 16.5f), Ui.TEXT);
        JLabel meta = Ui.label(show.genre + "  ·  " + Ui.runtime(show.durationMin) + "  ·  " + show.rating,
            Ui.sans(Font.PLAIN, 13f), Ui.MUTED);
        Occupancy bar = new Occupancy(show.sold / (float) Hall.CAPACITY);
        JLabel seats = Ui.label(show.sold + " sold  ·  " + show.seatsLeft() + " left",
            Ui.sans(Font.PLAIN, 12.5f), Ui.MUTED);

        JLabel price = Ui.label(Ui.money(show.price), Ui.sans(Font.BOLD, 22f), Ui.TEXT);
        JLabel perSeat = Ui.label("per seat", Ui.sans(Font.PLAIN, 12f), Ui.MUTED);
        JPanel priceBlock = Ui.clear(null);
        priceBlock.setLayout(new BoxLayout(priceBlock, BoxLayout.Y_AXIS));
        priceBlock.add(price);
        priceBlock.add(perSeat);
        JButton choose = Ui.button(show.seatsLeft() > 0 ? "Choose seats" : "Sold out", Ui.Style.PRIMARY);
        choose.setEnabled(show.seatsLeft() > 0);
        choose.getAccessibleContext().setAccessibleName("Choose seats for " + show.title + " at "
            + show.startsAt.format(Ui.WHEN));
        choose.addActionListener(e -> onChooseSeats.accept(show));
        JPanel bottom = Ui.clear(new BorderLayout(10, 0));
        bottom.add(priceBlock, BorderLayout.WEST);
        bottom.add(choose, BorderLayout.EAST);

        Glass card = new Glass(null).hoverable().entering(enterAt);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        for (JComponent c : new JComponent[] {dayLabel, time, title, meta, bar, seats, bottom}) {
            c.setAlignmentX(Component.LEFT_ALIGNMENT);
        }
        card.add(dayLabel);
        card.add(time);
        card.add(Box.createVerticalStrut(8));
        card.add(title);
        card.add(meta);
        card.add(Box.createVerticalStrut(14));
        card.add(bar);
        card.add(Box.createVerticalStrut(6));
        card.add(seats);
        card.add(Box.createVerticalStrut(16));
        card.add(bottom);
        return card;
    }

    /** A thin brass bar showing how full the hall is. */
    private static final class Occupancy extends JPanel {
        private static final long serialVersionUID = 1L;
        private final float filled;
        private final long createdAt = System.nanoTime();

        Occupancy(float filled) {
            this.filled = Math.max(0f, Math.min(1f, filled));
            setOpaque(false);
            setPreferredSize(new Dimension(200, 6));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 6));
            getAccessibleContext().setAccessibleName(String.format(Locale.ENGLISH, "%.0f%% full", this.filled * 100));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Ui.smooth(g);
            int h = getHeight();
            g2.setColor(Ui.TRACK);
            g2.fillRoundRect(0, 0, getWidth(), h, h, h);
            // The bar fills in from empty each time the page opens.
            float shown = filled * Motion.easeOut(Motion.progress(createdAt, 0.9f));
            g2.setColor(Ui.ACCENT);
            g2.fillRoundRect(0, 0, Math.round(getWidth() * shown), h, h, h);
            g2.dispose();
            if (shown < filled) {
                repaint();
            }
        }
    }

    /** Lets the card grid stretch to the scroll pane's width instead of keeping its preferred width. */
    private static final class WidthTracking extends JPanel implements Scrollable {
        private static final long serialVersionUID = 1L;

        WidthTracking() {
            super(new BorderLayout());
            setOpaque(false);
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visible, int orientation, int direction) {
            return 16;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visible, int orientation, int direction) {
            return visible.height;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }
}
