package br.com.microservices.orchestrated.paymentservice.core.service;

import br.com.microservices.orchestrated.paymentservice.config.exception.MercadoPagoException;
import br.com.microservices.orchestrated.paymentservice.config.exception.ValidationException;
import br.com.microservices.orchestrated.paymentservice.config.mercadopago.MercadoPagoProperties;
import br.com.microservices.orchestrated.paymentservice.config.system.SystemProperties;
import br.com.microservices.orchestrated.paymentservice.core.dto.Event;
import br.com.microservices.orchestrated.paymentservice.core.dto.History;
import br.com.microservices.orchestrated.paymentservice.core.dto.OrderProducts;
import br.com.microservices.orchestrated.paymentservice.core.dto.PaymentResponseDTO;
import br.com.microservices.orchestrated.paymentservice.core.enums.EPaymentStatus;
import br.com.microservices.orchestrated.paymentservice.core.model.Payment;
import br.com.microservices.orchestrated.paymentservice.core.producer.KafkaProducer;
import br.com.microservices.orchestrated.paymentservice.core.repository.PaymentRepository;
import br.com.microservices.orchestrated.paymentservice.core.utils.JsonUtil;
import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.preference.PreferenceBackUrlsRequest;
import com.mercadopago.client.preference.PreferenceClient;
import com.mercadopago.client.preference.PreferenceItemRequest;
import com.mercadopago.client.preference.PreferenceRequest;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.preference.Preference;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static br.com.microservices.orchestrated.paymentservice.core.enums.EPaymentStatus.PENDING;
import static br.com.microservices.orchestrated.paymentservice.core.enums.EPaymentStatus.REFUND;
import static br.com.microservices.orchestrated.paymentservice.core.enums.ESagaStatus.*;

@Slf4j
@Service
@AllArgsConstructor
public class PaymentService {

    private final MercadoPagoProperties mercadoPagoProperties;
    private final SystemProperties systemProperties;

    private static final String CURRENT_SOURCE = "PAYMENT_SERVICE";
    private static final Double REDUCE_SUM_VALUE = 0.0;
    private static final Double MIN_AMOUNT_VALUE = 0.1;

    private final JsonUtil jsonUtil;
    private final KafkaProducer producer;
    private final PaymentRepository paymentRepository;
    private final PreferenceClient preferenceClient;
    private final EventCacheService eventCacheService;

    public void realizePayment(Event event) {
        try {
        checkCurrentValidation(event);
        createPendingPayment(event);
        var payment = findByOrderIdAndTransactionId(event);
        validateAmount(payment.getTotalAmount());
        processPayment(event, payment);
        eventCacheService.saveEvent(event);

        } catch (Exception ex) {
            generateRefund(ex.getMessage(), event);
        }
    }

    public void realizeRefund(Event event) {
        event.setStatus(FAIL);
        event.setSource(CURRENT_SOURCE);
        try {
            changePaymentStatusToRefund(event);
            addHistory(event, "Rollback executed for payment!");
        } catch (Exception ex) {
            addHistory(event, "Rollback not executed for payment: ".concat(ex.getMessage()));
        }
        producer.sendEvent(jsonUtil.toJson(event));

    }

    public PaymentResponseDTO processPayment(Event event, Payment payment) {

        try {
            MercadoPagoConfig.setAccessToken(mercadoPagoProperties.getAccessToken());

            List<PreferenceItemRequest> items = new ArrayList<>();

            event.getPayload().getProducts().forEach(product -> {

                PreferenceItemRequest itemRequest =
                        PreferenceItemRequest.builder()
                                .id(product.getProduct().getCode())
                                .title(product.getProduct().getCode())
                                .description(product.getProduct().getDescription())
                                .quantity(product.getQuantity())
                                .currencyId("BRL")
                                .unitPrice(BigDecimal.valueOf(product.getProduct().getUnitValue()))
                                .build();

                items.add(itemRequest);
            });

            PreferenceRequest preferenceRequest = PreferenceRequest.builder()
                    .backUrls(
                            PreferenceBackUrlsRequest.builder()
                                    .success(getUrlRedirectCheckout(EPaymentStatus.SUCCESS, event))
                                    .failure(getUrlRedirectCheckout(REFUND, event))
                                    .pending(getUrlRedirectCheckout(PENDING, event))
                                    .build())
                    .expires(false)
                    .autoReturn("all")
                    .binaryMode(true)
                    .operationType("regular_payment")
                    .items(items)
                    .build();

            Preference preference = preferenceClient.create(preferenceRequest);

            log.info(preference.getInitPoint());
            payment.setInitPoint(preference.getInitPoint());
            save(payment);

            return PaymentResponseDTO.builder()
                    .items(items)
                    .initPoint(preference.getInitPoint())
                    .build();

        } catch (MPApiException apiException) {
            generateRefund(apiException.getMessage(), event);
            throw new MercadoPagoException(apiException.getApiResponse().getContent());
        } catch (MPException exception) {
            generateRefund(exception.getMessage(), event);
            throw new MercadoPagoException(exception.getMessage());
        }
    }

