package cinema.ui;

import cinema.model.Hall;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.QuadCurve2D;
import java.awt.geom.RoundRectangle2D;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import javax.swing.AbstractAction;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.UIManager;

/** The hall drawn from above. Click seats, or use the arrow keys and Space. Picked seats pop in. */
final class SeatMap extends JPanel {
    private static final long serialVersionUID = 1L;
    private static final int FREE = 0;
    private static final int SELECTED = 1;
    private static final int SOLD = 2;
    private static final int SEAT = 34;
    private static final int GAP = 7;
    private static final int AISLE = 28;
    private static final int SIDE = 30;
    private static final int TOP = 78;
    private static final int LEGEND = 60;
    private static final float POP_SECONDS = 0.28f;

    private final transient Runnable onChange;
    private final transient Set<String> selected = new TreeSet<>(Hall.SEAT_ORDER);
    private final transient Map<String, Long> pickedAt = new HashMap<>();
    private transient Set<String> sold = new HashSet<>();
    private String hover;
    private int cursorRow = 4;
    private int cursorCol = 5;

    SeatMap(Runnable onChange) {
        this.onChange = onChange;
        setOpaque(false);
        setFocusable(true);
        setToolTipText("");
        getAccessibleContext().setAccessibleName("Seat map. Use the arrow keys to move and Space to pick a seat.");

        MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                String seat = seatAt(e.getPoint());
                if (!Objects.equals(seat, hover)) {
                    hover = seat;
                    repaint();
                }
                setCursor(Cursor.getPredefinedCursor(seat != null && !sold.contains(seat)
                    ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hover = null;
                repaint();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
                String seat = seatAt(e.getPoint());
                if (seat != null) {
                    cursorRow = Hall.ROWS.indexOf(seat.charAt(0));
                    cursorCol = Integer.parseInt(seat.substring(1)) - 1;
                    toggle(seat);
                }
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
        addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                repaint();
            }

            @Override
            public void focusLost(FocusEvent e) {
                repaint();
            }
        });
        bind("LEFT", () -> moveCursor(0, -1));
        bind("RIGHT", () -> moveCursor(0, 1));
        bind("UP", () -> moveCursor(-1, 0));
        bind("DOWN", () -> moveCursor(1, 0));
        bind("SPACE", () -> toggle(Hall.seat(cursorRow, cursorCol + 1)));
        bind("ENTER", () -> toggle(Hall.seat(cursorRow, cursorCol + 1)));
    }

    void load(Set<String> soldSeats) {
        sold = soldSeats;
        selected.clear();
        pickedAt.clear();
        repaint();
        onChange.run();
    }

    Set<String> selected() {
        return Collections.unmodifiableSet(selected);
    }

    @Override
    public Dimension getPreferredSize() {
        int width = SIDE * 2 + Hall.SEATS_PER_ROW * SEAT + (Hall.SEATS_PER_ROW - 1) * GAP + AISLE;
        int height = TOP + Hall.ROWS.length() * (SEAT + GAP) + LEGEND;
        return new Dimension(width, height);
    }

    @Override
    public String getToolTipText(MouseEvent e) {
        String seat = seatAt(e.getPoint());
        if (seat == null) {
            return null;
        }
        return "Seat " + seat + (sold.contains(seat) ? " - sold" : selected.contains(seat) ? " - selected" : " - free");
    }

    private void bind(String key, Runnable action) {
        getInputMap(WHEN_FOCUSED).put(KeyStroke.getKeyStroke(key), key);
        getActionMap().put(key, new AbstractAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                action.run();
            }
        });
    }

    private void moveCursor(int rows, int cols) {
        cursorRow = Math.max(0, Math.min(Hall.ROWS.length() - 1, cursorRow + rows));
        cursorCol = Math.max(0, Math.min(Hall.SEATS_PER_ROW - 1, cursorCol + cols));
        repaint();
    }

    private void toggle(String seat) {
        if (sold.contains(seat)) {
            UIManager.getLookAndFeel().provideErrorFeedback(this);
            return;
        }
        if (selected.remove(seat)) {
            pickedAt.remove(seat);
        } else {
            if (selected.size() >= Hall.MAX_SEATS_PER_BOOKING) {
                UIManager.getLookAndFeel().provideErrorFeedback(this);
                return;
            }
            selected.add(seat);
            pickedAt.put(seat, System.nanoTime());
        }
        repaint();
        onChange.run();
    }

    private int originX() {
        return Math.max(0, (getWidth() - getPreferredSize().width) / 2);
    }

    private int originY() {
        return Math.max(0, (getHeight() - getPreferredSize().height) / 2);
    }

    private Rectangle seatBounds(int row, int col) {
        int x = originX() + SIDE + col * (SEAT + GAP) + (col >= Hall.SEATS_PER_ROW / 2 ? AISLE : 0);
        int y = originY() + TOP + row * (SEAT + GAP);
        return new Rectangle(x, y, SEAT, SEAT);
    }

    private String seatAt(Point p) {
        for (int row = 0; row < Hall.ROWS.length(); row++) {
            for (int col = 0; col < Hall.SEATS_PER_ROW; col++) {
                if (seatBounds(row, col).contains(p)) {
                    return Hall.seat(row, col + 1);
                }
            }
        }
        return null;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = Ui.smooth(g);
        int oy = originY();
        float left = seatBounds(0, 0).x;
        float right = seatBounds(0, Hall.SEATS_PER_ROW - 1).x + SEAT;

        QuadCurve2D screen = new QuadCurve2D.Float(left, oy + 36, (left + right) / 2, oy + 10, right, oy + 36);
        for (int i = 4; i >= 1; i--) {
            g2.setStroke(new BasicStroke(i * 3.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(new Color(240, 80, 90, 22));
            g2.draw(screen);
        }
        g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(Ui.ACCENT);
        g2.draw(screen);
        g2.setFont(Ui.tracked(Ui.sans(Font.BOLD, 11f), 0.4f));
        g2.setColor(Ui.MUTED);
        centered(g2, "SCREEN", (left + right) / 2, oy + 56);

        boolean animating = false;
        g2.setFont(Ui.sans(Font.BOLD, 12f));
        for (int row = 0; row < Hall.ROWS.length(); row++) {
            Rectangle first = seatBounds(row, 0);
            String letter = String.valueOf(Hall.ROWS.charAt(row));
            g2.setColor(Ui.MUTED);
            centered(g2, letter, first.x - SIDE / 2f, first.y + SEAT / 2f + 4);
            centered(g2, letter, right + SIDE / 2f, first.y + SEAT / 2f + 4);
            for (int col = 0; col < Hall.SEATS_PER_ROW; col++) {
                Rectangle b = seatBounds(row, col);
                String id = Hall.seat(row, col + 1);
                int state = sold.contains(id) ? SOLD : selected.contains(id) ? SELECTED : FREE;
                float pop = state == SELECTED ? Motion.progress(pickedAt.getOrDefault(id, 0L), POP_SECONDS) : 1f;
                animating |= pop < 1f;
                Graphics2D seat = (Graphics2D) g2.create();
                if (pop < 1f) {
                    // Grows from 70% to full size, overshooting slightly, like a button being pressed in.
                    float scale = 0.7f + 0.3f * Motion.easeOut(pop) + 0.08f * (float) Math.sin(pop * Math.PI);
                    seat.translate(b.getCenterX(), b.getCenterY());
                    seat.scale(scale, scale);
                    seat.translate(-b.getCenterX(), -b.getCenterY());
                }
                paintSeat(seat, b.x, b.y, SEAT, state, String.valueOf(col + 1), id.equals(hover));
                seat.dispose();
            }
        }

        if (isFocusOwner()) {
            Rectangle b = seatBounds(cursorRow, cursorCol);
            g2.setStroke(new BasicStroke(2f));
            g2.setColor(Ui.TEXT);
            g2.drawRoundRect(b.x - 4, b.y - 4, b.width + 8, b.height + 8, 12, 12);
        }

        int legendY = seatBounds(Hall.ROWS.length() - 1, 0).y + SEAT + 30;
        int x = (int) ((left + right) / 2) - 150;
        String[] names = {"Free", "Your pick", "Sold"};
        g2.setFont(Ui.sans(Font.PLAIN, 12.5f));
        for (int state = 0; state < names.length; state++) {
            paintSeat(g2, x, legendY - 14, 20, state, null, false);
            g2.setColor(Ui.MUTED);
            g2.drawString(names[state], x + 28, legendY + 1);
            x += 110;
        }
        g2.dispose();
        if (animating) {
            repaint();
        }
    }

    private static void paintSeat(Graphics2D g2, int x, int y, int size, int state, String number, boolean hover) {
        RoundRectangle2D back = new RoundRectangle2D.Float(x, y, size, size * 0.8f, size * 0.36f, size * 0.36f);
        RoundRectangle2D cushion = new RoundRectangle2D.Float(x + 2, y + size * 0.86f, size - 4, size * 0.14f, 4, 4);
        if (state == SELECTED) {
            g2.setColor(Ui.ACCENT);
            g2.fill(back);
            g2.fill(cushion);
        } else if (state == SOLD) {
            g2.setColor(new Color(255, 255, 255, 12));
            g2.fill(back);
            g2.fill(cushion);
            // A cross as well as the colour, so sold seats don't rely on colour alone.
            float m = size * 0.32f;
            g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(new Color(255, 255, 255, 80));
            g2.drawLine((int) (x + m), (int) (y + m * 0.8f), (int) (x + size - m), (int) (y + size * 0.8f - m * 0.8f));
            g2.drawLine((int) (x + size - m), (int) (y + m * 0.8f), (int) (x + m), (int) (y + size * 0.8f - m * 0.8f));
        } else {
            g2.setColor(new Color(255, 255, 255, hover ? 46 : 16));
            g2.fill(back);
            g2.setStroke(new BasicStroke(1.2f));
            g2.setColor(new Color(255, 255, 255, hover ? 200 : 95));
            g2.draw(back);
            g2.setColor(new Color(255, 255, 255, 34));
            g2.fill(cushion);
        }
        if (number != null && state != SOLD) {
            g2.setFont(Ui.sans(Font.BOLD, 11f));
            g2.setColor(state == SELECTED ? Color.WHITE : new Color(255, 255, 255, 175));
            centered(g2, number, x + size / 2f, y + size * 0.4f + 4);
        }
    }

    private static void centered(Graphics2D g2, String text, float cx, float baseline) {
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(text, cx - fm.stringWidth(text) / 2f, baseline);
    }
}
