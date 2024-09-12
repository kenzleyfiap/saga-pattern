package br.com.microservices.orchestrated.orderservice.core.controller;

import br.com.microservices.orchestrated.orderservice.core.document.Customer;
import br.com.microservices.orchestrated.orderservice.core.dto.CustomerRequest;
import br.com.microservices.orchestrated.orderservice.core.service.CustomerService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@AllArgsConstructor
@RequestMapping("/api/customer")
public class CustomerController {

    private final CustomerService customerService;

    @PostMapping
    public Customer createCustomer(@RequestBody CustomerRequest customerRequest) {
        return customerService.createCustomer(customerRequest);
    }

    @GetMapping("{cpf}")
    public Customer findByCpf(@PathVariable String cpf) {
        return customerService.findByCpf(cpf);
    }

    @PutMapping("{cpf}")
    public String disableCustomer(@PathVariable String cpf) {
        customerService.disableCustomerByCpf(cpf);
        return "Client successfully deactivated";
    }
}
