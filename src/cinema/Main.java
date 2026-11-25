package cinema;

import cinema.db.Database;
import cinema.db.Seed;
import cinema.ui.LoginFrame;
import cinema.ui.Ui;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public final class Main {
    private Main() { }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Ui.install();
            try {
                Database.init();
                Seed.run();
                new LoginFrame().setVisible(true);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null,
                    "Could not open the database. Is the app already running?\n" + e.getMessage(),
                    "Startup error", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        });
    }
}
