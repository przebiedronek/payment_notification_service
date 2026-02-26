package com.biedron.payments.customer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import static net.logstash.logback.argument.StructuredArguments.kv;

@Slf4j
@RequiredArgsConstructor
@Service
public class DatabaseCustomerService implements CustomerService {

    private final CustomerRepository customerRepository;

    @Override
    public CustomerDto getCustomerData(Long customerId) {
        log.info("Fetching customer data from database", kv("customer_id", customerId));

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> {
                    log.error("Customer not found", kv("customer_id", customerId));
                    return new CustomerNotFoundException("Customer not found: " + customerId);
                });

        log.info("Successfully fetched customer data from database",
                kv("customer_id", customerId),
                kv("customer_email", customer.getEmail()),
                kv("customer_name", customer.getName()));

        return CustomerDto.builder()
                .customerId(customer.getId())
                .customerEmail(customer.getEmail())
                .customerName(customer.getName())
                .build();
    }
}

