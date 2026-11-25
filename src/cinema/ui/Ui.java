package cinema.ui;

import cinema.dao.CinemaException;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.font.TextAttribute;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import javax.swing.AbstractButton;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.JViewport;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

/** The house style: dark animated glass with a coral-red accent. Colours, fonts, buttons, tables and dialogs. */
public final class Ui {
    static final Color TEXT = new Color(0xF6F0EA);
    static final Color MUTED = new Color(0xB8ADA6);
    static final Color ACCENT = new Color(0xF0505A);
    static final Color LINE = new Color(255, 255, 255, 34);
    static final Color TRACK = new Color(255, 255, 255, 26);
    private static final Color ACCENT_LIGHT = new Color(0xFF7880);
    private static final Color ROW_SELECTED = new Color(240, 80, 90, 72);
    private static final Color FIELD_EDGE = new Color(255, 255, 255, 80);

    static final DateTimeFormatter WHEN = DateTimeFormatter.ofPattern("EEE d MMM, h:mm a", Locale.ENGLISH);
    static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);
    static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("EEE d MMM", Locale.ENGLISH);

    enum Style { PRIMARY, GHOST, DANGER }

    private Ui() { }

    /** Dark Nimbus, so dialogs, fields and dropdowns match the glass. */
    public static void install() {
        UIManager.put("control", new Color(0x1C1418));
        UIManager.put("info", new Color(0x1C1418));
        UIManager.put("nimbusBase", new Color(0x3A2A30));
        UIManager.put("nimbusBlueGrey", new Color(0x4A3A40));
        UIManager.put("nimbusLightBackground", new Color(0x1A1216));
        UIManager.put("text", TEXT);
        UIManager.put("nimbusFocus", ACCENT);
        UIManager.put("nimbusSelectionBackground", new Color(0xA8343C));
        UIManager.put("nimbusSelectedText", Color.WHITE);
        UIManager.put("nimbusDisabledText", new Color(0x7E7176));
        UIManager.put("nimbusOrange", ACCENT);
        UIManager.put("defaultFont", new FontUIResource(sans(Font.PLAIN, 13f)));
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ignored) {
            // The default look and feel still works.
        }
    }

    static Font sans(int style, float size) {
        return new Font("Segoe UI", style, 1).deriveFont(size);
    }

    /** Letter-spaced font for small-capitals labels. */
    static Font tracked(Font font, float tracking) {
        Map<TextAttribute, Object> attributes = new HashMap<>();
        attributes.put(TextAttribute.TRACKING, tracking);
        return font.deriveFont(attributes);
    }

    static Graphics2D smooth(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        return g2;
    }

    static Color mix(Color a, Color b, float t) {
        return new Color((int) (a.getRed() + (b.getRed() - a.getRed()) * t),
            (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t),
            (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t),
            (int) (a.getAlpha() + (b.getAlpha() - a.getAlpha()) * t));
    }

    static JLabel label(String text, Font font, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(font);
        label.setForeground(color);
        return label;
    }

    static JLabel caps(String text) {
        return label(text.toUpperCase(Locale.ROOT), tracked(sans(Font.BOLD, 11f), 0.14f), MUTED);
    }

    static JLabel subtitle(String text) {
        return label(text, sans(Font.PLAIN, 14f), MUTED);
    }

    static JPanel clear(LayoutManager layout) {
        JPanel panel = new JPanel(layout);
        panel.setOpaque(false);
        return panel;
    }

    static JPanel header(String title, JLabel subtitle, JComponent... actions) {
        JPanel text = clear(null);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        JLabel heading = label(title, sans(Font.BOLD, 28f), TEXT);
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        text.add(heading);
        text.add(Box.createVerticalStrut(2));
        text.add(subtitle);

        JPanel buttons = clear(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        for (JComponent action : actions) {
            buttons.add(action);
        }
        JPanel east = clear(new GridBagLayout());
        east.add(buttons);

        JPanel row = clear(new BorderLayout(16, 0));
        row.setBorder(new EmptyBorder(4, 6, 0, 6));
        row.add(text, BorderLayout.CENTER);
        row.add(east, BorderLayout.EAST);
        return row;
    }

    static JButton button(String text, Style style) {
        JButton button = new JButton(text) {
            private static final long serialVersionUID = 1L;
            private final transient Motion.Hover hover = Motion.Hover.attach(this);

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = smooth(g);
                int w = getWidth();
                int h = getHeight();
                float lit = hover.level();
                Color fill;
                Color edge = null;
                if (style == Style.PRIMARY) {
                    fill = isEnabled() ? mix(ACCENT, ACCENT_LIGHT, lit) : new Color(255, 255, 255, 22);
                } else {
                    fill = new Color(255, 255, 255, (int) (14 + 26 * lit));
                    edge = style == Style.DANGER && isEnabled()
                        ? new Color(240, 80, 90, (int) (150 + 105 * lit))
                        : new Color(255, 255, 255, (int) (60 + 70 * lit));
                }
                if (getModel().isPressed()) {
                    fill = fill.darker();
                }
                g2.setColor(fill);
                g2.fillRoundRect(0, 0, w, h, h, h);
                if (edge != null) {
                    g2.setColor(edge);
                    g2.drawRoundRect(0, 0, w - 1, h - 1, h, h);
                }
                if (isFocusOwner()) {
                    g2.setStroke(new BasicStroke(2f));
                    g2.setColor(style == Style.PRIMARY ? Color.WHITE : ACCENT);
                    g2.drawRoundRect(2, 2, w - 5, h - 5, h - 4, h - 4);
                }
                g2.dispose();
                Color text = style == Style.DANGER ? ACCENT_LIGHT : Color.WHITE;
                setForeground(isEnabled() ? text : new Color(255, 255, 255, 90));
                super.paintComponent(g);
            }
        };
        button.setForeground(style == Style.DANGER ? ACCENT_LIGHT : Color.WHITE);
        button.setFont(sans(Font.BOLD, 13f));
        plain(button);
        button.setBorder(new EmptyBorder(9, 20, 9, 20));
        return button;
    }

    static JToggleButton navButton(String text) {
        JToggleButton button = new JToggleButton(text) {
            private static final long serialVersionUID = 1L;
            private final transient Motion.Hover hover = Motion.Hover.attach(this);

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = smooth(g);
                int w = getWidth();
                int h = getHeight();
                float lit = hover.level();
                // The selected highlight is drawn by the sidebar, so it can slide between items.
                if (lit > 0f && !isSelected()) {
                    g2.setColor(new Color(255, 255, 255, (int) (16 * lit)));
                    g2.fillRoundRect(0, 0, w, h, 14, 14);
                }
                if (isFocusOwner()) {
                    g2.setColor(FIELD_EDGE);
                    g2.drawRoundRect(0, 0, w - 1, h - 1, 14, 14);
                }
                g2.dispose();
                setForeground(isSelected() ? TEXT : mix(MUTED, TEXT, lit));
                super.paintComponent(g);
            }
        };
        button.setFont(sans(Font.BOLD, 14f));
        plain(button);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setBorder(new EmptyBorder(10, 24, 10, 12));
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        return button;
    }

    private static void plain(AbstractButton button) {
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setOpaque(false);
        button.setRolloverEnabled(true);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    static DefaultTableModel model(String... columns) {
        return new DefaultTableModel(columns, 0) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            // Reporting the real type makes numbers and dates sort correctly instead of as text.
            @Override
            public Class<?> getColumnClass(int column) {
                for (int row = 0; row < getRowCount(); row++) {
                    Object value = getValueAt(row, column);
                    if (value != null) {
                        return value.getClass();
                    }
                }
                return Object.class;
            }
        };
    }

    /** A see-through table, so the glass behind it shows between the rows. */
    static JTable table(DefaultTableModel model) {
        JTable table = new JTable(model);
        table.setOpaque(false);
        table.setFont(sans(Font.PLAIN, 13.5f));
        table.setRowHeight(38);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setGridColor(LINE);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setAutoCreateRowSorter(true);
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        CellRenderer cells = new CellRenderer();
        table.setDefaultRenderer(Object.class, cells);
        table.setDefaultRenderer(Number.class, cells);
        table.getTableHeader().setOpaque(false);
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setDefaultRenderer((t, value, selected, focus, row, column) -> {
            JLabel label = label(String.valueOf(value).toUpperCase(Locale.ROOT),
                tracked(sans(Font.BOLD, 11f), 0.1f), MUTED);
            label.setBorder(new CompoundBorder(new MatteBorder(0, 0, 1, 0, LINE), new EmptyBorder(10, 14, 10, 14)));
            return label;
        });
        return table;
    }

    static JScrollPane scroll(JComponent view) {
        JScrollPane scroll = new JScrollPane(view);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(new EmptyBorder(0, 0, 0, 0));
        scroll.setViewportBorder(null);
        // Content scrolls over moving glass, so copying old pixels would drag the background along.
        scroll.getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getVerticalScrollBar().setOpaque(false);
        scroll.getVerticalScrollBar().setPreferredSize(new Dimension(12, 0));
        scroll.getVerticalScrollBar().setUI(new ThinScrollBar());
        if (view instanceof JTable) {
            scroll.setColumnHeaderView(((JTable) view).getTableHeader());
            scroll.getColumnHeader().setOpaque(false);
            scroll.setCorner(ScrollPaneConstants.UPPER_RIGHT_CORNER, clear(null));
        }
        return scroll;
    }

    static void columnWidths(JTable table, int... widths) {
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
    }

    static void onChange(JTextField field, Runnable action) {
        field.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                action.run();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                action.run();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                action.run();
            }
        });
    }

    /** Text field with a thin light border that turns accent-red while it has focus. */
    static <T extends JTextField> T flat(T field) {
        EmptyBorder padding = new EmptyBorder(6, 8, 6, 8);
        CompoundBorder idle = new CompoundBorder(new LineBorder(FIELD_EDGE), padding);
        CompoundBorder focused = new CompoundBorder(new LineBorder(ACCENT), padding);
        field.setBorder(idle);
        field.setCaretColor(TEXT);
        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                field.setBorder(focused);
            }

            @Override
            public void focusLost(FocusEvent e) {
                field.setBorder(idle);
            }
        });
        return field;
    }

    static JPanel form(String[] labels, JComponent[] fields) {
        JPanel panel = clear(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(5, 4, 5, 4);
        g.fill = GridBagConstraints.HORIZONTAL;
        for (int i = 0; i < labels.length; i++) {
            JLabel label = label(labels[i], sans(Font.PLAIN, 13f), MUTED);
            label.setLabelFor(fields[i]);
            if (fields[i] instanceof JTextField) {
                flat((JTextField) fields[i]);
            }
            g.gridy = i;
            g.gridx = 0;
            g.weightx = 0;
            panel.add(label, g);
            g.gridx = 1;
            g.weightx = 1;
            panel.add(fields[i], g);
        }
        return panel;
    }

    static String money(int amount) {
        return String.format(Locale.ENGLISH, "%,d", amount);
    }

    static String runtime(int minutes) {
        return minutes / 60 + "h " + minutes % 60 + "m";
    }

    static void info(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Matinee", JOptionPane.INFORMATION_MESSAGE);
    }

    static void warn(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Check this", JOptionPane.WARNING_MESSAGE);
    }

    static void error(Component parent, Exception e) {
        JOptionPane.showMessageDialog(parent, message(e), "Could not complete", JOptionPane.ERROR_MESSAGE);
    }

    private static String message(Exception e) {
        if (e instanceof CinemaException || !(e instanceof SQLException)) {
            return e.getMessage();
        }
        String state = ((SQLException) e).getSQLState();
        if ("23503".equals(state)) {
            return "This is still used by shows or bookings, so it can't be deleted.";
        }
        if ("23505".equals(state)) {
            return "That value is already in use.";
        }
        if ("23513".equals(state)) {
            return "One of the values is outside the allowed range.";
        }
        if ("22001".equals(state)) {
            return "One of the values is too long.";
        }
        return "Database error: " + e.getMessage();
    }

    /** A slim translucent scrollbar without arrow buttons. */
    private static final class ThinScrollBar extends BasicScrollBarUI {
        @Override
        protected JButton createDecreaseButton(int orientation) {
            return noButton();
        }

        @Override
        protected JButton createIncreaseButton(int orientation) {
            return noButton();
        }

        @Override
        protected void paintTrack(Graphics g, JComponent c, Rectangle bounds) {
            // No track: the glass shows through.
        }

        @Override
        protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
            if (r.isEmpty()) {
                return;
            }
            Graphics2D g2 = smooth(g);
            g2.setColor(new Color(255, 255, 255, isThumbRollover() ? 120 : 60));
            g2.fillRoundRect(r.x + 3, r.y + 2, r.width - 6, r.height - 4, 6, 6);
            g2.dispose();
        }

        private static JButton noButton() {
            JButton b = new JButton();
            b.setPreferredSize(new Dimension(0, 0));
            b.setMinimumSize(new Dimension(0, 0));
            b.setMaximumSize(new Dimension(0, 0));
            return b;
        }
    }

    private static final class CellRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                                                       boolean focus, int row, int column) {
            super.getTableCellRendererComponent(table, value, selected, false, row, column);
            setOpaque(selected);
            setBackground(ROW_SELECTED);
            setForeground(selected ? Color.WHITE : TEXT);
            setFont(table.getFont());
            setBorder(new EmptyBorder(0, 14, 0, 14));
            setHorizontalAlignment(value instanceof Number ? SwingConstants.RIGHT : SwingConstants.LEFT);
            return this;
        }

        @Override
        protected void setValue(Object value) {
            super.setValue(value instanceof LocalDateTime ? ((LocalDateTime) value).format(WHEN) : value);
        }
    }
}
