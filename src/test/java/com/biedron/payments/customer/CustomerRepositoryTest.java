package com.biedron.payments.customer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for CustomerRepository.
 * Uses @DataJpaTest to set up an in-memory database for testing.
 */
@DataJpaTest
@ActiveProfiles("test")
class CustomerRepositoryTest {

    @Autowired
    private CustomerRepository customerRepository;

    private Customer testCustomer;

    @BeforeEach
    void setUp() {
        // Clean up before each test
        customerRepository.deleteAll();

        // Create a test customer
        testCustomer = new Customer();
        testCustomer.setId(123L);
        testCustomer.setEmail("john.doe@example.com");
        testCustomer.setName("John Doe");
        testCustomer.setCreatedAt(Instant.now());
        testCustomer.setUpdatedAt(Instant.now());
    }

    @Test
    void testSaveAndFindById() {
        customerRepository.save(testCustomer);

        Optional<Customer> customer = customerRepository.findById(123L);

        assertThat(customer).isPresent();
        assertThat(customer.get().getId()).isEqualTo(123L);
        assertThat(customer.get().getEmail()).isEqualTo("john.doe@example.com");
        assertThat(customer.get().getName()).isEqualTo("John Doe");
    }

}

