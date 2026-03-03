public class SubmissionService {

    private SubmissionRepository repository = new SubmissionRepository();

    public String getSubmission(long id) throws Exception {
        return repository.getSubmissionStatusAndOutput(id);
    }

    public long submit(int problemId, String language, String sourceCode) throws Exception {

        long submissionId =
                repository.saveSubmission(problemId, language, sourceCode);

        SubmissionProducer.publish(submissionId);

        return submissionId;
    }
}