package com.codevision.codevisionbackend.keycloak;

import java.util.Optional;
import org.keycloak.models.jpa.entities.RealmEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KeycloakRealmRepository extends JpaRepository<RealmEntity, String> {

    Optional<RealmEntity> findByName(String name);
}
