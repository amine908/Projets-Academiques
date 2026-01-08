import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.*;

public class AdminLogin {
    private JFrame frame;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private Connection connection;

    public AdminLogin() {
        initializeDatabase(); // Initialize the SQLite database
    }

    private void initializeDatabase() {
        try {
            // Connect to SQLite database (it will create the file if it doesn't exist)
            connection = DriverManager.getConnection("jdbc:sqlite:admin.db");
            Statement statement = connection.createStatement();

            // Create the users table if it doesn't exist
            statement.execute("CREATE TABLE IF NOT EXISTS users (username TEXT PRIMARY KEY, password TEXT)");

            // Insert the admin user if it doesn't exist
            statement.execute("INSERT OR IGNORE INTO users (username, password) VALUES ('admin', 'admin')");

            statement.close();
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(frame, "Error initializing database: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void display() {
        frame = new JFrame("Admin Login");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 350); // Increased height to accommodate the message
        frame.setLayout(new BorderLayout());

        // Main content panel
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel titleLabel = new JLabel("Admin Login");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Add a label to inform the user about the default credentials
        JLabel defaultCredentialsLabel = new JLabel("<html><div style='text-align: center;'>Default username: <b>admin</b><br>Default password: <b>admin</b><br>You should change the default password after login.</div></html>");
        defaultCredentialsLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        defaultCredentialsLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        defaultCredentialsLabel.setForeground(Color.RED); // Make the message stand out

        JLabel usernameLabel = new JLabel("Username:");
        usernameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        usernameField = new JTextField("admin", 20); // Set default username to "admin"
        usernameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, usernameField.getPreferredSize().height));

        JLabel passwordLabel = new JLabel("Password:");
        passwordLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        passwordField = new JPasswordField("admin", 20); // Set default password to "admin"
        passwordField.setMaximumSize(new Dimension(Integer.MAX_VALUE, passwordField.getPreferredSize().height));

        JButton loginButton = createStyledButton("Login");
        loginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String username = usernameField.getText();
                String password = new String(passwordField.getPassword());

                if (authenticateAdmin(username, password)) {
                    frame.dispose();
                    new PortSelection().display(); // Proceed to port selection
                } else {
                    JOptionPane.showMessageDialog(frame, "Invalid credentials!", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        JButton changePasswordButton = createStyledButton("Change Password");
        changePasswordButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                changePassword();
            }
        });

        panel.add(titleLabel);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(defaultCredentialsLabel); // Add the default credentials message
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(usernameLabel);
        panel.add(usernameField);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(passwordLabel);
        panel.add(passwordField);
        panel.add(Box.createRigidArea(new Dimension(0, 20)));
        panel.add(loginButton);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(changePasswordButton);

        // Footer
        JLabel footerLabel = new JLabel("Project created by Hamid, Malak, and Saad", SwingConstants.CENTER);
        footerLabel.setFont(new Font("Arial", Font.ITALIC, 12));
        footerLabel.setForeground(Color.DARK_GRAY);

        frame.add(panel, BorderLayout.CENTER);
        frame.add(footerLabel, BorderLayout.SOUTH);
        frame.setVisible(true);
    }

    private boolean authenticateAdmin(String username, String password) {
        try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM users WHERE username = ? AND password = ?")) {
            ps.setString(1, username);
            ps.setString(2, password);
            ResultSet rs = ps.executeQuery();
            return rs.next(); // Returns true if the admin user exists with the given credentials
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void changePassword() {
        String oldPassword = JOptionPane.showInputDialog(frame, "Enter old password:");
        if (oldPassword == null || oldPassword.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Old password cannot be empty.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String newPassword = JOptionPane.showInputDialog(frame, "Enter new password:");
        if (newPassword == null || newPassword.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "New password cannot be empty.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try (PreparedStatement ps = connection.prepareStatement("UPDATE users SET password = ? WHERE username = 'admin' AND password = ?")) {
            ps.setString(1, newPassword);
            ps.setString(2, oldPassword);
            int rowsUpdated = ps.executeUpdate();
            if (rowsUpdated > 0) {
                JOptionPane.showMessageDialog(frame, "Password changed successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(frame, "Failed to change password. Old password is incorrect.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(frame, "Error changing password: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private JButton createStyledButton(String text) {
        JButton button = new JButton(text);
        button.setBackground(new Color(72, 209, 204)); // Light teal
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setFont(new Font("Arial", Font.BOLD, 14));
        button.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        return button;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new AdminLogin().display();
        });
    }
}