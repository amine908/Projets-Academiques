import javax.swing.*;
import java.awt.*;
import java.sql.*;

class UserLogin {
    private JFrame frame;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private String serverIp;
    private int serverPort;
    private String databaseUrl;

    public UserLogin(String serverIp, int serverPort, String databaseUrl) {
        this.serverIp = serverIp;
        this.serverPort = serverPort;
        this.databaseUrl = databaseUrl; // Store the database URL
    }

    public void display() {
        frame = new JFrame("User Login");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 350);
        frame.setLayout(new BorderLayout());

        frame.getContentPane().setBackground(new Color(173, 216, 230));

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel titleLabel = new JLabel("User Login");
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

        JButton loginButton = createStyledButton("Login");
        loginButton.addActionListener(e -> {
            String username = usernameField.getText();
            String password = new String(passwordField.getPassword());
            if (authenticateUser(username, password)) {
                frame.dispose();
                new UserDashboard(username, databaseUrl, serverIp, serverPort).display(); // Pass username and database URL
            } else {
                JOptionPane.showMessageDialog(frame, "Invalid username or password!", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        JButton signupButton = createStyledButton("Sign Up");
        signupButton.addActionListener(e -> {
            frame.dispose();
            new UserSignUp(databaseUrl, serverIp, serverPort).display(); // Pass database URL and server details
        });

        JPanel formPanel = new JPanel();
        formPanel.setLayout(new GridLayout(4, 1, 10, 10));
        formPanel.add(usernameLabel);
        formPanel.add(usernameField);
        formPanel.add(passwordLabel);
        formPanel.add(passwordField);

        panel.add(titleLabel);
        panel.add(formPanel);
        panel.add(loginButton);
        panel.add(signupButton);

        frame.add(panel, BorderLayout.CENTER);
        frame.setVisible(true);
    }

    private boolean authenticateUser(String username, String password) {
        try (Connection conn = DriverManager.getConnection(databaseUrl, "root", "Md1SuL81fahq1BP19Zau");
             Statement stmt = conn.createStatement()) {
            String query = "SELECT * FROM users WHERE username = '" + username + "' AND password = '" + password + "'";
            ResultSet rs = stmt.executeQuery(query);
            return rs.next(); // Returns true if user exists
        } catch (Exception ex) {
            ex.printStackTrace();
            return false; // Return false if there's an error
        }
    }

    private JButton createStyledButton(String text) {
        JButton button = new JButton(text);
        button.setBackground(new Color(70, 130, 180));
        button.setForeground(Color.WHITE);
        button.setFont(new Font("Arial", Font.BOLD, 14));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        return button;
    }
}
