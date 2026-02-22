

public class SubmissionService {

    private SubmissionRepository repository = new SubmissionRepository();

    public long submit(int problemId, String language, String sourceCode) throws Exception {

        // Step 1: Save in DB
        long submissionId =
                repository.saveSubmission(problemId, language, sourceCode);

        // Step 2: Push to Redis queue
        QueueUtil.push(submissionId);

        return submissionId;
    }
}