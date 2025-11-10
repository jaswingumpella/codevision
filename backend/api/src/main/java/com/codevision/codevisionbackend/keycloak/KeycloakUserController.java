package com.codevision.codevisionbackend.keycloak;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/keycloak")
public class KeycloakUserController {

    private static final Logger log = LoggerFactory.getLogger(KeycloakUserController.class);

    private final KeycloakIdentityService keycloakIdentityService;

    public KeycloakUserController(KeycloakIdentityService keycloakIdentityService) {
        this.keycloakIdentityService = keycloakIdentityService;
    }

    @GetMapping("/realms/{realmName}/users")
    public ResponseEntity<KeycloakUserListResponse> getRealmUsers(
            @PathVariable("realmName") String realmName,
            @RequestParam(name = "limit", defaultValue = "20") int limit) {
        if (realmName == null || realmName.isBlank()) {
            log.warn("Rejecting Keycloak user lookup due to missing realm name");
            return ResponseEntity.badRequest().build();
        }
        return keycloakIdentityService
                .fetchRealmUsers(realmName, limit)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
