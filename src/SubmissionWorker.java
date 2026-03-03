import com.rabbitmq.client.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class SubmissionWorker {

    private static final String QUEUE_NAME = "submission_queue";

     private static void updateSubmissionStatus(long id, String status, String output) throws Exception {
        String sql = "UPDATE submissions SET status = ?, output = ? WHERE id = ?";

        try (var conn = DatabaseUtil.getConnection();
             var ps = conn.prepareStatement(sql)) {

            ps.setString(1, status);
            ps.setString(2, output);
            ps.setLong(3, id);
            ps.executeUpdate();
        }
    }


    private static String executeInSandbox(String sourceCode, String input) throws Exception {

    String tempDir = "sandbox_" + System.currentTimeMillis();
    new File(tempDir).mkdir();

    Path sourceFile = Paths.get(tempDir, "Solution.java");
    Files.writeString(sourceFile, sourceCode);
    
    Path inputFile = Paths.get(tempDir, "input.txt");
    Files.writeString(inputFile, input);

    ProcessBuilder pb = new ProcessBuilder(
            "docker", "run",
            "--rm",
            "--network", "none",
            "--memory", "128m",
            "--cpus", "0.5",
            "-v", new File(tempDir).getAbsolutePath() + ":/app",
            "code-judge-sandbox"
    );

    pb.redirectErrorStream(true);

    Process process = pb.start();

    BufferedReader reader =
            new BufferedReader(
                    new InputStreamReader(process.getInputStream()));

    StringBuilder output = new StringBuilder();
    String line;

    while ((line = reader.readLine()) != null) {
        output.append(line).append("\n");
    }

    process.waitFor();

    // Cleanup temp folder
    Files.walk(Paths.get(tempDir))
            .sorted(Comparator.reverseOrder())
            .map(Path::toFile)
            .forEach(File::delete);

    return output.toString().trim();
}

    public static void start() throws Exception {

        Channel channel = RabbitMQUtil.getChannel();

        channel.queueDeclare(QUEUE_NAME,
                true,
                false,
                false,
                null);

        channel.basicQos(1);

        System.out.println("🟢 Worker waiting for messages...");

        DeliverCallback callback = (consumerTag, delivery) -> {

    String message =
            new String(delivery.getBody(), StandardCharsets.UTF_8);

    long submissionId = Long.parseLong(message);

    System.out.println("📥 Received submission ID: " + submissionId);

    try {
        SubmissionRepository repository = new SubmissionRepository();
        String sourceCode = repository.getSourceCodeById(submissionId);
        int problemId = repository.getProblemIdBySubmissionId(submissionId);
        List<TestCase> testCases = repository.getTestCasesByProblemId(problemId);

        String finalVerdict = "ACCEPTED";
        String finalOutput = "All test cases passed";

        for (TestCase tc : testCases) {
            String output = executeInSandbox(sourceCode, tc.getInput());

            if (output.contains("COMPILATION_ERROR")) {
                finalVerdict = "COMPILATION_ERROR";
                finalOutput = output;
                break;
            }

            if (output.contains("TIME_LIMIT_EXCEEDED")) {
                finalVerdict = "TIME_LIMIT_EXCEEDED";
                finalOutput = output;
                break;
            }

            if (output.contains("RUNTIME_ERROR")) {
                finalVerdict = "RUNTIME_ERROR";
                finalOutput = output;
                break;
            }

            if (!output.equals(tc.getExpectedOutput())) {
                finalVerdict = "WRONG_ANSWER";
                finalOutput = "Expected: " + tc.getExpectedOutput() + " \nGot: " + output;
                break;
            }
        }

        updateSubmissionStatus(submissionId, finalVerdict, finalOutput);

        channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);

        System.out.println("✅ Processed submission: " + submissionId + " Verdict: " + finalVerdict);

    } catch (Exception e) {
        e.printStackTrace();
    }
};

        channel.basicConsume(QUEUE_NAME,
                false, // auto-ack
                callback,
                consumerTag -> {});
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

   
}