package br.com.microservices.orchestrated.orderservice.core.service;

import br.com.caelum.stella.validation.CPFValidator;
import br.com.microservices.orchestrated.orderservice.config.exception.ValidationException;
import br.com.microservices.orchestrated.orderservice.core.document.Customer;
import br.com.microservices.orchestrated.orderservice.core.dto.CustomerRequest;
import br.com.microservices.orchestrated.orderservice.core.repository.CustomerRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@AllArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;

    public Customer findByCpf(String cpf) {
        validateCpf(cpf);
        return customerRepository.findByCpfAndIsActiveTrue(cpf).orElseThrow(() -> new ValidationException("customer not found by cpf"));
    }


    public Customer createCustomer(CustomerRequest customerRequest) {
        validateCpf(customerRequest.getCpf());
        validateCustomerCreated(customerRequest.getCpf());

        Customer customer = Customer
                .builder()
                .id(UUID.randomUUID().toString())
                .name(customerRequest.getName())
                .cpf(customerRequest.getCpf())
                .phone(customerRequest.getPhone())
                .address(customerRequest.getAddress())
                .build();

        return customerRepository.save(customer);
    }

    public void disableCustomerByCpf(String cpf) {
        validateCpf(cpf);
        var customer = findByCpf(cpf);
        customer.setIsActive(false);
        save(customer);
    }


    private void validateCpf(String cpf) {
        CPFValidator cpfValidator = new CPFValidator();
        cpfValidator.assertValid(cpf);
    }

    private void validateCustomerCreated(String cpf) {
        if(customerRepository.existsByCpf(cpf)){
            throw new ValidationException("Already registered customer by cpf: ".concat(cpf));
        }
    }

    private void save(Customer customer) {
        customerRepository.save(customer);
    }

}
