package com.codevision.codevisionbackend.keycloak;

import org.keycloak.models.jpa.entities.UserEntity;

public record KeycloakUserSummary(
        String id,
        String username,
        String email,
        String firstName,
        String lastName,
        boolean enabled,
        boolean emailVerified) {

    public static KeycloakUserSummary fromEntity(UserEntity entity) {
        if (entity == null) {
            return null;
        }
        return new KeycloakUserSummary(
                entity.getId(),
                entity.getUsername(),
                entity.getEmail(),
                entity.getFirstName(),
                entity.getLastName(),
                entity.isEnabled(),
                entity.isEmailVerified());
    }
}
