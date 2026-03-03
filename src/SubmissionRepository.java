

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SubmissionRepository {

    public String getSubmissionStatusAndOutput(long submissionId) throws Exception {
        String sql = "SELECT status, output FROM submissions WHERE id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, submissionId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String status = rs.getString("status");
                String output = rs.getString("output");
                if (output == null) output = "";
                
                // Escape JSON string for simple payload
                output = output.replace("\\", "\\\\")
                               .replace("\"", "\\\"")
                               .replace("\n", "\\n")
                               .replace("\r", "");
                
                return "{ \"id\": " + submissionId + ", \"status\": \"" + status + "\", \"output\": \"" + output + "\" }";
            }
            return "{ \"error\": \"Submission not found\" }";
        }
    }

    public int getProblemIdBySubmissionId(long submissionId) throws Exception {
        String sql = "SELECT problem_id FROM submissions WHERE id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, submissionId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("problem_id");
            }
            throw new RuntimeException("Submission not found");
        }
    }

    public List<TestCase> getTestCasesByProblemId(int problemId) throws Exception {
        List<TestCase> list = new ArrayList<>();
        String sql = "SELECT input, expected_output FROM test_cases WHERE problem_id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, problemId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new TestCase(
                    rs.getString("input"),
                    rs.getString("expected_output")
                ));
            }
        }
        return list;
    }

    public String getSourceCodeById(long submissionId) throws Exception {

    String sql = "SELECT source_code FROM submissions WHERE id = ?";

    try (Connection conn = DatabaseUtil.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {

        ps.setLong(1, submissionId);

        ResultSet rs = ps.executeQuery();

        if (rs.next()) {
            return rs.getString("source_code");
        }

        throw new RuntimeException("Submission not found");
    }
}

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