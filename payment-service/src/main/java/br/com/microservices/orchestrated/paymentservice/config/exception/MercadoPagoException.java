package br.com.microservices.orchestrated.paymentservice.config.exception;

public class MercadoPagoException extends RuntimeException {
    public MercadoPagoException(String message) {
        super(message);
    }
}
