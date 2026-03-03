import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;

public class RabbitMQUtil {

    private static final String HOST = "localhost";

    public static Channel getChannel() throws Exception {

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(HOST);

        Connection connection = factory.newConnection();
        return connection.createChannel();
    }
}