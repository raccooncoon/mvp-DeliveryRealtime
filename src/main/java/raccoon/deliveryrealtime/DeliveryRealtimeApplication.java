package raccoon.deliveryrealtime;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class DeliveryRealtimeApplication {

  public static void main(String[] args) {
    SpringApplication.run(DeliveryRealtimeApplication.class, args);
  }

  // 주문 상태를 메모리에서 관리할 수 있도록 설정
  @Bean
  public ConcurrentMap<Long, Order> orderStore() {
    return new ConcurrentHashMap<>();
  }

}
