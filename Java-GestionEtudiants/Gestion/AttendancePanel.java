import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Vector;

public class AttendancePanel extends JFrame {
    private JTable attendanceTable;
    private DefaultTableModel tableModel;
    private JTextField searchField;
    private JComboBox<String> filiereComboBox;
    private JComboBox<String> niveauComboBox;
    private JTextField yearField;
    private JTextField monthField;
    private JTextField dayField;
    private JCheckBox yearCheckBox;
    private JCheckBox monthCheckBox;
    private JCheckBox dayCheckBox;
    private JComboBox<String> statusBox;
    private JTextField codeApogeeField;

    public AttendancePanel() {
        setTitle("Gestion des absences");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());
        getContentPane().setBackground(new Color(240, 248, 255)); // Light background

        // Create the table model for attendance data
        tableModel = new DefaultTableModel(new String[]{"Code Apogée", "Nom", "Prénom", "Filière", "Niveau", "Date", "Statut", "Séances"}, 0);
        attendanceTable = new JTable(tableModel);
        attendanceTable.setFont(new Font("Arial", Font.PLAIN, 14)); // Set font
        attendanceTable.setRowHeight(30); // Set row height
        attendanceTable.setSelectionBackground(new Color(173, 216, 230)); // Highlight selected row
        loadAttendance(); // Load attendance data from the database

