import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import java.sql.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class ChatWindow {
    private JFrame frame;
    private String username;
    private String databaseUrl;
    private String serverIp; // Added serverIp variable
    private int serverPort; // Added serverPort variable
    private JPanel messagePanel;
    private JTextField messageInput;
    private JTable userTable;
    private DefaultTableModel userTableModel;
    private Timer messageRefreshTimer;
    private Socket socket;
    private DataOutputStream dos;
    private DataInputStream dis;

    public ChatWindow(String username, String databaseUrl, String serverIp, int serverPort) {
        this.username = username;
        this.databaseUrl = databaseUrl;
        this.serverIp = serverIp; // Store server IP
        this.serverPort = serverPort; // Store server port
        initializeUI();
        loadUsers();
        startMessageRefresh();
        connectToServer();
        updateOnlineStatus("Online");
        listenForNotifications();
    }

    public void display() {
        frame.setVisible(true);
    }

    private void initializeUI() {
        frame = new JFrame("Chat Window");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1000, 700);
        frame.setLayout(new BorderLayout());

        // Set background color
        frame.getContentPane().setBackground(new Color(240, 240, 240));

        // Title Bar
        JPanel titleBar = new JPanel(new BorderLayout());
        titleBar.setBackground(new Color(0, 102, 204)); // Blue color
        titleBar.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel titleLabel = new JLabel("Chat Application", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        titleLabel.setForeground(Color.WHITE);
        titleBar.add(titleLabel, BorderLayout.CENTER);

        frame.add(titleBar, BorderLayout.NORTH);

        // User Table
        userTableModel = new DefaultTableModel(new String[]{"Users", "Status"}, 0);
        userTable = new JTable(userTableModel);
        userTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        userTable.setBackground(new Color(255, 255, 255));
        userTable.setForeground(new Color(0, 0, 0));
        userTable.setFont(new Font("Arial", Font.PLAIN, 14));
        userTable.setRowHeight(25);

        JScrollPane userScrollPane = new JScrollPane(userTable);
        userScrollPane.setPreferredSize(new Dimension(250, 0));
        userScrollPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        frame.add(userScrollPane, BorderLayout.WEST);

        // Message Panel
        messagePanel = new JPanel();
        messagePanel.setLayout(new BoxLayout(messagePanel, BoxLayout.Y_AXIS));
        messagePanel.setBackground(new Color(255, 255, 255));
        JScrollPane messageScrollPane = new JScrollPane(messagePanel);
        messageScrollPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        frame.add(messageScrollPane, BorderLayout.CENTER);

        // Input Panel
        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.setBackground(new Color(240, 240, 240));
        inputPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        messageInput = new JTextField();
        messageInput.setFont(new Font("Arial", Font.PLAIN, 14));
        messageInput.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0, 102, 204), 1),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        messageInput.addActionListener(e -> sendMessage());
        inputPanel.add(messageInput, BorderLayout.CENTER);

        JButton sendButton = createStyledButton("Send", new Color(0, 102, 204));
        sendButton.addActionListener(e -> sendMessage());
        inputPanel.add(sendButton, BorderLayout.EAST);

        JButton sendFileButton = createStyledButton("Send File", new Color(0, 102, 204));
        sendFileButton.addActionListener(e -> sendFile());
        inputPanel.add(sendFileButton, BorderLayout.WEST);

        // Add right-click context menu for message removal
        messagePanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showPopupMenu(e);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showPopupMenu(e);
                }
            }
        });

        frame.add(inputPanel, BorderLayout.SOUTH);
    }

    private JButton createStyledButton(String text, Color color) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 14));
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return button;
    }

    private void connectToServer() {
        try {
            socket = new Socket(serverIp, serverPort); // Use serverIp and serverPort
            dos = new DataOutputStream(socket.getOutputStream());
            dis = new DataInputStream(socket.getInputStream());
            dos.writeUTF("LOGIN");
            dos.writeUTF(username);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(frame, "Error connecting to server: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void listenForNotifications() {
        new Thread(() -> {
            try {
                while (true) {
                    String notification = dis.readUTF();
                    if ("NEW_MESSAGE".equals(notification)) {
                        String sender = dis.readUTF();
                        String message = dis.readUTF();
                        Timestamp timestamp = new Timestamp(dis.readLong());
                        SwingUtilities.invokeLater(() -> {
                            appendMessageToChat(sender, message, timestamp);
                            showNotification("New message from " + sender + ": " + message);
                        });
                    } else if ("NEW_FILE".equals(notification)) {
                        String sender = dis.readUTF();
                        String fileName = dis.readUTF();
                        Timestamp timestamp = new Timestamp(dis.readLong());
                        SwingUtilities.invokeLater(() -> {
                            appendFileMessageToChat(sender, fileName, timestamp);
                            showNotification("New file from " + sender + ": " + fileName);
                        });
                    }
                }
            } catch (IOException e) {
                System.err.println("Connection closed: " + e.getMessage());
                updateOnlineStatus("Offline");
            }
        }).start();
    }

    private void showNotification(String message) {
        JOptionPane.showMessageDialog(frame, message, "New Message", JOptionPane.INFORMATION_MESSAGE);
    }

    private void loadUsers() {
        try (Connection conn = DriverManager.getConnection(databaseUrl, "root", "Md1SuL81fahq1BP19Zau");
             PreparedStatement ps = conn.prepareStatement("SELECT username, status FROM users WHERE username != ?")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                userTableModel.setRowCount(0);
                while (rs.next()) {
                    userTableModel.addRow(new Object[]{
                            rs.getString("username"),
                            rs.getString("status")
                    });
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(frame, "Error loading users: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadChat(String selectedUser) {
        messagePanel.removeAll();
        try (Connection conn = DriverManager.getConnection(databaseUrl, "root", "Md1SuL81fahq1BP19Zau")) {
            // Query for text messages
            String textMessagesQuery = "SELECT sender, message, timestamp FROM messages WHERE " +
                    "(sender = ? AND receiver = ?) OR (sender = ? AND receiver = ?) ORDER BY timestamp";
            PreparedStatement textMessagesStmt = conn.prepareStatement(textMessagesQuery);
            textMessagesStmt.setString(1, username);
            textMessagesStmt.setString(2, selectedUser);
            textMessagesStmt.setString(3, selectedUser);
            textMessagesStmt.setString(4, username);

            // Query for file messages
            String fileMessagesQuery = "SELECT sender, file_name, timestamp FROM file_messages WHERE " +
                    "(sender = ? AND receiver = ?) OR (sender = ? AND receiver = ?) ORDER BY timestamp";
            PreparedStatement fileMessagesStmt = conn.prepareStatement(fileMessagesQuery);
            fileMessagesStmt.setString(1, username);
            fileMessagesStmt.setString(2, selectedUser);
            fileMessagesStmt.setString(3, selectedUser);
            fileMessagesStmt.setString(4, username);

            // Execute both queries
            ResultSet textMessagesResult = textMessagesStmt.executeQuery();
            ResultSet fileMessagesResult = fileMessagesStmt.executeQuery();

            // Combine results into a single list
            List<ChatEntry> chatEntries = new ArrayList<>();

            // Add text messages to the list
            while (textMessagesResult.next()) {
                String sender = textMessagesResult.getString("sender");
                String message = textMessagesResult.getString("message");
                Timestamp timestamp = textMessagesResult.getTimestamp("timestamp");
                chatEntries.add(new ChatEntry(sender, message, timestamp, false)); // false = text message
            }

            // Add file messages to the list
            while (fileMessagesResult.next()) {
                String sender = fileMessagesResult.getString("sender");
                String fileName = fileMessagesResult.getString("file_name");
                Timestamp timestamp = fileMessagesResult.getTimestamp("timestamp");
                chatEntries.add(new ChatEntry(sender, fileName, timestamp, true)); // true = file message
            }

            // Sort the combined list by timestamp
            chatEntries.sort((e1, e2) -> e1.timestamp.compareTo(e2.timestamp));

            // Display the sorted chat entries
            for (ChatEntry entry : chatEntries) {
                if (entry.isFileMessage) {
                    appendFileMessageToChat(entry.sender, entry.content, entry.timestamp);
                } else {
                    appendMessageToChat(entry.sender, entry.content, entry.timestamp);
                }
            }

            messagePanel.revalidate();
            messagePanel.repaint();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(frame, "Error loading chat: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void appendMessageToChat(String sender, String message, Timestamp timestamp) {
        JPanel messageBubble = new JPanel();
        messageBubble.setLayout(new BoxLayout(messageBubble, BoxLayout.Y_AXIS));
        messageBubble.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        messageBubble.setBackground(sender.equals(username) ? new Color(0, 102, 204) : new Color(230, 230, 230));
        messageBubble.setAlignmentX(sender.equals(username) ? Component.RIGHT_ALIGNMENT : Component.LEFT_ALIGNMENT);

        // Store the sender and timestamp in the messageBubble's client properties
        messageBubble.putClientProperty("sender", sender);
        messageBubble.putClientProperty("timestamp", timestamp);

        JLabel messageLabel = new JLabel("<html><div style='padding: 5px;'>" + message + "</div></html>");
        messageLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        messageLabel.setForeground(sender.equals(username) ? Color.WHITE : Color.BLACK);

        JLabel timestampLabel = new JLabel("<html><div style='color: #666; font-size: 10px;'>" + timestamp + "</div></html>");
        timestampLabel.setFont(new Font("Arial", Font.ITALIC, 10));
        timestampLabel.setForeground(sender.equals(username) ? new Color(200, 200, 200) : new Color(102, 102, 102));

        messageBubble.add(messageLabel);
        messageBubble.add(timestampLabel);
        messagePanel.add(messageBubble);
        messagePanel.add(Box.createRigidArea(new Dimension(0, 10)));

        messagePanel.revalidate();
        messagePanel.repaint();
    }

    private void appendFileMessageToChat(String sender, String fileName, Timestamp timestamp) {
        JPanel fileBubble = new JPanel();
        fileBubble.setLayout(new BoxLayout(fileBubble, BoxLayout.Y_AXIS));
        fileBubble.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        fileBubble.setBackground(sender.equals(username) ? new Color(0, 102, 204) : new Color(230, 230, 230));
        fileBubble.setAlignmentX(sender.equals(username) ? Component.RIGHT_ALIGNMENT : Component.LEFT_ALIGNMENT);

        JLabel fileLabel = new JLabel("<html><div style='padding: 5px;'>" + sender + " sent a file: " + fileName + "</div></html>");
        fileLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        fileLabel.setForeground(sender.equals(username) ? Color.WHITE : Color.BLACK);

        JButton downloadButton = createStyledButton("Download", new Color(0, 102, 204));
        downloadButton.addActionListener(e -> downloadFile(fileName));

        // Add "Remove" button for files sent by the current user
        if (sender.equals(username)) {
            JButton removeButton = createStyledButton("Remove", new Color(204, 0, 0));
            removeButton.addActionListener(e -> removeFile(fileName, sender, timestamp));
            fileBubble.add(removeButton);
        }

        JLabel timestampLabel = new JLabel("<html><div style='color: #666; font-size: 10px;'>" + timestamp + "</div></html>");
        timestampLabel.setFont(new Font("Arial", Font.ITALIC, 10));
        timestampLabel.setForeground(sender.equals(username) ? new Color(200, 200, 200) : new Color(102, 102, 102));

        fileBubble.add(fileLabel);
        fileBubble.add(downloadButton);
        fileBubble.add(timestampLabel);
        messagePanel.add(fileBubble);
        messagePanel.add(Box.createRigidArea(new Dimension(0, 10)));

        messagePanel.revalidate();
        messagePanel.repaint();
    }

    private void sendMessage() {
        String message = messageInput.getText().trim();
        if (message.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Message cannot be empty.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try (Connection conn = DriverManager.getConnection(databaseUrl, "root", "Md1SuL81fahq1BP19Zau");
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO messages (sender, receiver, message, timestamp) VALUES (?, ?, ?, ?)")) {

            int selectedRow = userTable.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(frame, "No user selected.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            String receiver = (String) userTable.getValueAt(selectedRow, 0);
            ps.setString(1, username);
            ps.setString(2, receiver);
            ps.setString(3, message);
            ps.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
            ps.executeUpdate();

            messageInput.setText("");
            loadChat(receiver); // Refresh the chat window
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(frame, "Error sending message: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void sendFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select File to Upload");
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setAcceptAllFileFilterUsed(true);

        int returnValue = fileChooser.showOpenDialog(frame);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            String uniqueFileName = System.currentTimeMillis() + "_" + file.getName();

            new Thread(() -> {
                try (Connection conn = DriverManager.getConnection(databaseUrl, "root", "Md1SuL81fahq1BP19Zau");
                     PreparedStatement ps = conn.prepareStatement(
                             "INSERT INTO file_messages (sender, receiver, file_name, file_data, timestamp) VALUES (?, ?, ?, ?, ?)")) {

                    int selectedRow = userTable.getSelectedRow();
                    if (selectedRow == -1) {
                        JOptionPane.showMessageDialog(frame, "No user selected.", "Error", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    String receiver = (String) userTable.getValueAt(selectedRow, 0);
                    ps.setString(1, username);
                    ps.setString(2, receiver);
                    ps.setString(3, uniqueFileName);

                    // Read the entire file into a byte array
                    byte[] fileData = Files.readAllBytes(file.toPath());
                    ps.setBytes(4, fileData);

                    ps.setTimestamp(5, new Timestamp(System.currentTimeMillis()));
                    ps.executeUpdate();

                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(frame, "File sent successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                        loadChat(receiver);
                    });
                } catch (SQLException | IOException e) {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(frame, "Error sending file: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                        e.printStackTrace();
                    });
                }
            }).start();
        } else {
            System.out.println("File selection was cancelled.");
        }
    }

    private void downloadFile(String fileName) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save File");
        int userSelection = fileChooser.showSaveDialog(frame);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try (Connection conn = DriverManager.getConnection(databaseUrl, "root", "Md1SuL81fahq1BP19Zau");
                 PreparedStatement ps = conn.prepareStatement("SELECT file_data FROM file_messages WHERE file_name = ?")) {
                ps.setString(1, fileName);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        Blob blob = rs.getBlob("file_data");
                        try (InputStream is = blob.getBinaryStream();
                             FileOutputStream fos = new FileOutputStream(file)) {
                            byte[] buffer = new byte[4096];
                            int bytesRead;
                            while ((bytesRead = is.read(buffer)) != -1) {
                                fos.write(buffer, 0, bytesRead);
                            }
                            JOptionPane.showMessageDialog(frame, "File downloaded successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                        }
                    } else {
                        JOptionPane.showMessageDialog(frame, "File not found in database.", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            } catch (SQLException | IOException e) {
                JOptionPane.showMessageDialog(frame, "Error downloading file: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void removeFile(String fileName, String sender, Timestamp timestamp) {
        try (Connection conn = DriverManager.getConnection(databaseUrl, "root", "Md1SuL81fahq1BP19Zau");
             PreparedStatement ps = conn.prepareStatement("DELETE FROM file_messages WHERE file_name = ? AND sender = ? AND timestamp = ?")) {
            ps.setString(1, fileName);
            ps.setString(2, sender);
            ps.setTimestamp(3, timestamp);
            ps.executeUpdate();
            loadChat(sender); // Refresh the chat window
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(frame, "Error removing file: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showPopupMenu(MouseEvent e) {
        JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem removeItem = new JMenuItem("Remove");
        removeItem.addActionListener(ev -> {
            Component component = messagePanel.getComponentAt(e.getPoint());
            if (component instanceof JPanel) {
                JPanel messageBubble = (JPanel) component;
                String sender = (String) messageBubble.getClientProperty("sender");
                Timestamp timestamp = (Timestamp) messageBubble.getClientProperty("timestamp");
                if (sender != null && timestamp != null) {
                    removeMessage(sender, timestamp);
                }
            }
        });
        popupMenu.add(removeItem);
        popupMenu.show(messagePanel, e.getX(), e.getY());
    }

    private void removeMessage(String sender, Timestamp timestamp) {
        try (Connection conn = DriverManager.getConnection(databaseUrl, "root", "Md1SuL81fahq1BP19Zau");
             PreparedStatement ps = conn.prepareStatement("DELETE FROM messages WHERE sender = ? AND timestamp = ?")) {
            ps.setString(1, sender);
            ps.setTimestamp(2, timestamp);
            ps.executeUpdate();
            loadChat(sender); // Refresh the chat window
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(frame, "Error removing message: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void startMessageRefresh() {
        messageRefreshTimer = new Timer(5000, e -> {
            int selectedRow = userTable.getSelectedRow();
            if (selectedRow != -1) {
                String selectedUser = (String) userTable.getValueAt(selectedRow, 0);
                loadChat(selectedUser);
            }
        });
        messageRefreshTimer.start();
    }

    private void updateOnlineStatus(String status) {
        try (Connection conn = DriverManager.getConnection(databaseUrl, "root", "Md1SuL81fahq1BP19Zau");
             PreparedStatement ps = conn.prepareStatement("UPDATE users SET status = ? WHERE username = ?")) {
            ps.setString(1, status);
            ps.setString(2, username);
            ps.executeUpdate();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(frame, "Error updating status: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Inner class to represent chat entries
    private static class ChatEntry {
        String sender;
        String content;
        Timestamp timestamp;
        boolean isFileMessage;

        ChatEntry(String sender, String content, Timestamp timestamp, boolean isFileMessage) {
            this.sender = sender;
            this.content = content;
            this.timestamp = timestamp;
            this.isFileMessage = isFileMessage;
        }
    }
}