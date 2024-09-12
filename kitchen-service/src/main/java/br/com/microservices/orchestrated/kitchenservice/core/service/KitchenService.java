package br.com.microservices.orchestrated.kitchenservice.core.service;

import br.com.microservices.orchestrated.kitchenservice.config.exception.ValidationException;
import br.com.microservices.orchestrated.kitchenservice.core.dto.Event;
import br.com.microservices.orchestrated.kitchenservice.core.dto.History;
import br.com.microservices.orchestrated.kitchenservice.core.enums.EProductStatus;
import br.com.microservices.orchestrated.kitchenservice.core.model.Kitchen;
import br.com.microservices.orchestrated.kitchenservice.core.producer.KafkaProducer;
import br.com.microservices.orchestrated.kitchenservice.core.repository.KitchenRepository;
import br.com.microservices.orchestrated.kitchenservice.core.utils.JsonUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

import static br.com.microservices.orchestrated.kitchenservice.core.enums.ESagaStatus.*;

@Slf4j
@Service
@AllArgsConstructor
public class KitchenService {

    private static final String CURRENT_SOURCE = "KITCHEN_SERVICE";
    private final JsonUtil jsonUtil;
    private final KafkaProducer producer;
    private final KitchenRepository kitchenRepository;


    public void startOrderKitchen(Event event) {
        try {
            checkCurrentValidation(event);
            updateChicken(event);
            handleSuccess(event);
        } catch (Exception e) {
            log.error("Fail to update kitchen: ", e);
            handleFailCurrentNotExecuted(event, "Fail to update kitchen: ".concat(e.getMessage()));
        }
        producer.sendEvent(jsonUtil.toJson(event));
    }

    public void rollbackKitchen(Event event) {
        event.setStatus(FAIL);
        event.setSource(CURRENT_SOURCE);
        try {
            addHistory(event, "Rollback executed for kitchen!");
        } catch (Exception ex) {
            addHistory(event, ex.getMessage());
        }
        producer.sendEvent(jsonUtil.toJson(event));
    }

    private void updateChicken(Event event) {
        var kitchen = Kitchen.builder()
                .orderId(event.getPayload().getId())
                .transactionId(event.getTransactionId())
                .status(EProductStatus.FINISHED)
                .build();

        kitchenRepository.save(kitchen);
    }

    private void checkCurrentValidation(Event event) {
        if (kitchenRepository.existsByOrderIdAndTransactionId(
                event.getOrderId(), event.getTransactionId())) {
            throw new ValidationException("There's another transactionId for this validation.");
        }
    }

    private void handleSuccess(Event event) {
        event.setStatus(SUCCESS);
        event.setSource(CURRENT_SOURCE);
        addHistory(event, "Order finished successfully!");
    }

    private void addHistory(Event event, String message) {
        var history = History.builder()
                .source(event.getSource())
                .status(event.getStatus())
                .message(message)
                .createdAt(LocalDateTime.now())
                .build();

        event.addToHistory(history);
    }

    private void handleFailCurrentNotExecuted(Event event, String message) {
        event.setStatus(ROLLBACK_PENDING);
        event.setSource(CURRENT_SOURCE);
        addHistory(event, "Fail to update kitchen: ".concat(message));
    }

}
