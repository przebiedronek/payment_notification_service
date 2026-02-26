package com.biedron.payments.customer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Unit test for DatabaseCustomerService.
 * Uses Mockito to mock the CustomerRepository dependency.
 */
@ExtendWith(MockitoExtension.class)
class DatabaseCustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private DatabaseCustomerService customerService;

    private Customer testCustomer;

    @BeforeEach
    void setUp() {
        testCustomer = new Customer();
        testCustomer.setId(123L);
        testCustomer.setEmail("john.doe@example.com");
        testCustomer.setName("John Doe");
        testCustomer.setCreatedAt(Instant.now());
        testCustomer.setUpdatedAt(Instant.now());
    }

    @Test
    void testGetCustomerData_Success() {
        // Given
        when(customerRepository.findById(123L)).thenReturn(Optional.of(testCustomer));

        // When
        CustomerDto result = customerService.getCustomerData(123L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getCustomerId()).isEqualTo(123L);
        assertThat(result.getCustomerEmail()).isEqualTo("john.doe@example.com");
        assertThat(result.getCustomerName()).isEqualTo("John Doe");

        verify(customerRepository, times(1)).findById(123L);
    }

    @Test
    void testGetCustomerData_CustomerNotFound() {
        // Given
        when(customerRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> customerService.getCustomerData(999L))
                .isInstanceOf(CustomerNotFoundException.class)
                .hasMessageContaining("Customer not found: 999");

        verify(customerRepository, times(1)).findById(999L);
    }

    @Test
    void testGetCustomerData_RepositoryThrowsException() {
        // Given
        when(customerRepository.findById(123L))
                .thenThrow(new RuntimeException("Database connection error"));

        // When & Then
        assertThatThrownBy(() -> customerService.getCustomerData(123L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Database connection error");

        verify(customerRepository, times(1)).findById(123L);
    }
}

