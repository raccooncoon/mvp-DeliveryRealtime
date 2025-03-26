package raccoon.deliveryrealtime;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SampleDataLoader {

  @Bean
  public CommandLineRunner loadSampleOrders(ConcurrentMap<Long, Order> orderStore) {
    return args -> {
      AtomicLong idGen = new AtomicLong(1000);

      Order order1 = Order.builder()
          .id(idGen.getAndIncrement())
          .customerId("user01")
          .status(OrderStatus.ORDERED)
          .updatedAt(LocalDateTime.now())
          .build();

      Order order2 = Order.builder()
          .id(idGen.getAndIncrement())
          .customerId("user02")
          .status(OrderStatus.PAID)
          .updatedAt(LocalDateTime.now())
          .build();

      Order order3 = Order.builder()
          .id(idGen.getAndIncrement())
          .customerId("user03")
          .status(OrderStatus.DELIVERING)
          .updatedAt(LocalDateTime.now())
          .build();

      orderStore.put(order1.getId(), order1);
      orderStore.put(order2.getId(), order2);
      orderStore.put(order3.getId(), order3);

      System.out.println("✅ 샘플 주문 데이터 등록 완료");
    };
  }
}