package cinema.ui;

import cinema.dao.CinemaException;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

/** Search, add, edit and delete screen shared by Movies and Shows. */
abstract class CrudPanel<T> extends JPanel implements MainFrame.Page {
    private static final long serialVersionUID = 1L;

    private final DefaultTableModel model;
    private final JTable table;
    private final JTextField search = Ui.flat(new JTextField(22));
    private transient List<T> rows = new ArrayList<>();

    CrudPanel(String title, String subtitle, String addLabel, String... columns) {
        super(new BorderLayout(0, 18));
        setOpaque(false);
        model = Ui.model(columns);
        table = Ui.table(model);

        JButton add = Ui.button(addLabel, Ui.Style.PRIMARY);
        JButton edit = Ui.button("Edit", Ui.Style.GHOST);
        JButton delete = Ui.button("Delete", Ui.Style.DANGER);
        add.addActionListener(e -> save(null));
        edit.addActionListener(e -> {
            T row = selected();
            if (row != null) {
                save(row);
            }
        });
        delete.addActionListener(e -> delete());
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && table.getSelectedRow() >= 0) {
                    save(rows.get(table.convertRowIndexToModel(table.getSelectedRow())));
                }
            }
        });
        add(Ui.header(title, Ui.subtitle(subtitle), add), BorderLayout.NORTH);

        JLabel searchLabel = new JLabel("Search");
        searchLabel.setLabelFor(search);
        Ui.onChange(search, this::refresh);
        JPanel left = Ui.clear(new FlowLayout(FlowLayout.LEFT, 10, 0));
        left.add(searchLabel);
        left.add(search);
        JPanel right = Ui.clear(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.add(edit);
        right.add(delete);
        JPanel bar = Ui.clear(new BorderLayout());
        bar.add(left, BorderLayout.WEST);
        bar.add(right, BorderLayout.EAST);

        Glass body = new Glass(new BorderLayout(0, 14));
        body.add(bar, BorderLayout.NORTH);
        body.add(Ui.scroll(table), BorderLayout.CENTER);
        add(body, BorderLayout.CENTER);
    }

    final void columnWidths(int... widths) {
        Ui.columnWidths(table, widths);
    }

    abstract List<T> load(String query) throws SQLException;

    abstract Object[] toRow(T item);

    /** Shows the form and returns the validated values, or null if cancelled. */
    abstract T showForm(T initial, String action);

    abstract void insert(T item) throws SQLException, CinemaException;

    abstract void update(T item) throws SQLException, CinemaException;

    abstract void remove(T item) throws SQLException;

    @Override
    public void refresh() {
        try {
            rows = load(search.getText());
            model.setRowCount(0);
            for (T row : rows) {
                model.addRow(toRow(row));
            }
        } catch (SQLException e) {
            Ui.error(this, e);
        }
    }

    private T selected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            Ui.warn(this, "Select a row first.");
            return null;
        }
        return rows.get(table.convertRowIndexToModel(row));
    }

    // Reopens the form after an error so the user keeps what they typed.
    private void save(T existing) {
        String action = existing == null ? "Add" : "Edit";
        T draft = existing;
        while ((draft = showForm(draft, action)) != null) {
            try {
                if (existing == null) {
                    insert(draft);
                } else {
                    update(draft);
                }
                refresh();
                return;
            } catch (SQLException | CinemaException e) {
                Ui.error(this, e);
            }
        }
    }

    private void delete() {
        T row = selected();
        if (row == null) {
            return;
        }
        int answer = JOptionPane.showConfirmDialog(this, "Delete " + row + "?", "Confirm delete",
            JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (answer != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            remove(row);
            refresh();
        } catch (SQLException e) {
            Ui.error(this, e);
        }
    }
}
