import java.sql.Connection;
import java.sql.DriverManager;

public class DatabaseUtil {

    private static final String URL =
            "jdbc:postgresql://localhost:5432/code_judge";
    private static final String USER = "vinithachowdary";
    private static final String PASSWORD = "mypassword";

    public static Connection getConnection() throws Exception {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}