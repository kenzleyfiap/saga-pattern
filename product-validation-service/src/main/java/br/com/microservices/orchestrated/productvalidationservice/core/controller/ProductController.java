package br.com.microservices.orchestrated.productvalidationservice.core.controller;

import br.com.microservices.orchestrated.productvalidationservice.core.model.Product;
import br.com.microservices.orchestrated.productvalidationservice.core.service.ProductValidationService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/api/product")
public class ProductController {

    private final ProductValidationService productValidationService;

    @GetMapping("/menu")
    public ResponseEntity<List<Product>> findAll() {
        List<Product> products = productValidationService.findAll();
        return ResponseEntity.ok(products);
    }

}
