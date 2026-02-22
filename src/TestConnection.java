import java.sql.Connection;


public class TestConnection {

    public static void main(String[] args) {
        try (Connection conn = DatabaseUtil.getConnection()) {
            System.out.println("✅ Database Connected Successfully!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}