package raccoon.deliveryrealtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class OrderWebSocketHandler extends TextWebSocketHandler {

  private final ConcurrentMap<Long, Order> orderStore;
  private final Map<Long, WebSocketSession> sessionMap = new ConcurrentHashMap<>();
  private final ObjectMapper objectMapper = new ObjectMapper()
      .registerModule(new JavaTimeModule())
      .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

  public OrderWebSocketHandler(ConcurrentMap<Long, Order> orderStore) {
    this.orderStore = orderStore;
  }

  @Override
  protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
    String payload = message.getPayload();
    try {
      if (payload.startsWith("{")) {
        // JSON 메시지 → 상태 변경
        OrderUpdateMessage update = objectMapper.readValue(payload, OrderUpdateMessage.class);
        Order order = orderStore.get(update.getOrderId());
        if (order != null) {
          order.setStatus(update.getStatus());
          order.setUpdatedAt(update.getUpdatedAt());
          notifyOrderUpdate(order);
        }
      } else {
        // 문자열 → 주문 ID로 구독
        Long orderId = Long.valueOf(payload);
        sessionMap.put(orderId, session);
        Order order = orderStore.get(orderId);
        if (order != null) {
          session.sendMessage(new TextMessage(objectMapper.writeValueAsString(order)));
        }
      }
    } catch (Exception e) {
      session.sendMessage(new TextMessage("{\"error\": \"Invalid message format\"}"));
    }
  }

  public void notifyOrderUpdate(Order order) {
    WebSocketSession session = sessionMap.get(order.getId());
    if (session != null && session.isOpen()) {
      try {
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(order)));
      } catch (IOException e) {
        sessionMap.remove(order.getId());
      }
    }
  }
}