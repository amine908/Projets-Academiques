import javax.swing.*;
import java.awt.*;
import java.sql.*;

class UserSignUp {
    private JFrame frame;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private String databaseUrl; // Database URL
    private String serverIp; // Server IP
    private int serverPort; // Server Port

    public UserSignUp(String databaseUrl, String serverIp, int serverPort) {
        this.databaseUrl = databaseUrl; // Store the database URL
        this.serverIp = serverIp; // Store the server IP
        this.serverPort = serverPort; // Store the server port
    }

    public void display() {
        frame = new JFrame("User Sign Up");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 350);
        frame.setLayout(new BorderLayout());

        frame.getContentPane().setBackground(new Color(173, 216, 230)); // Light blue color

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel titleLabel = new JLabel("Sign Up for File Transfer");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel usernameLabel = new JLabel("Username:");
        usernameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        usernameField = new JTextField(20);
        usernameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, usernameField.getPreferredSize().height));

        JLabel passwordLabel = new JLabel("Password:");
        passwordLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        passwordField = new JPasswordField(20);
        passwordField.setMaximumSize(new Dimension(Integer.MAX_VALUE, passwordField.getPreferredSize().height));

        JButton submitButton = createStyledButton("Sign Up");
        submitButton.addActionListener(e -> {
            String username = usernameField.getText();
            String password = new String(passwordField.getPassword());
            if (registerUser(username, password)) {
                frame.dispose();
                new UserLogin(serverIp, serverPort, databaseUrl).display(); // Redirect to login after successful signup
            } else {
                JOptionPane.showMessageDialog(frame, "Sign-up failed!", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        JPanel formPanel = new JPanel();
        formPanel.setLayout(new GridLayout(4, 1, 10, 10));
        formPanel.add(usernameLabel);
        formPanel.add(usernameField);
        formPanel.add(passwordLabel);
        formPanel.add(passwordField);

        panel.add(titleLabel);
        panel.add(formPanel);
        panel.add(submitButton);

        frame.add(panel, BorderLayout.CENTER);
        frame.setVisible(true);
    }

    private boolean registerUser(String username, String password) {
        try (Connection conn = DriverManager.getConnection(databaseUrl, "root", "Md1SuL81fahq1BP19Zau");
             PreparedStatement ps = conn.prepareStatement("INSERT INTO users (username, password) VALUES (?, ?)")) {
            ps.setString(1, username);
            ps.setString(2, password);
            ps.executeUpdate();
            return true; // Registration successful
        } catch (Exception ex) {
            ex.printStackTrace();
            return false; // Return false if there's an error
        }
    }

    private JButton createStyledButton(String text) {
        JButton button = new JButton(text);
        button.setBackground(new Color(70, 130, 180)); // Steel Blue
        button.setForeground(Color.WHITE);
        button.setFont(new Font("Arial", Font.BOLD, 14));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        return button;
    }
}
