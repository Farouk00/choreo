package com.orderplatform.inventory.config;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.orderplatform.inventory.domain.Product;
import com.orderplatform.inventory.domain.ProductRepository;

@Configuration
@ConditionalOnProperty(name = "inventory.seed-demo-data", havingValue = "true")
public class DemoDataSeeder {

    @Bean
    ApplicationRunner seedProducts(ProductRepository products) {
        return args -> {
            if (products.count() == 0) {
                products.save(new Product("KEYBOARD-01", "Mechanical keyboard", 50));
                products.save(new Product("MOUSE-01", "Wireless mouse", 100));
                products.save(new Product("MONITOR-27", "27-inch monitor", 3));
            }
        };
    }
}