        // Table Panel
        JScrollPane tableScroll = new JScrollPane(attendanceTable);
        tableScroll.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(70, 130, 180), 2), "Liste des Absences"));
        add(tableScroll, BorderLayout.CENTER);

        // Search panel setup
        JPanel searchPanel = new JPanel();
        searchPanel.setLayout(new GridBagLayout());
        searchPanel.setBackground(new Color(70, 130, 180)); // Blue background
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5); // Add padding around elements

        gbc.gridx = 0;
        gbc.gridy = 0;
        searchPanel.add(new JLabel("Rechercher par Code Apogée / Nom:"), gbc);

        gbc.gridx = 1;
        searchField = new JTextField(15);
        searchPanel.add(searchField, gbc);

        // Filter by Filière
        gbc.gridx = 0;
        gbc.gridy = 1;
        searchPanel.add(new JLabel("Filière:"), gbc);
        gbc.gridx = 1;
        filiereComboBox = new JComboBox<>();
        loadFiliereOptions(); // Load options for filière
        searchPanel.add(filiereComboBox, gbc);

        // Filter by Niveau
        gbc.gridx = 0;
        gbc.gridy = 2;
        searchPanel.add(new JLabel("Niveau:"), gbc);
        gbc.gridx = 1;
        niveauComboBox = new JComboBox<>();
        loadNiveauOptions(); // Load options for niveau
        searchPanel.add(niveauComboBox, gbc);

        // Date filtering options
        JPanel datePanel = new JPanel(new FlowLayout());
        datePanel.setBackground(new Color(70, 130, 180)); // Blue background
        yearCheckBox = new JCheckBox("Filtrer par Année");
        yearField = new JTextField(5);
        monthCheckBox = new JCheckBox("Filtrer par Mois");
        monthField = new JTextField(5);
        dayCheckBox = new JCheckBox("Filtrer par Jour");
        dayField = new JTextField(5);
        datePanel.add(yearCheckBox);
        datePanel.add(yearField);
        datePanel.add(monthCheckBox);
        datePanel.add(monthField);
        datePanel.add(dayCheckBox);
        datePanel.add(dayField);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        searchPanel.add(datePanel, gbc);

        // Search Button
        JButton searchButton = new JButton("Exécuter la recherche");
        searchButton.setBackground(new Color(0, 103, 255)); // Button color
        searchButton.setForeground(Color.WHITE); // Button text color
        searchButton.setFocusPainted(false);
        searchButton.addActionListener(e -> searchAttendance());
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        searchPanel.add(searchButton, gbc);

        add(searchPanel, BorderLayout.NORTH);

        // Button Panel with some spacing between buttons
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 20, 10));
        buttonPanel.setBackground(new Color(240, 248, 255)); // Light background

        JButton markButton = new JButton("Marquer l'absence");
        markButton.setBackground(new Color(0, 103, 255)); // Button color
        markButton.setForeground(Color.WHITE); // Button text color
        markButton.setFocusPainted(false);
        markButton.addActionListener(e -> markAttendance());

        JButton deleteButton = new JButton("Supprimer l'absence");
        deleteButton.setBackground(new Color(0, 103, 255)); // Button color
        deleteButton.setForeground(Color.WHITE); // Button text color
        deleteButton.setFocusPainted(false);
        deleteButton.addActionListener(e -> deleteAttendance());

        buttonPanel.add(markButton);
        buttonPanel.add(deleteButton);

        add(buttonPanel, BorderLayout.SOUTH);

        // Add table row selection listener
        attendanceTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting() && attendanceTable.getSelectedRow() != -1) {
                    String selectedCodeApogee = (String) attendanceTable.getValueAt(attendanceTable.getSelectedRow(), 0);
                    codeApogeeField.setText(selectedCodeApogee); // Auto-fill code_apogee
                }
            }
        });

        setSize(800, 600);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void loadAttendance() {
        try (Connection conn = connect();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT s.code_apogee, s.nom, s.prenom, s.filiere, s.niveau, a.absence_date, a.status, a.sessions_count " +
                             "FROM students s " +
                             "LEFT JOIN absences a ON s.code_apogee = a.code_apogee")) {
            ResultSet rs = stmt.executeQuery();
            tableModel.setRowCount(0);
            while (rs.next()) {
                Vector<Object> row = new Vector<>();
                row.add(rs.getString("code_apogee"));
                row.add(rs.getString("nom"));
                row.add(rs.getString("prenom"));
                row.add(rs.getString("filiere"));
                row.add(rs.getString("niveau"));
                row.add(rs.getString("absence_date"));
                row.add(rs.getString("status"));
                row.add(rs.getInt("sessions_count"));
                tableModel.addRow(row);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Erreur lors du chargement des absences : " + e.getMessage());
        }
    }

    private void loadFiliereOptions() {
        try (Connection conn = connect();
             Statement stmt = conn.createStatement()) {
            String query = "SELECT DISTINCT filiere FROM students";
            ResultSet rs = stmt.executeQuery(query);
            filiereComboBox.addItem("Tous"); // Default value
            while (rs.next()) {
                filiereComboBox.addItem(rs.getString("filiere"));
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Erreur lors du chargement des filières : " + e.getMessage());
        }
    }

    private void loadNiveauOptions() {
        try (Connection conn = connect();
             Statement stmt = conn.createStatement()) {
            String query = "SELECT DISTINCT niveau FROM students";
            ResultSet rs = stmt.executeQuery(query);
            niveauComboBox.addItem("Tous"); // Default value
            while (rs.next()) {
                niveauComboBox.addItem(rs.getString("niveau"));
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Erreur lors du chargement des niveaux : " + e.getMessage());
        }
    }

    private void searchAttendance() {
        String searchText = searchField.getText().trim();
        String filiere = (String) filiereComboBox.getSelectedItem();
        String niveau = (String) niveauComboBox.getSelectedItem();

        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("SELECT s.code_apogee, s.nom, s.prenom, s.filiere, s.niveau, a.absence_date, a.status, a.sessions_count FROM students s ")
                .append("LEFT JOIN absences a ON s.code_apogee = a.code_apogee WHERE 1=1");

        if (!searchText.isEmpty()) {
            queryBuilder.append(" AND (s.code_apogee LIKE '%").append(searchText).append("%' OR s.nom LIKE '%").append(searchText).append("%')");
        }
        if (!"Tous".equals(filiere)) {
            queryBuilder.append(" AND s.filiere = '").append(filiere).append("'");
        }
        if (!"Tous".equals(niveau)) {
            queryBuilder.append(" AND s.niveau = '").append(niveau).append("'");
        }

        // Add date filters based on checkboxes
        if (yearCheckBox.isSelected()) {
            String year = yearField.getText().trim();
            if (isValidYear(year)) {
                queryBuilder.append(" AND strftime('%Y', a.absence_date) = '").append(year).append("'");
            } else {
                JOptionPane.showMessageDialog(this, "Veuillez entrer une année valide.");
                return;
            }
        }
        if (monthCheckBox.isSelected()) {
            String month = monthField.getText().trim();
            if (isValidMonth(month)) {
                queryBuilder.append(" AND strftime('%m', a.absence_date) = '").append(month).append("'");
            } else {
                JOptionPane.showMessageDialog(this, "Veuillez entrer un mois valide (01-12).");
                return;
            }
        }
        if (dayCheckBox.isSelected()) {
            String day = dayField.getText().trim();
            if (isValidDay(day)) {
                queryBuilder.append(" AND strftime('%d', a.absence_date) = '").append(day).append("'");
            } else {
                JOptionPane.showMessageDialog(this, "Veuillez entrer un jour valide (01-31).");
                return;
            }
        }

        try (Connection conn = connect();
             PreparedStatement stmt = conn.prepareStatement(queryBuilder.toString())) {
            ResultSet rs = stmt.executeQuery();
            tableModel.setRowCount(0); // Clear previous rows
            int rowCount = 0;
            while (rs.next()) {
                Vector<Object> row = new Vector<>();
                row.add(rs.getString("code_apogee"));
                row.add(rs.getString("nom"));
                row.add(rs.getString("prenom"));
                row.add(rs.getString("filiere"));
                row.add(rs.getString("niveau"));
                row.add(rs.getString("absence_date"));
                row.add(rs.getString("status"));
                row.add(rs.getInt("sessions_count"));
                tableModel.addRow(row);
                rowCount++;
            }

            // Inform the user of the number of results found
            if (rowCount == 0) {
                JOptionPane.showMessageDialog(this, "Aucun enregistrement trouvé.");
            } else {
                JOptionPane.showMessageDialog(this, rowCount + " enregistrement(s) trouvé(s).");
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Erreur lors de la recherche des absences : " + e.getMessage());
        }
    }

    // Validate year input
    private boolean isValidYear(String year) {
        return year.matches("\\d{4}"); // Year should be 4 digits
    }

    // Validate month input
    private boolean isValidMonth(String month) {
        return month.matches("^(0[1-9]|1[0-2])$"); // Month should be 01 to 12
    }

    // Validate day input
    private boolean isValidDay(String day) {
        return day.matches("^(0[1-9]|[12][0-9]|3[01])$"); // Day should be 01 to 31
    }

    private void markAttendance() {
        // Show dialog to mark attendance
        JPanel panel = new JPanel(new GridLayout(0, 2));

        panel.add(new JLabel("Code Apogée:"));
        codeApogeeField = new JTextField();
        panel.add(codeApogeeField);

        if (attendanceTable.getSelectedRow() != -1) {
            String selectedCodeApogee = (String) attendanceTable.getValueAt(attendanceTable.getSelectedRow(), 0);
            codeApogeeField.setText(selectedCodeApogee); // Auto-fill code_apogee if selected
        }

        panel.add(new JLabel("Date de l'absence (yyyy-MM-dd):"));
        JTextField dateField = new JTextField();
        panel.add(dateField);

        panel.add(new JLabel("Séances:"));
        JTextField sessionsCountField = new JTextField();
        panel.add(sessionsCountField);

        statusBox = new JComboBox<>(new String[]{"Absent", "Excusé"});
        panel.add(new JLabel("Statut de l'absence:"));
        panel.add(statusBox);

        int option = JOptionPane.showConfirmDialog(this, panel, "Marquer une absence", JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION) {
            String codeApogee = codeApogeeField.getText().trim();
            String absenceDate = dateField.getText().trim();
            String status = (String) statusBox.getSelectedItem();
            int sessionsCount;

            // Validate sessions count input
            try {
                sessionsCount = Integer.parseInt(sessionsCountField.getText().trim());
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Veuillez entrer un nombre valide pour les séances.");
                return;
            }

            // Validate date format
            if (!isValidDate(absenceDate)) {
                JOptionPane.showMessageDialog(this, "Le format de la date est incorrect. Veuillez entrer la date au format YYYY-MM-DD.");
                return;
            }

            // Insert into the database
            try (Connection conn = connect();
                 PreparedStatement stmt = conn.prepareStatement("INSERT INTO absences (code_apogee, absence_date, status, sessions_count) VALUES (?, ?, ?, ?)")) {
                stmt.setString(1, codeApogee);
                stmt.setString(2, absenceDate);
                stmt.setString(3, status);
                stmt.setInt(4, sessionsCount);
                stmt.executeUpdate();

                // Update student absences
                updateStudentAbsences(codeApogee, sessionsCount, status);

                loadAttendance();
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Erreur lors du marquage de l'absence : " + e.getMessage());
            }
        }
    }

    // Validate date format
    private boolean isValidDate(String date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        sdf.setLenient(false);
        try {
            sdf.parse(date);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void updateStudentAbsences(String codeApogee, int sessionsCount, String status) {
        try (Connection conn = connect()) {
            // Update absence_count
            try (PreparedStatement stmt = conn.prepareStatement("UPDATE students SET absence_count = absence_count + ? WHERE code_apogee = ?")) {
                stmt.setInt(1, sessionsCount);
                stmt.setString(2, codeApogee);
                stmt.executeUpdate();
            }

            // Update absence_excused if the status is 'Excusé'
            if ("Excusé".equals(status)) {
                try (PreparedStatement stmt = conn.prepareStatement("UPDATE students SET absence_excused = absence_excused + ? WHERE code_apogee = ?")) {
                    stmt.setInt(1, sessionsCount);
                    stmt.setString(2, codeApogee);
                    stmt.executeUpdate();
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Erreur lors de la mise à jour des absences de l'étudiant : " + e.getMessage());
        }
    }

    private void deleteAttendance() {
        int selectedRow = attendanceTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Sélectionnez une ligne pour supprimer une absence.");
            return;
        }

        String codeApogee = (String) attendanceTable.getValueAt(selectedRow, 0);
        String absenceDate = (String) attendanceTable.getValueAt(selectedRow, 5);

        int confirmation = JOptionPane.showConfirmDialog(this, "Êtes-vous sûr de vouloir supprimer l'absence ?", "Confirmer la suppression", JOptionPane.YES_NO_OPTION);
        if (confirmation == JOptionPane.YES_OPTION) {
            try (Connection conn = connect();
                 PreparedStatement stmt = conn.prepareStatement("DELETE FROM absences WHERE code_apogee = ? AND absence_date = ?")) {
                stmt.setString(1, codeApogee);
                stmt.setString(2, absenceDate);
                stmt.executeUpdate();

                // Update the student absence count and excused absence count
                int sessionsCount = (int) attendanceTable.getValueAt(selectedRow, 7);
                String status = (String) attendanceTable.getValueAt(selectedRow, 6);
                updateStudentAbsencesAfterDeletion(codeApogee, sessionsCount, status);

                loadAttendance();
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Erreur lors de la suppression de l'absence : " + e.getMessage());
            }
        }
    }

    private void updateStudentAbsencesAfterDeletion(String codeApogee, int sessionsCount, String status) {
        try (Connection conn = connect()) {
            // Update absence_count
            try (PreparedStatement stmt = conn.prepareStatement("UPDATE students SET absence_count = absence_count - ? WHERE code_apogee = ?")) {
                stmt.setInt(1, sessionsCount);
                stmt.setString(2, codeApogee);
                stmt.executeUpdate();
            }

            // Update absence_excused if the status was 'Excusé'
            if ("Excusé".equals(status)) {
                try (PreparedStatement stmt = conn.prepareStatement("UPDATE students SET absence_excused = absence_excused - ? WHERE code_apogee = ?")) {
                    stmt.setInt(1, sessionsCount);
                    stmt.setString(2, codeApogee);
                    stmt.executeUpdate();
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Erreur lors de la mise à jour des absences de l'étudiant après la suppression : " + e.getMessage());
        }
    }

    private Connection connect() {
        try {
            return DriverManager.getConnection("jdbc:sqlite:ensa_tetouan.db");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Erreur de connexion à la base de données : " + e.getMessage());
            return null;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(AttendancePanel::new);
    }
}

