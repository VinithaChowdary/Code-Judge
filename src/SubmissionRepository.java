

import java.sql.*;// need to access the utility class

public class SubmissionRepository {

    public long saveSubmission(int problemId, String language, String sourceCode) throws Exception {

        String sql = "INSERT INTO submissions (problem_id, language, source_code, status) VALUES (?, ?, ?, ?)";

        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, problemId);
            ps.setString(2, language);
            ps.setString(3, sourceCode);
            ps.setString(4, "PENDING");

            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                return rs.getLong(1);
            }

            throw new RuntimeException("Failed to generate ID");
        }
    }
}