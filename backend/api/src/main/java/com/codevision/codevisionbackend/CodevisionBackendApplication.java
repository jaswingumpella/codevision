package com.codevision.codevisionbackend;

import org.keycloak.models.jpa.entities.UserEntity;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;

@SpringBootApplication
@EntityScan(basePackageClasses = {CodevisionBackendApplication.class, UserEntity.class})
public class CodevisionBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(CodevisionBackendApplication.class, args);
    }
}
