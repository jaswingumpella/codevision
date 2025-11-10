package com.codevision.codevisionbackend.keycloak;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class KeycloakIdentityService {

    private static final Logger log = LoggerFactory.getLogger(KeycloakIdentityService.class);
    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;

    private final KeycloakRealmRepository realmRepository;
    private final KeycloakUserRepository userRepository;

    public KeycloakIdentityService(
            KeycloakRealmRepository realmRepository, KeycloakUserRepository userRepository) {
        this.realmRepository = realmRepository;
        this.userRepository = userRepository;
    }

    public Optional<KeycloakUserListResponse> fetchRealmUsers(String realmName, int requestedLimit) {
        return realmRepository.findByName(realmName).map(realm -> {
            int pageSize = normalizeLimit(requestedLimit);
            Pageable pageable = PageRequest.of(0, pageSize);
            List<KeycloakUserSummary> summaries = userRepository
                    .findByRealmIdOrderByUsernameAsc(realm.getId(), pageable)
                    .stream()
                    .map(KeycloakUserSummary::fromEntity)
                    .filter(Objects::nonNull)
                    .toList();
            log.debug("Resolved {} Keycloak users for realm {}", summaries.size(), realm.getName());
            return new KeycloakUserListResponse(realm.getName(), summaries);
        });
    }

    private int normalizeLimit(int requestedLimit) {
        if (requestedLimit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(requestedLimit, MAX_LIMIT);
    }
}
