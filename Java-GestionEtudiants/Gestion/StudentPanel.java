import javax.swing.*;
import javax.swing.RowFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.*;
import java.util.List;
import javax.swing.table.TableRowSorter;

public class StudentPanel extends JFrame {
    private JTable studentTable;
    private DefaultTableModel tableModel;
    private JComboBox<String> filterFiliereComboBox;
    private JComboBox<String> filterNiveauComboBox;
    private JTextField searchField;

    public StudentPanel() {
        setTitle("Gestion des étudiants");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());
        getContentPane().setBackground(new Color(240, 248, 255)); // Light background

        // Set up the table model
        tableModel = new DefaultTableModel(new String[]{"Code Apogée", "Nom", "Prénom", "Cycle", "Filière", "Niveau", "Sexe", "Age", "Absences", "Excusées"}, 0);
        studentTable = new JTable(tableModel);
        studentTable.setFont(new Font("Arial", Font.PLAIN, 14)); // Set font
        studentTable.setRowHeight(30); // Set row height
        studentTable.setSelectionBackground(new Color(173, 216, 230)); // Highlight selected row
        loadStudents();

        // Panel for the filters (keep this at the top)
        JPanel filterPanel = new JPanel();
        filterPanel.setLayout(new FlowLayout());
        filterPanel.setBackground(new Color(70, 130, 180)); // Blue background
        
        filterPanel.add(new JLabel("Filière:"));
        filterFiliereComboBox = new JComboBox<>(new String[]{"Toutes", "Sciences des Données", "Génie en Cybersécurité", "Génie Informatique", "Génie Civil", "Génie Mécatronique", "Génie des Systèmes de Télécommunications", "Supply Chain Management"});
        filterPanel.add(filterFiliereComboBox);

        filterPanel.add(new JLabel("Niveau:"));
        filterNiveauComboBox = new JComboBox<>(new String[]{"Tous les niveaux"}); // Default option
        filterPanel.add(filterNiveauComboBox);

        // Add action listener to combo boxes for filtering
        filterFiliereComboBox.addActionListener(e -> updateNiveauOptions());
        filterFiliereComboBox.addActionListener(e -> filterStudents());
        filterNiveauComboBox.addActionListener(e -> filterStudents());

        // Add buttons for add and delete
        JButton addButton = new JButton("Ajouter étudiant");
        customizeButton(addButton);

        JButton deleteButton = new JButton("Supprimer étudiant");
        customizeButton(deleteButton);

        addButton.addActionListener(e -> addStudent());
        deleteButton.addActionListener(e -> deleteStudent());

        // Panel for the action buttons
        JPanel buttonPanel = new JPanel();
        buttonPanel.setBackground(Color.WHITE);
        buttonPanel.add(addButton);
        buttonPanel.add(deleteButton);

