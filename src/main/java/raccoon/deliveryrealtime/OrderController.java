package raccoon.deliveryrealtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

@RestController
@RequestMapping("orders")
public class OrderController {

  private final ConcurrentMap<Long, Order> orderStore;
  private final AtomicLong idGenerator = new AtomicLong(1003);
  private final Map<Long, List<SseEmitter>> emitterMap = new ConcurrentHashMap<>();
  private final Map<Long, List<WebSocketSession>> sessionMap = new ConcurrentHashMap<>();
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final OrderWebSocketHandler orderWebSocketHandler;

  public OrderController(ConcurrentMap<Long, Order> orderStore, OrderWebSocketHandler orderWebSocketHandler) {
    this.orderStore = orderStore;
    this.orderWebSocketHandler = orderWebSocketHandler;
  }

  @PostMapping
  public Order createOrder(@RequestParam String customerId) {
    long id = idGenerator.getAndIncrement();
    Order order = Order.builder()
        .id(id)
        .customerId(customerId)
        .status(OrderStatus.ORDERED)
        .updatedAt(LocalDateTime.now())
        .build();
    orderStore.put(id, order);
    return order;
  }

  @GetMapping
  public Collection<Order> listOrders() {
    return orderStore.values();
  }

  @GetMapping("/{id}")
  public Order getOrder(@PathVariable Long id) {
    return orderStore.get(id);
  }

  @PatchMapping("/{id}/status")
  public Order updateStatus(@PathVariable Long id, @RequestParam OrderStatus status) {
    Order order = orderStore.get(id);
    if (order != null) {
      order.setStatus(status);
      order.setUpdatedAt(LocalDateTime.now());

      List<SseEmitter> emitters = emitterMap.getOrDefault(id, new ArrayList<>());
      List<SseEmitter> deadEmitters = new ArrayList<>();
      for (SseEmitter emitter : emitters) {
        try {
          emitter.send(order);
        } catch (IOException e) {
          emitter.completeWithError(e);
          deadEmitters.add(emitter);
        }
      }
      emitters.removeAll(deadEmitters);

      List<WebSocketSession> sessions = sessionMap.getOrDefault(id, new ArrayList<>());
      List<WebSocketSession> deadSessions = new ArrayList<>();

      for (WebSocketSession session : sessions) {
        if (session != null && session.isOpen()) {
          try {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(order)));
          } catch (IOException e) {
            deadSessions.add(session);
          }
        } else {
          deadSessions.add(session);
        }
      }

      sessions.removeAll(deadSessions);

      orderWebSocketHandler.notifyOrderUpdate(order);
    }
    return order;
  }

  @GetMapping(value = "/subscribe/{orderId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter subscribe(@PathVariable Long orderId) {
    SseEmitter emitter = new SseEmitter(0L);
    emitterMap.computeIfAbsent(orderId, k -> new ArrayList<>()).add(emitter);
    emitter.onCompletion(() -> removeEmitter(orderId, emitter));
    emitter.onTimeout(() -> removeEmitter(orderId, emitter));
    emitter.onError(e -> removeEmitter(orderId, emitter));
    Order order = orderStore.get(orderId);
    if (order != null) {
      try {
        emitter.send(order);
      } catch (IOException e) {
        emitter.completeWithError(e);
      }
    }
    return emitter;
  }

  private void removeEmitter(Long orderId, SseEmitter emitter) {
    List<SseEmitter> emitters = emitterMap.get(orderId);
    if (emitters != null) {
      emitters.remove(emitter);
    }
  }
}