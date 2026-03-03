import com.rabbitmq.client.Channel;

public class SubmissionProducer {

    private static final String QUEUE_NAME = "submission_queue";

    public static void publish(long submissionId) throws Exception {

        Channel channel = RabbitMQUtil.getChannel();

        // Declare queue (creates if not exists)
        channel.queueDeclare(QUEUE_NAME,
                true,  // durable
                false,
                false,
                null);

        String message = String.valueOf(submissionId);

        channel.basicPublish("",
                QUEUE_NAME,
                null,
                message.getBytes());

        System.out.println("📤 Sent to RabbitMQ: " + message);

        channel.close();
    }
}