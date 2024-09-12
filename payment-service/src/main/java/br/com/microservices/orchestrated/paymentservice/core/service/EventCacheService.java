package br.com.microservices.orchestrated.paymentservice.core.service;

import br.com.microservices.orchestrated.paymentservice.core.dto.Event;
import lombok.AllArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@AllArgsConstructor
public class EventCacheService {
    private final Map<String, Event> eventStore = new HashMap<>();

    @Cacheable(value = "events", key = "#orderId")
    public Event getEventByOrderId(String orderId) {
        // Simula a recuperação do objeto a partir de uma fonte de dados
        return eventStore.get(orderId);
    }

    @CacheEvict(value = "events", key = "#orderId")
    public void removeEventByOrderId(String orderId) {
        eventStore.remove(orderId);
    }

    @CacheEvict(value = "events", allEntries = true)
    public void clearAllEvents() {}

    public void saveEvent(Event event) {
        eventStore.put(event.getOrderId(), event);
    }
}
