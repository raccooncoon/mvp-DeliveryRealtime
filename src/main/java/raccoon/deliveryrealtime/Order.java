package raccoon.deliveryrealtime;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Order {

  private Long id;
  private String customerId;
  private OrderStatus status;
  private LocalDateTime updatedAt;

}