        // Panel for the search bar (move this to the bottom)
        JPanel searchPanel = new JPanel();
        searchPanel.setLayout(new FlowLayout());
        searchPanel.setBackground(Color.WHITE);
        searchPanel.add(new JLabel("Recherche:"));
        searchField = new JTextField(20);
        searchField.setFont(new Font("Arial", Font.PLAIN, 14));
        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                searchStudents(searchField.getText());
            }
        });
        searchPanel.add(searchField);

        // Combine the button panel and search panel into a new panel
        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BorderLayout());
        bottomPanel.setBackground(Color.WHITE);
        bottomPanel.add(buttonPanel, BorderLayout.NORTH);
        bottomPanel.add(searchPanel, BorderLayout.SOUTH);

        // Add components to frame
        add(filterPanel, BorderLayout.NORTH); // Filters at the top
        add(new JScrollPane(studentTable), BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);  // Combined action buttons and search panel

        setSize(800, 600);
        setLocationRelativeTo(null);
        setVisible(true);
        
        updateNiveauOptions(); // Initialize niveau options based on the default filiere
    }

    private void customizeButton(JButton button) {
        button.setBackground(new Color(70, 130, 180)); // Button color
        button.setForeground(Color.WHITE); // Button text color
        button.setFont(new Font("Arial", Font.BOLD, 14));
        button.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        button.setFocusPainted(false);
    }

    private void loadStudents() {
        try (Connection conn = connect();
             Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT * FROM students");
            tableModel.setRowCount(0);
            while (rs.next()) {
                Vector<Object> row = new Vector<>();
                row.add(rs.getString("code_apogee"));
                row.add(rs.getString("nom"));
                row.add(rs.getString("prenom"));
                row.add(rs.getString("cycle"));
                row.add(rs.getString("filiere"));
                row.add(rs.getString("niveau"));
                row.add(rs.getString("sexe"));
                row.add(rs.getInt("age"));
                row.add(rs.getInt("absence_count"));
                row.add(rs.getInt("absence_excused"));
                tableModel.addRow(row);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Erreur lors du chargement des étudiants : " + e.getMessage());
        }
    }

    private void updateNiveauOptions() {
        // Clear the niveau options
        filterNiveauComboBox.removeAllItems();
        String selectedFiliere = (String) filterFiliereComboBox.getSelectedItem();

        if (selectedFiliere != null && !selectedFiliere.equals("Toutes")) {
            // Load niveaux based on selected filière
            switch (selectedFiliere) {
                case "Sciences des Données":
                    filterNiveauComboBox.addItem("BDIA1");
                    filterNiveauComboBox.addItem("BDIA2");
                    filterNiveauComboBox.addItem("BDIA3");
                    break;
                case "Génie en Cybersécurité":
                    filterNiveauComboBox.addItem("GCSE1");
                    filterNiveauComboBox.addItem("GCSE2");
                    filterNiveauComboBox.addItem("GCSE3");
                    break;
                case "Génie Informatique":
                    filterNiveauComboBox.addItem("GE1");
                    filterNiveauComboBox.addItem("GE2");
                    filterNiveauComboBox.addItem("GE3");
                    break;
                case "Génie Civil":
                    filterNiveauComboBox.addItem("GC1");
                    filterNiveauComboBox.addItem("GC2");
                    filterNiveauComboBox.addItem("GC3");
                    break;
                case "Génie Mécatronique":
                    filterNiveauComboBox.addItem("GM1");
                    filterNiveauComboBox.addItem("GM2");
                    filterNiveauComboBox.addItem("GM3");
                    break;
                case "Génie des Systèmes de Télécommunications":
                    filterNiveauComboBox.addItem("GSTR1");
                    filterNiveauComboBox.addItem("GSTR2");
                    filterNiveauComboBox.addItem("GSTR3");
                    break;
                case "Supply Chain Management":
                    filterNiveauComboBox.addItem("SCM1");
                    filterNiveauComboBox.addItem("SCM2");
                    filterNiveauComboBox.addItem("SCM3");
                    break;
                default:
                    break;
            }
        } else {
            // If "Toutes" is selected, show all niveaux
                   filterNiveauComboBox.addItem("2AP1");
                   filterNiveauComboBox.addItem("2AP2");
                   filterNiveauComboBox.addItem("BDIA1");
                   filterNiveauComboBox.addItem("BDIA2");
                   filterNiveauComboBox.addItem("BDIA3");
                   filterNiveauComboBox.addItem("GCSE1");
                   filterNiveauComboBox.addItem("GCSE2");
                   filterNiveauComboBox.addItem("GCSE3");
                   filterNiveauComboBox.addItem("GE1");
                   filterNiveauComboBox.addItem("GE2");
                   filterNiveauComboBox.addItem("GE3");
                   filterNiveauComboBox.addItem("GC1");
                   filterNiveauComboBox.addItem("GC2");
                   filterNiveauComboBox.addItem("GC3");
                    filterNiveauComboBox.addItem("GM1");
                    filterNiveauComboBox.addItem("GM2");
                    filterNiveauComboBox.addItem("GM3");
                    filterNiveauComboBox.addItem("GSTR1");
                    filterNiveauComboBox.addItem("GSTR2");
                    filterNiveauComboBox.addItem("GSTR3");
                    filterNiveauComboBox.addItem("SCM1");
                    filterNiveauComboBox.addItem("SCM2");
                    filterNiveauComboBox.addItem("SCM3");
        }

        // Add "Tous les niveaux" option
        filterNiveauComboBox.insertItemAt("Tous les niveaux", 0);
        filterNiveauComboBox.setSelectedIndex(0); // Select the first item
    }

    private void addStudent() {
        JTextField codeField = new JTextField();
        JTextField nomField = new JTextField();
        JTextField prenomField = new JTextField();
        JTextField ageField = new JTextField();

        JComboBox<String> cycleBox = new JComboBox<>(new String[]{"Cycle Préparatoire", "Cycle Ingénieur"});
        JComboBox<String> filiereBox = new JComboBox<>();
        JComboBox<String> niveauBox = new JComboBox<>();
        JComboBox<String> sexeBox = new JComboBox<>(new String[]{"Homme", "Femme"});

        cycleBox.addActionListener(e -> {
            String cycle = (String) cycleBox.getSelectedItem();
            filiereBox.removeAllItems();
            niveauBox.removeAllItems();
            if (cycle.equals("Cycle Préparatoire")) {
                filiereBox.addItem("Deux Années Préparatoires");
                niveauBox.addItem("2AP1");
                niveauBox.addItem("2AP2");
            } else {
                filiereBox.addItem("Sciences des Données, Big Data & IA");
                filiereBox.addItem("Génie en Cybersécurité et Systèmes Embarqués");
                filiereBox.addItem("Génie Informatique");
                filiereBox.addItem("Génie Civil");
                filiereBox.addItem("Génie Mécatronique");
                filiereBox.addItem("Génie des Systèmes de Télécommunications et Réseaux");
                filiereBox.addItem("Supply Chain Management");
            }
        });

        filiereBox.addActionListener(e -> {
            String filiere = (String) filiereBox.getSelectedItem();
            niveauBox.removeAllItems();
            if (filiere != null) {
                switch (filiere) {
                    case "Sciences des Données, Big Data & IA":
                        niveauBox.addItem("BDIA1");
                        niveauBox.addItem("BDIA2");
                        niveauBox.addItem("BDIA3");
                        break;
                    case "Génie en Cybersécurité et Systèmes Embarqués":
                        niveauBox.addItem("GCSE1");
                        niveauBox.addItem("GCSE2");
                        niveauBox.addItem("GCSE3");
                        break;
                    case "Génie Informatique":
                        niveauBox.addItem("GE1");
                        niveauBox.addItem("GE2");
                        niveauBox.addItem("GE3");
                        break;
                    case "Génie Civil":
                        niveauBox.addItem("GC1");
                        niveauBox.addItem("GC2");
                        niveauBox.addItem("GC3");
                        break;
                    case "Génie Mécatronique":
                        niveauBox.addItem("GM1");
                        niveauBox.addItem("GM2");
                        niveauBox.addItem("GM3");
                        break;
                    case "Génie des Systèmes de Télécommunications et Réseaux":
                        niveauBox.addItem("GSTR1");
                        niveauBox.addItem("GSTR2");
                        niveauBox.addItem("GSTR3");
                        break;
                    case "Supply Chain Management":
                        niveauBox.addItem("SCM1");
                        niveauBox.addItem("SCM2");
                        niveauBox.addItem("SCM3");
                        break;
                }
            }
        });

        Object[] message = {
                "Code Apogée:", codeField,
                "Nom:", nomField,
                "Prénom:", prenomField,
                "Cycle:", cycleBox,
                "Filière:", filiereBox,
                "Niveau:", niveauBox,
                "Sexe:", sexeBox,
                "Age:", ageField
        };

        int option = JOptionPane.showConfirmDialog(this, message, "Ajouter un étudiant", JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION) {
            String codeApogee = codeField.getText();
            String nom = nomField.getText();
            String prenom = prenomField.getText();
            String cycle = (String) cycleBox.getSelectedItem();
            String filiere = (String) filiereBox.getSelectedItem();
            String niveau = (String) niveauBox.getSelectedItem();
            String sexe = (String) sexeBox.getSelectedItem();
            int age = Integer.parseInt(ageField.getText());

            try (Connection conn = connect();
                 PreparedStatement stmt = conn.prepareStatement("INSERT INTO students (code_apogee, nom, prenom, cycle, filiere, niveau, sexe, age) VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
                stmt.setString(1, codeApogee);
                stmt.setString(2, nom);
                stmt.setString(3, prenom);
                stmt.setString(4, cycle);
                stmt.setString(5, filiere);
                stmt.setString(6, niveau);
                stmt.setString(7, sexe);
                stmt.setInt(8, age);
                stmt.executeUpdate();
                loadStudents();
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Erreur lors de l'ajout de l'étudiant : " + e.getMessage());
            }
        }
    }

    private void deleteStudent() {
        int selectedRow = studentTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Sélectionnez une ligne pour supprimer un étudiant.");
            return;
        }

        String codeApogee = (String) studentTable.getValueAt(selectedRow, 0);

        int confirmation = JOptionPane.showConfirmDialog(this, "Êtes-vous sûr de vouloir supprimer l'étudiant ?", "Confirmer la suppression", JOptionPane.YES_NO_OPTION);
        if (confirmation == JOptionPane.YES_OPTION) {
            try (Connection conn = connect();
                 PreparedStatement stmt = conn.prepareStatement("DELETE FROM students WHERE code_apogee = ?")) {
                stmt.setString(1, codeApogee);
                stmt.executeUpdate();
                loadStudents();
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Erreur lors de la suppression de l'étudiant : " + e.getMessage());
            }
        }
    }

    private Connection connect() {
        try {
            return DriverManager.getConnection("jdbc:sqlite:ensa_tetouan.db"); // Adjust database URL as needed
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Erreur de connexion à la base de données : " + e.getMessage());
            return null;
        }
    }

private void searchStudents(String query) {
    TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(tableModel);
    studentTable.setRowSorter(sorter);

    // Filter based on search query
    RowFilter<Object, Object> searchFilter = RowFilter.regexFilter(query, 1, 2, 0);  // Search in "Code Apogée", "Nom", and "Prénom" columns
    sorter.setRowFilter(searchFilter);
}

    private void filterStudents() {
        String filiereFilter = (String) filterFiliereComboBox.getSelectedItem();
        String niveauFilter = (String) filterNiveauComboBox.getSelectedItem();

        List<RowFilter<Object, Object>> filters = new ArrayList<>();
        if (filiereFilter != null && !filiereFilter.equals("Toutes")) {
            filters.add(RowFilter.regexFilter("(?i)" + filiereFilter, 4)); // Column index 4 is for "Filière"
        }
        if (niveauFilter != null && !niveauFilter.equals("Tous les niveaux")) {
            filters.add(RowFilter.regexFilter("(?i)" + niveauFilter, 5)); // Column index 5 is for "Niveau"
        }

        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(tableModel);
        studentTable.setRowSorter(sorter);
        if (filters.isEmpty()) {
            sorter.setRowFilter(null); // Show all students
        } else {
            sorter.setRowFilter(RowFilter.andFilter(filters)); // Apply filters
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new StudentPanel());
    }
}