    public void checkoutPayment(String orderId, EPaymentStatus status) {
        Event event = eventCacheService.getEventByOrderId(orderId);

        if (!status.equals(EPaymentStatus.SUCCESS)) {
            generateRefund("not authorized", event);
            throw new ValidationException("Payment not authorized");
        }
        try {
            var payment = findByOrderId(orderId);
            changePaymentToSuccess(payment);
            handleSuccess(event);
            producer.sendEvent(jsonUtil.toJson(event));
            eventCacheService.clearAllEvents();
        } catch (ValidationException e) {
            generateRefund(e.getMessage(), event);
        }

    }

    private void generateRefund(String messageFail, Event event) {
        log.error("Error trying to make payment: ", messageFail);
        handleFailCurrentNotExecuted(event ,messageFail);
        eventCacheService.clearAllEvents();
        realizeRefund(event);
    }

    private void changePaymentStatusToRefund(Event event) {
        var payment = findByOrderIdAndTransactionId(event);
        payment.setStatus(REFUND);
        setEventAmountItems(event, payment);
        save(payment);
    }

    private void checkCurrentValidation(Event event) {
        if(Boolean.TRUE.equals(paymentRepository.existsByOrderIdAndTransactionId(
                event.getOrderId(), event.getTransactionId()))) {
            throw new ValidationException("There's another transactionId for this validation.");
        }
    }

    private void createPendingPayment(Event event) {

        var totalAmount = calculateAmount(event);
        var totalItems = calculateTotalItems(event);

        var payment = Payment.builder()
                .orderId(event.getPayload().getId())
                .transactionId(event.getTransactionId())
                .totalAmount(totalAmount)
                .totalItems(totalItems)
                .build();

        save(payment);
        setEventAmountItems(event, payment);
    }

    private double calculateAmount(Event event) {
        return event.getPayload().getProducts().stream()
                .map(product -> product.getQuantity() * product.getProduct().getUnitValue())
                .reduce(REDUCE_SUM_VALUE, Double::sum);
    }

    private Integer calculateTotalItems(Event event) {
        return event.getPayload().getProducts().stream()
                .map(OrderProducts::getQuantity)
                .reduce(REDUCE_SUM_VALUE.intValue(), Integer::sum);
    }

    private void setEventAmountItems(Event event, Payment payment) {
        event.getPayload().setTotalAmount(payment.getTotalAmount());
        event.getPayload().setTotalItems(payment.getTotalItems());
    }

    private void validateAmount(double amount) {
        if(amount < MIN_AMOUNT_VALUE) {
            throw new ValidationException("The minimun amount available is: ".concat(MIN_AMOUNT_VALUE.toString()));
        }
    }

    private void changePaymentToSuccess(Payment payment) {
        payment.setStatus(EPaymentStatus.SUCCESS);
        save(payment);
    }

    private void handleSuccess(Event event) {
        event.setStatus(SUCCESS);
        event.setSource(CURRENT_SOURCE);
        addHistory(event, "Payment realized successfully!");
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
        addHistory(event, "Fail to realize payment: ".concat(message));
    }

    private Payment findByOrderIdAndTransactionId(Event event) {
        return paymentRepository
                .findByOrderIdAndTransactionId(event.getPayload().getId(), event.getTransactionId())
                .orElseThrow(() -> new ValidationException("Payment not found by OrderId And TransactionId"));
    }

    public Payment findByOrderId(String orderId) {
        return paymentRepository
                .findByOrderId(orderId)
                .orElseThrow(() -> new ValidationException("Payment not found by OrderId"));
    }
    private void save(Payment payment) {
        paymentRepository.save(payment);
    }

    private String getUrlRedirectCheckout(EPaymentStatus status, Event event) {
       return systemProperties.getUrl()
                .concat("/api/checkout/")
               .concat(event.getPayload().getId())
               .concat("/").concat(status.name());
    }

}
