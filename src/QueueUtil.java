
import redis.clients.jedis.Jedis;

public class QueueUtil {
   private static final String QUEUE_KEY = "submission_queue";

   public QueueUtil() {
   }

   public static void push(long var0) {
      Jedis var2 = RedisUtil.getClient();

      try {
         var2.lpush("submission_queue", new String[]{String.valueOf(var0)});
      } catch (Throwable var6) {
         if (var2 != null) {
            try {
               var2.close();
            } catch (Throwable var5) {
               var6.addSuppressed(var5);
            }
         }

         throw var6;
      }

      if (var2 != null) {
         var2.close();
      }

   }
}
