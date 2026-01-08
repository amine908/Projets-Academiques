import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class GestionAbsencesENSA {

    private static final String DB_URL = "jdbc:sqlite:ensa_tetouan.db";

    public static void main(String[] args) {
        initializeDatabase();
        SwingUtilities.invokeLater(LoginFrame::new); // Launch the login frame
    }

    private static void initializeDatabase() {
        try (Connection conn = connect();
             Statement stmt = conn.createStatement()) {

            // Create tables if they do not exist
            String createAdminsTable = "CREATE TABLE IF NOT EXISTS admins (" +
                    "username TEXT PRIMARY KEY, " +
                    "password TEXT NOT NULL" +
                    ");";
            stmt.execute(createAdminsTable);

            String createStudentsTable = "CREATE TABLE IF NOT EXISTS students (" +
                    "code_apogee TEXT PRIMARY KEY, " +
                    "nom TEXT, " +
                    "prenom TEXT, " +
                    "cycle TEXT, " +
                    "filiere TEXT, " +
                    "niveau TEXT, " +
                    "sexe TEXT, " +  // Added sexe column
                    "age INTEGER, " + // Added age column
                    "absence_count INTEGER DEFAULT 0, " +
                    "absence_excused INTEGER DEFAULT 0" +
                    ");";
            stmt.execute(createStudentsTable);

            String createAbsencesTable = "CREATE TABLE IF NOT EXISTS absences (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "code_apogee TEXT, " +
                    "absence_date TEXT, " +
                    "status TEXT, " +
                    "sessions_count INTEGER, " + // Added sessions_count for tracking sessions
                    "FOREIGN KEY(code_apogee) REFERENCES students(code_apogee)" +
                    ");";
            stmt.execute(createAbsencesTable);

            // Insert default admin if it does not already exist
            String insertAdmin = "INSERT INTO admins (username, password) " +
                    "SELECT 'admin', 'admin' WHERE NOT EXISTS (SELECT 1 FROM admins WHERE username = 'admin');";
            stmt.execute(insertAdmin);

            System.out.println("Base de données initialisée avec succès.");
        } catch (SQLException e) {
            System.err.println("Erreur d'initialisation de la base de données: " + e.getMessage());
        }
    }

    private static Connection connect() {
        try {
            return DriverManager.getConnection(DB_URL);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Connexion à la base de données échouée: " + e.getMessage());
            return null;
        }
    }

    static class LoginFrame extends JFrame {
        private JTextField usernameField;
        private JPasswordField passwordField;

        public LoginFrame() {
            setTitle("Connexion");
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setLayout(new GridLayout(4, 2, 10, 10));
            getContentPane().setBackground(new Color(240, 248, 255)); // Light background

            // Title Label with Blue color
            JLabel titleLabel = new JLabel("Connexion", SwingConstants.CENTER);
            titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
            titleLabel.setForeground(new Color(0, 119, 181)); // LinkedIn blue
            add(titleLabel);

            add(new JLabel("Nom d'utilisateur :"));
            usernameField = new JTextField();
            add(usernameField);

            add(new JLabel("Mot de passe :"));
            passwordField = new JPasswordField();
            add(passwordField);

            // Styled buttons
            JButton loginButton = new JButton("Se connecter");
            customizeButton(loginButton);
            loginButton.addActionListener(e -> authenticate());
            add(loginButton);

            JButton changePasswordButton = new JButton("Changer le mot de passe");
            customizeButton(changePasswordButton);
            changePasswordButton.addActionListener(e -> new ChangePasswordFrame());
            add(changePasswordButton);

            JButton forgetPasswordButton = new JButton("Mot de passe oublié");
            customizeButton(forgetPasswordButton);
            forgetPasswordButton.addActionListener(e -> new ForgetPasswordFrame());
            add(forgetPasswordButton);

            setSize(350, 250);
            setLocationRelativeTo(null);
            setVisible(true);
        }

        private void customizeButton(JButton button) {
            button.setBackground(new Color(0, 119, 181)); // LinkedIn blue
            button.setForeground(Color.WHITE);
            button.setFocusPainted(false);
            button.setFont(new Font("Arial", Font.BOLD, 14));
            button.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        }

        private void authenticate() {
            String username = usernameField.getText();
            String password = new String(passwordField.getPassword());

            try (Connection conn = connect();
                 PreparedStatement stmt = conn.prepareStatement("SELECT * FROM admins WHERE username = ? AND password = ?")) {

                stmt.setString(1, username);
                stmt.setString(2, password);
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    JOptionPane.showMessageDialog(this, "Connexion réussie !");
                    new DashboardFrame(); // Launch the dashboard frame
                    dispose();
                } else {
                    JOptionPane.showMessageDialog(this, "Identifiants incorrects.");
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Erreur de la base de données : " + ex.getMessage());
            }
        }
    }

    static class DashboardFrame extends JFrame {
        public DashboardFrame() {
            setTitle("Tableau de bord");
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setLayout(new BorderLayout());

            JPanel menuPanel = new JPanel(new GridLayout(5, 1));
            menuPanel.setBackground(new Color(70, 130, 180)); // Blue background

            // Buttons with styling
            JButton studentButton = new JButton("Gestion des étudiants");
            customizeMenuButton(studentButton);
            studentButton.addActionListener(e -> new StudentPanel()); // Assuming StudentPanel is implemented

            JButton attendanceButton = new JButton("Gestion des absences");
            customizeMenuButton(attendanceButton);
            attendanceButton.addActionListener(e -> new AttendancePanel()); // Assuming AttendancePanel is implemented

            JButton logoutButton = new JButton("Se déconnecter");
            customizeMenuButton(logoutButton);
            logoutButton.addActionListener(e -> {
                new LoginFrame();
                dispose();
            });

            menuPanel.add(studentButton);
            menuPanel.add(attendanceButton);
            menuPanel.add(logoutButton);

            add(menuPanel, BorderLayout.WEST);

            // Add the image to the center of the dashboard
            ImageIcon icon = new ImageIcon("resources/images.jpeg");
            JLabel imageLabel = new JLabel(icon);
            imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
            imageLabel.setPreferredSize(new Dimension(200, 100)); // Set a preferred size for the image
            add(imageLabel, BorderLayout.CENTER);

            // Add project credits at the bottom of the dashboard
            JPanel creditsPanel = new JPanel();
            creditsPanel.setBackground(new Color(245, 245, 245)); // Light gray background
            JLabel creditsLabel = new JLabel("Projet créé par Amine Belamine, Ihrouchen Myriam et Nour Naitalouane");
            creditsLabel.setFont(new Font("Arial", Font.ITALIC, 12));
            creditsLabel.setForeground(new Color(0, 119, 181)); // LinkedIn blue
            creditsPanel.add(creditsLabel);
            add(creditsPanel, BorderLayout.SOUTH);

            setSize(600, 400);
            setLocationRelativeTo(null);
            setVisible(true);
        }

        private void customizeMenuButton(JButton button) {
            button.setBackground(new Color(0, 119, 181)); // LinkedIn blue
            button.setForeground(Color.WHITE);
            button.setFont(new Font("Arial", Font.BOLD, 14));
            button.setFocusPainted(false);
            button.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        }
    }

    static class ChangePasswordFrame extends JFrame {
        private JPasswordField oldPasswordField;
        private JPasswordField newPasswordField;

        public ChangePasswordFrame() {
            setTitle("Changer le mot de passe");
            setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            setLayout(new GridLayout(3, 2, 10, 10));

            getContentPane().setBackground(new Color(240, 248, 255)); // Light background

            add(new JLabel("Ancien mot de passe :"));
            oldPasswordField = new JPasswordField();
            add(oldPasswordField);

            add(new JLabel("Nouveau mot de passe :"));
            newPasswordField = new JPasswordField();
            add(newPasswordField);

            JButton changeButton = new JButton("Changer le mot de passe");
            customizeButton(changeButton);
            changeButton.addActionListener(e -> changePassword());
            add(changeButton);

            setSize(350, 200);
            setLocationRelativeTo(null);
            setVisible(true);
        }

        private void customizeButton(JButton button) {
            button.setBackground(new Color(0, 119, 181)); // LinkedIn blue
            button.setForeground(Color.WHITE);
            button.setFocusPainted(false);
            button.setFont(new Font("Arial", Font.BOLD, 14));
            button.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        }

        private void changePassword() {
            String oldPassword = new String(oldPasswordField.getPassword());
            String newPassword = new String(newPasswordField.getPassword());

            if (oldPassword.isEmpty() || newPassword.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Les champs ne peuvent pas être vides.");
                return;
            }

            try (Connection conn = connect();
                 PreparedStatement stmt = conn.prepareStatement("SELECT password FROM admins WHERE username = 'admin'")) {

                ResultSet rs = stmt.executeQuery();
                if (rs.next() && rs.getString("password").equals(oldPassword)) {
                    PreparedStatement updateStmt = conn.prepareStatement("UPDATE admins SET password = ? WHERE username = 'admin'");
                    updateStmt.setString(1, newPassword);
                    updateStmt.executeUpdate();
                    JOptionPane.showMessageDialog(this, "Mot de passe changé avec succès.");
                    dispose();
                } else {
                    JOptionPane.showMessageDialog(this, "L'ancien mot de passe est incorrect.");
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Erreur de la base de données : " + ex.getMessage());
            }
        }
    }

    static class ForgetPasswordFrame extends JFrame {
        private JTextField answer1Field;
        private JTextField answer2Field;
        private JTextField answer3Field;

        public ForgetPasswordFrame() {
            setTitle("Réinitialisation du mot de passe");
            setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            setLayout(new GridLayout(4, 2, 10, 10));

            getContentPane().setBackground(new Color(240, 248, 255)); // Light background

            add(new JLabel("Quand ENSA Tetouan a-t-elle ouvert pour la première fois ?"));
            answer1Field = new JTextField();
            add(answer1Field);

            add(new JLabel("Quand la cybersécurité est-elle devenue une nouvelle filière à ENSA Tetouan ?"));
            answer2Field = new JTextField();
            add(answer2Field);

            add(new JLabel("Quel est le prénom du premier enseignant ?"));
            answer3Field = new JTextField();
            add(answer3Field);

            JButton resetButton = new JButton("Réinitialiser le mot de passe");
            resetButton.setBackground(new Color(0, 119, 181)); // LinkedIn blue
            resetButton.setForeground(Color.WHITE);
            resetButton.setFocusPainted(false);
            resetButton.setFont(new Font("Arial", Font.BOLD, 14));
            resetButton.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
            resetButton.addActionListener(e -> resetPassword());
            add(resetButton);

            setSize(350, 200);
            setLocationRelativeTo(null);
            setVisible(true);
        }

        private void resetPassword() {
            String answer1 = answer1Field.getText().trim().toLowerCase();
            String answer2 = answer2Field.getText().trim().toLowerCase();
            String answer3 = answer3Field.getText().trim().toLowerCase();

            if (answer1.equals("2008") && answer2.equals("2024") && answer3.equals("fouad")) {
                String newPassword = JOptionPane.showInputDialog(this, "Entrez le nouveau mot de passe :");
                if (newPassword != null && !newPassword.isEmpty()) {
                    try (Connection conn = connect();
                         PreparedStatement stmt = conn.prepareStatement("UPDATE admins SET password = ? WHERE username = 'admin'")) {

                        stmt.setString(1, newPassword);
                        stmt.executeUpdate();
                        JOptionPane.showMessageDialog(this, "Mot de passe réinitialisé avec succès.");
                        dispose();
                    } catch (SQLException ex) {
                        JOptionPane.showMessageDialog(this, "Erreur de la base de données : " + ex.getMessage());
                    }
                }
            } else {
                JOptionPane.showMessageDialog(this, "Les réponses aux questions sont incorrectes.");
            }
        }
    }
}

