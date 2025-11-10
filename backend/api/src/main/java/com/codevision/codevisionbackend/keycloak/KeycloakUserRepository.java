package com.codevision.codevisionbackend.keycloak;

import java.util.List;
import org.keycloak.models.jpa.entities.UserEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KeycloakUserRepository extends JpaRepository<UserEntity, String> {

    List<UserEntity> findByRealmIdOrderByUsernameAsc(String realmId, Pageable pageable);
}
