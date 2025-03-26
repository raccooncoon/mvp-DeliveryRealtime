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
    Long orderId = Long.valueOf(message.getPayload());
    sessionMap.put(orderId, session);

    Order order = orderStore.get(orderId);
    if (order != null) {
      session.sendMessage(new TextMessage(objectMapper.writeValueAsString(order)));
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