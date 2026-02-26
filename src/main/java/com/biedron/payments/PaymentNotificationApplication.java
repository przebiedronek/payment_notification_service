package com.biedron.payments;

import com.biedron.payments.customer.CustomerRepository;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableJpaRepositories(
        basePackages = "com.biedron.payments.customer",
        includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                CustomerRepository.class
        })
)
@EntityScan(basePackages = {
		"com.biedron.payments.customer"
})
public class PaymentNotificationApplication {

	public static void main(String[] args) {
		SpringApplication.run(PaymentNotificationApplication.class, args);
	}

}
