package boundary;

import javax.swing.*;
import java.awt.*;

/**
 * Login screen.
 *
 * Requirement:
 * - Any username/password input is accepted (demo validation).
 * - On successful "login", opens the main dashboard.
 */
public class LoginUI extends JFrame {

    public interface LoginCallback {
        void onLoginSuccess();
    }

    private final JTextField usernameField = new JTextField();
    private final JPasswordField passwordField = new JPasswordField();

    public LoginUI(LoginCallback callback) {
        super("ParkWise - Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(420, 220);
        setLocationRelativeTo(null);

        JPanel form = new JPanel(new GridLayout(3, 2, 10, 10));
        form.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        form.add(new JLabel("Username:"));
        form.add(usernameField);

        form.add(new JLabel("Password:"));
        form.add(passwordField);

        JButton loginBtn = new JButton("Login");
        loginBtn.addActionListener(e -> {
            // Any input is allowed (as requested)
            callback.onLoginSuccess();
        });

        form.add(new JLabel());
        form.add(loginBtn);

        setContentPane(form);
    }
}