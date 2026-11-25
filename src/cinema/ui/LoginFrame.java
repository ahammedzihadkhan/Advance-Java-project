package cinema.ui;

import cinema.dao.UserDao;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.sql.SQLException;
import java.util.Arrays;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

public final class LoginFrame extends JFrame {
    private static final long serialVersionUID = 1L;

    private final transient UserDao users = new UserDao();
    private final JTextField username = new JTextField(18);
    private final JPasswordField password = new JPasswordField(18);
    private final JLabel message = Ui.label(" ", Ui.sans(Font.PLAIN, 12.5f), Ui.ACCENT);

    public LoginFrame() {
        super("Matinee - Box office sign in");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JLabel tag = Ui.caps("Cinema box office");
        JLabel mark = Ui.label("Matinee", Ui.sans(Font.BOLD, 36f), Ui.TEXT);
        JLabel hint = Ui.label("Sign in to start selling tickets.", Ui.sans(Font.PLAIN, 13.5f), Ui.MUTED);
        JPanel fields = Ui.form(new String[] {"Username", "Password"}, new JComponent[] {username, password});
        message.setMaximumSize(new Dimension(Integer.MAX_VALUE, message.getPreferredSize().height));

        JButton signIn = Ui.button("Sign in", Ui.Style.PRIMARY);
        signIn.setMaximumSize(new Dimension(Integer.MAX_VALUE, signIn.getPreferredSize().height));
        signIn.addActionListener(e -> login());
        getRootPane().setDefaultButton(signIn);

        Glass card = new Glass(null);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(32, 34, 32, 34));
        for (JComponent c : new JComponent[] {tag, mark, hint, fields, message, signIn}) {
            c.setAlignmentX(Component.LEFT_ALIGNMENT);
        }
        card.add(tag);
        card.add(Box.createVerticalStrut(4));
        card.add(mark);
        card.add(hint);
        card.add(Box.createVerticalStrut(22));
        card.add(fields);
        card.add(Box.createVerticalStrut(4));
        card.add(message);
        card.add(Box.createVerticalStrut(12));
        card.add(signIn);

        Backdrop root = new Backdrop(new GridBagLayout());
        root.add(card);
        setContentPane(root);
        setSize(580, 540);
        setResizable(false);
        setLocationRelativeTo(null);
    }

    private void login() {
        String name = username.getText().trim();
        char[] secret = password.getPassword();
        try {
            if (name.isEmpty() || secret.length == 0) {
                message.setText("Enter your username and password.");
            } else if (users.authenticate(name, secret)) {
                new MainFrame(name).setVisible(true);
                dispose();
            } else {
                message.setText("Wrong username or password.");
                password.setText("");
                password.requestFocusInWindow();
            }
        } catch (SQLException e) {
            Ui.error(this, e);
        } finally {
            Arrays.fill(secret, '\0');
        }
    }
}
