import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    private static final String URL = "jdbc:mysql://filetransfer.cdwcai6qo2dm.eu-north-1.rds.amazonaws.com:3306/file_transfer";
    private static final String USER = "root";
    private static final String PASSWORD = "Md1SuL81fahq1BP19Zau";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
