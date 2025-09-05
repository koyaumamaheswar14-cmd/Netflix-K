import java.sql.*;

public class DBUtil {
    private static final String URL = "jdbc:mysql://localhost:3306/netflix_clone";
    private static final String USER = "root"; // replace with your MySQL username
    private static final String PASS = "uma123"; // replace with your MySQL password

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }
}
