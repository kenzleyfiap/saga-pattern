package br.com.microservices.orchestrated.kitchenservice.core.repository;

import br.com.microservices.orchestrated.kitchenservice.core.model.Kitchen;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KitchenRepository extends JpaRepository<Kitchen, Integer> {
    boolean existsByOrderIdAndTransactionId(String orderId, String transactionId);
}
