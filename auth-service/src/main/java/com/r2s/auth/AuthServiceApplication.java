package com.r2s.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;

@SpringBootApplication(scanBasePackages = {"com.r2s.auth", "com.r2s.core"})
@EntityScan(basePackages = {"com.r2s.auth.entity"}) // Đổi lại thành auth.entity
public class AuthServiceApplication {
    //test
    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}