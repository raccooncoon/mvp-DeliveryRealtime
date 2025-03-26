package raccoon.deliveryrealtime;

import lombok.Builder;
import lombok.Data;

@Data
public class OrderUpdateMessage {
  private Long orderId;
  private OrderStatus status;
  private java.time.LocalDateTime updatedAt;
  private OrderMessageType type;

}
