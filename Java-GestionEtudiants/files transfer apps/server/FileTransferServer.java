import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.*;

public class FileTransferServer {
    private int port;
    private AdminDashboard adminDashboard;
    private Map<String, Socket> connectedClients = new HashMap<>(); // Track connected clients

    public FileTransferServer(int port) {
        this.port = port;
        try {
            this.adminDashboard = new AdminDashboard(port);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void start() {
        System.out.println("Server started on port: " + port);
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected: " + clientSocket.getInetAddress());

                // Handle client in a separate thread
                new Thread(() -> handleClient(clientSocket)).start();
            }
        } catch (IOException e) {
            System.err.println("Error starting server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleClient(Socket clientSocket) {
        try (DataInputStream dis = new DataInputStream(clientSocket.getInputStream());
             DataOutputStream dos = new DataOutputStream(clientSocket.getOutputStream())) {

            // Read the initial login message
            String operation = dis.readUTF();
            if ("LOGIN".equals(operation)) {
                String username = dis.readUTF();
                connectedClients.put(username, clientSocket); // Add client to the connected list
                updateUserStatus(username, "Online"); // Update user status to online
                System.out.println(username + " is now online.");

                // Listen for client messages
                while (true) {
                    String messageType = dis.readUTF();
                    if ("NEW_MESSAGE".equals(messageType)) {
                        String receiver = dis.readUTF();
                        String message = dis.readUTF();
                        saveMessageToDatabase(username, receiver, message); // Save message to database
                        notifyNewMessage(receiver, message); // Notify the receiver
                    } else if ("TYPING".equals(messageType)) {
                        String receiver = dis.readUTF();
                        boolean isTyping = dis.readBoolean();
                        broadcastTyping(username, receiver, isTyping); // Broadcast typing indicator
                    } else if ("LOGOUT".equals(messageType)) {
                        updateUserStatus(username, "Offline"); // Update user status to offline
                        connectedClients.remove(username); // Remove client from the connected list
                        System.out.println(username + " is now offline.");
                        break; // Exit the loop and close the connection
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Client disconnected: " + e.getMessage());
            // Remove the client from the connected list and update status to offline
            for (Map.Entry<String, Socket> entry : connectedClients.entrySet()) {
                if (entry.getValue().isClosed()) {
                    String username = entry.getKey();
                    connectedClients.remove(username);
                    updateUserStatus(username, "Offline");
                    System.out.println(username + " is now offline.");
                }
            }
        }
    }

    private void notifyNewMessage(String receiver, String message) {
        Socket receiverSocket = connectedClients.get(receiver);
        if (receiverSocket != null) {
            try {
                DataOutputStream dos = new DataOutputStream(receiverSocket.getOutputStream());
                dos.writeUTF("NEW_MESSAGE");
                dos.writeUTF(message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void broadcastTyping(String sender, String receiver, boolean isTyping) {
        Socket receiverSocket = connectedClients.get(receiver);
        if (receiverSocket != null) {
            try {
                DataOutputStream dos = new DataOutputStream(receiverSocket.getOutputStream());
                dos.writeUTF("TYPING");
                dos.writeUTF(sender);
                dos.writeBoolean(isTyping);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void updateUserStatus(String username, String status) {
        try (Connection conn = DriverManager.getConnection("jdbc:mysql://filetransfer.cdwcai6qo2dm.eu-north-1.rds.amazonaws.com:3306/file_transfer", "root", "Md1SuL81fahq1BP19Zau");
             PreparedStatement ps = conn.prepareStatement("UPDATE users SET status = ? WHERE username = ?")) {
            ps.setString(1, status);
            ps.setString(2, username);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error updating user status: " + e.getMessage());
        }
    }

    private void saveMessageToDatabase(String sender, String receiver, String message) {
        try (Connection conn = DriverManager.getConnection("jdbc:mysql://filetransfer.cdwcai6qo2dm.eu-north-1.rds.amazonaws.com:3306/file_transfer", "root", "Md1SuL81fahq1BP19Zau");
             PreparedStatement ps = conn.prepareStatement("INSERT INTO messages (sender, receiver, message, timestamp) VALUES (?, ?, ?, ?)")) {
            ps.setString(1, sender);
            ps.setString(2, receiver);
            ps.setString(3, message);
            ps.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error saving message to database: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        new FileTransferServer(8080).start(); // Start the server on port 8080
    }
}