package com.biedron.payments.customer;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CustomerDto {

    Long customerId;
    String customerEmail;
    String customerName;

}

