package cinema.ui;

import cinema.model.Show;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RadialGradientPaint;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.util.List;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

/** The "booking confirmed" screen: a glass ticket with an animated check mark, instead of a plain message box. */
final class TicketDialog extends JDialog {
    private static final long serialVersionUID = 1L;

    private TicketDialog(Window owner, int id, Show show, String customer, List<String> seats) {
        super(owner, "Booking confirmed", ModalityType.APPLICATION_MODAL);
        setUndecorated(true);

        JLabel heading = Ui.label("Booking confirmed", Ui.sans(Font.BOLD, 24f), Ui.TEXT);
        JLabel number = Ui.label("Booking #" + id + "  ·  show this number at the door",
            Ui.sans(Font.PLAIN, 13f), Ui.MUTED);

        JPanel details = Ui.clear(new GridBagLayout());
        row(details, 0, "Movie", show.title);
        row(details, 1, "Showtime", show.startsAt.format(Ui.WHEN));
        row(details, 2, "Seats", String.join("  ", seats));
        row(details, 3, "Customer", customer);

        JPanel amount = Ui.clear(null);
        amount.setLayout(new BoxLayout(amount, BoxLayout.Y_AXIS));
        amount.add(Ui.caps("Total"));
        amount.add(Ui.label(seats.size() + (seats.size() == 1 ? " seat" : " seats") + " × " + Ui.money(show.price),
            Ui.sans(Font.PLAIN, 12.5f), Ui.MUTED));
        JPanel total = Ui.clear(new BorderLayout(16, 0));
        total.add(amount, BorderLayout.WEST);
        total.add(Ui.label(Ui.money(show.price * seats.size()), Ui.sans(Font.BOLD, 30f), Ui.TEXT), BorderLayout.EAST);

        JButton done = Ui.button("Done", Ui.Style.PRIMARY);
        done.addActionListener(e -> dispose());

        Glass ticket = new Glass(null);
        ticket.setLayout(new BoxLayout(ticket, BoxLayout.Y_AXIS));
        ticket.setBorder(new EmptyBorder(22, 30, 24, 30));
        // Every child is centred; the wide ones stretch to the full ticket width.
        for (JComponent c : new JComponent[] {heading, number, details, total, done}) {
            c.setAlignmentX(Component.CENTER_ALIGNMENT);
        }
        for (JComponent c : new JComponent[] {details, total, done}) {
            c.setMaximumSize(new Dimension(Integer.MAX_VALUE, c.getPreferredSize().height));
        }
        ticket.add(new Check());
        ticket.add(Box.createVerticalStrut(8));
        ticket.add(heading);
        ticket.add(Box.createVerticalStrut(2));
        ticket.add(number);
        ticket.add(new Perforation());
        ticket.add(details);
        ticket.add(new Perforation());
        ticket.add(total);
        ticket.add(Box.createVerticalStrut(22));
        ticket.add(done);

        Backdrop root = new Backdrop(new BorderLayout());
        root.setBorder(new CompoundBorder(new LineBorder(Ui.LINE), new EmptyBorder(20, 20, 20, 20)));
        root.add(ticket);
        setContentPane(root);
        getRootPane().setDefaultButton(done);
        getRootPane().registerKeyboardAction(e -> dispose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
            JComponent.WHEN_IN_FOCUSED_WINDOW);
        pack();
        setLocationRelativeTo(owner);
    }

    /** Shows the ticket over the given component's window and waits until it is closed. */
    static void show(Component parent, int id, Show show, String customer, List<String> seats) {
        new TicketDialog(SwingUtilities.getWindowAncestor(parent), id, show, customer, seats).setVisible(true);
    }

    private static void row(JPanel grid, int y, String caption, String value) {
        GridBagConstraints g = new GridBagConstraints();
        g.gridy = y;
        g.insets = new Insets(5, 0, 5, 0);
        g.anchor = GridBagConstraints.NORTHWEST;
        g.gridx = 0;
        JLabel label = Ui.caps(caption);
        label.setBorder(new EmptyBorder(3, 0, 0, 18));
        grid.add(label, g);
        g.gridx = 1;
        g.weightx = 1;
        g.fill = GridBagConstraints.HORIZONTAL;
        // HTML lets long titles and seat lists wrap; the value is escaped since it is typed by staff.
        String text = value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        grid.add(Ui.label("<html><div style='width:220px'>" + text + "</div></html>",
            Ui.sans(Font.BOLD, 14.5f), Ui.TEXT), g);
    }

    /** A dashed tear line, like the stub of a paper ticket. */
    private static final class Perforation extends JPanel {
        private static final long serialVersionUID = 1L;

        Perforation() {
            setOpaque(false);
            setPreferredSize(new Dimension(10, 30));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Ui.smooth(g);
            g2.setColor(new Color(255, 255, 255, 60));
            g2.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f,
                new float[] {5f, 6f}, 0f));
            int y = getHeight() / 2;
            g2.drawLine(0, y, getWidth(), y);
            g2.dispose();
        }
    }

    /** An accent disc that grows in, then a check mark that draws itself. */
    private static final class Check extends JPanel {
        private static final long serialVersionUID = 1L;
        private long start;

        Check() {
            setOpaque(false);
            setPreferredSize(new Dimension(96, 96));
            setMaximumSize(new Dimension(96, 96));
            setAlignmentX(Component.CENTER_ALIGNMENT);
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (start == 0) {
                // Let the window's own fade-in get going first.
                start = System.nanoTime() + 150_000_000L;
            }
            float disc = Motion.easeOut(Motion.progress(start, 0.4f));
            float tick = Motion.easeOut(Motion.progress(start + 250_000_000L, 0.35f));
            Graphics2D g2 = Ui.smooth(g);
            float cx = getWidth() / 2f;
            float cy = getHeight() / 2f;

            g2.setPaint(new RadialGradientPaint(new Point2D.Float(cx, cy), 46, new float[] {0.55f, 1f},
                new Color[] {new Color(240, 80, 90, (int) (110 * disc)), new Color(240, 80, 90, 0)}));
            g2.fillRect(0, 0, getWidth(), getHeight());
            float r = 30 * (0.6f + 0.4f * disc);
            g2.setColor(Ui.mix(new Color(240, 80, 90, 0), Ui.ACCENT, disc));
            g2.fill(new Ellipse2D.Float(cx - r, cy - r, 2 * r, 2 * r));

            if (tick > 0f) {
                // Short stroke a->b, then the long stroke b->c, drawn up to the current fraction of the total length.
                float ax = cx - 12;
                float ay = cy + 1;
                float bx = cx - 3;
                float by = cy + 10;
                float qx = cx + 13;
                float qy = cy - 9;
                double first = Math.hypot(bx - ax, by - ay);
                double second = Math.hypot(qx - bx, qy - by);
                double drawn = tick * (first + second);
                Path2D.Float path = new Path2D.Float();
                path.moveTo(ax, ay);
                if (drawn <= first) {
                    float f = (float) (drawn / first);
                    path.lineTo(ax + (bx - ax) * f, ay + (by - ay) * f);
                } else {
                    float f = (float) ((drawn - first) / second);
                    path.lineTo(bx, by);
                    path.lineTo(bx + (qx - bx) * f, by + (qy - by) * f);
                }
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(4.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.draw(path);
            }
            g2.dispose();
            if (tick < 1f) {
                repaint();
            }
        }
    }
}
