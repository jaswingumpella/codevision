package com.codevision.codevisionbackend.keycloak;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.models.jpa.entities.RealmEntity;
import org.keycloak.models.jpa.entities.UserEntity;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class KeycloakIdentityServiceTest {

    @Mock
    private KeycloakRealmRepository realmRepository;

    @Mock
    private KeycloakUserRepository userRepository;

    @InjectMocks
    private KeycloakIdentityService keycloakIdentityService;

    @Test
    void fetchRealmUsersReturnsEmptyWhenRealmMissing() {
        when(realmRepository.findByName("missing")).thenReturn(Optional.empty());

        Optional<KeycloakUserListResponse> result = keycloakIdentityService.fetchRealmUsers("missing", 10);

        assertThat(result).isEmpty();
    }

    @Test
    void fetchRealmUsersReturnsUsersWhenRealmExists() {
        RealmEntity realm = new RealmEntity();
        realm.setId("realm-id");
        realm.setName("cv");
        when(realmRepository.findByName("cv")).thenReturn(Optional.of(realm));

        UserEntity user = new UserEntity();
        user.setId("user-1");
        user.setUsername("demo");
        user.setEnabled(true);
        user.setEmailVerified(true);
        when(userRepository.findByRealmIdOrderByUsernameAsc(eq("realm-id"), any(Pageable.class)))
                .thenReturn(List.of(user));

        Optional<KeycloakUserListResponse> result = keycloakIdentityService.fetchRealmUsers("cv", 10);

        assertThat(result).isPresent();
        assertThat(result.get().getRealmName()).isEqualTo("cv");
        assertThat(result.get().getTotal()).isEqualTo(1);
        assertThat(result.get().getUsers()).hasSize(1);
        assertThat(result.get().getUsers().get(0).username()).isEqualTo("demo");
    }

    @Test
    void fetchRealmUsersClampsRequestedLimit() {
        RealmEntity realm = new RealmEntity();
        realm.setId("realm-id");
        realm.setName("cv");
        when(realmRepository.findByName("cv")).thenReturn(Optional.of(realm));
        when(userRepository.findByRealmIdOrderByUsernameAsc(eq("realm-id"), any(Pageable.class)))
                .thenReturn(List.of());

        keycloakIdentityService.fetchRealmUsers("cv", 500);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(userRepository).findByRealmIdOrderByUsernameAsc(eq("realm-id"), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(100);
    }
}
