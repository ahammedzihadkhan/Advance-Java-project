package cinema.ui;

import cinema.model.Hall;
import cinema.model.Show;
import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.border.EmptyBorder;

/** Main window: a glass sidebar and pages that fade and slide in when they open. */
public final class MainFrame extends JFrame {
    private static final long serialVersionUID = 1L;
    private static final String SEATS = "Seats";
    private static final float PAGE_SECONDS = 0.32f;

    interface Page {
        void refresh();
    }

    private final CardLayout cards = new CardLayout();
    private long pageShownAt;
    private final JPanel content = new JPanel(cards) {
        private static final long serialVersionUID = 1L;

        @Override
        protected void paintChildren(Graphics g) {
            float t = Motion.easeOut(Motion.progress(pageShownAt, PAGE_SECONDS));
            if (t >= 1f) {
                super.paintChildren(g);
                return;
            }
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setComposite(AlphaComposite.SrcOver.derive(t));
            g2.translate(0, Math.round((1f - t) * 18));
            super.paintChildren(g2);
            g2.dispose();
            repaint();
        }
    };
    private transient JToggleButton current;
    private float highlightY = -1;
    private float highlightFrom = -1;
    private long highlightMovedAt;
    private final Glass sidebar = new Glass(null) {
        private static final long serialVersionUID = 1L;

        // The selected-page highlight glides from the old menu item to the new one.
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (current == null) {
                return;
            }
            float target = current.getY();
            float t = Motion.easeOut(Motion.progress(highlightMovedAt, 0.28f));
            highlightY = highlightFrom < 0 ? target : highlightFrom + (target - highlightFrom) * t;
            int y = Math.round(highlightY);
            int h = current.getHeight();
            Graphics2D g2 = Ui.smooth(g);
            g2.setColor(new Color(255, 255, 255, 30));
            g2.fillRoundRect(current.getX(), y, current.getWidth(), h, 14, 14);
            g2.setColor(Ui.ACCENT);
            g2.fillRoundRect(current.getX() + 8, y + h / 2 - 3, 6, 6, 6, 6);
            g2.dispose();
            if (t < 1f) {
                repaint();
            }
        }
    };
    private final ButtonGroup navGroup = new ButtonGroup();
    private final transient Map<String, Page> pages = new LinkedHashMap<>();
    private final transient Map<String, JToggleButton> nav = new LinkedHashMap<>();
    private final BookingPanel seats = new BookingPanel(() -> open("Now Showing"));

    public MainFrame(String username) {
        super("Matinee - Cinema Box Office");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        content.setOpaque(false);

        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBorder(new EmptyBorder(26, 14, 18, 14));
        sidebar.setPreferredSize(new Dimension(224, 0));
        JLabel mark = Ui.label("Matinee", Ui.sans(Font.BOLD, 26f), Ui.TEXT);
        JLabel tag = Ui.caps("Box office");
        for (JComponent c : new JComponent[] {tag, mark}) {
            c.setAlignmentX(Component.LEFT_ALIGNMENT);
            c.setBorder(new EmptyBorder(0, 18, 0, 0));
        }
        sidebar.add(tag);
        sidebar.add(mark);
        sidebar.add(Box.createVerticalStrut(26));

        addPage("Now Showing", new NowShowingPanel(this::openSeats));
        addPage("Movies", new MoviesPanel());
        addPage("Shows", new ShowsPanel());
        addPage("Bookings", new BookingsPanel());
        content.add(seats, SEATS);

        sidebar.add(Box.createVerticalGlue());
        JLabel hall = Ui.label("Hall 1 · " + Hall.CAPACITY + " seats", Ui.sans(Font.PLAIN, 12.5f), Ui.MUTED);
        JLabel user = Ui.label("Signed in as " + username, Ui.sans(Font.PLAIN, 12.5f), Ui.MUTED);
        for (JComponent c : new JComponent[] {hall, user}) {
            c.setAlignmentX(Component.LEFT_ALIGNMENT);
            c.setBorder(new EmptyBorder(0, 18, 6, 0));
        }
        sidebar.add(hall);
        sidebar.add(user);
        JButton logout = Ui.button("Log out", Ui.Style.GHOST);
        logout.setAlignmentX(Component.LEFT_ALIGNMENT);
        logout.setMaximumSize(new Dimension(Integer.MAX_VALUE, logout.getPreferredSize().height));
        logout.addActionListener(e -> {
            new LoginFrame().setVisible(true);
            dispose();
        });
        sidebar.add(Box.createVerticalStrut(8));
        sidebar.add(logout);

        Backdrop root = new Backdrop(new BorderLayout(18, 0));
        root.setBorder(new EmptyBorder(18, 18, 18, 22));
        root.add(sidebar, BorderLayout.WEST);
        root.add(content, BorderLayout.CENTER);
        setContentPane(root);
        setMinimumSize(new Dimension(1120, 720));
        setSize(1260, 800);
        setLocationRelativeTo(null);
        open("Now Showing");
    }

    private <P extends JPanel & Page> void addPage(String name, P page) {
        page.setOpaque(false);
        pages.put(name, page);
        content.add(page, name);
        JToggleButton button = Ui.navButton(name);
        button.addActionListener(e -> open(name));
        navGroup.add(button);
        nav.put(name, button);
        sidebar.add(button);
        sidebar.add(Box.createVerticalStrut(4));
    }

    private void open(String name) {
        pages.get(name).refresh();
        show(name);
        nav.get(name).setSelected(true);
        highlightFrom = highlightY;
        highlightMovedAt = System.nanoTime();
        current = nav.get(name);
        sidebar.repaint();
    }

    private void openSeats(Show show) {
        seats.load(show);
        show(SEATS);
    }

    private void show(String card) {
        pageShownAt = System.nanoTime();
        cards.show(content, card);
        content.repaint();
    }
}
