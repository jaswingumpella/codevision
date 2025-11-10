package com.codevision.codevisionbackend.keycloak;

import java.util.List;

public class KeycloakUserListResponse {

    private final String realmName;
    private final int total;
    private final List<KeycloakUserSummary> users;

    public KeycloakUserListResponse(String realmName, List<KeycloakUserSummary> users) {
        this.realmName = realmName;
        List<KeycloakUserSummary> safeUsers = users == null ? List.of() : List.copyOf(users);
        this.users = safeUsers;
        this.total = safeUsers.size();
    }

    public String getRealmName() {
        return realmName;
    }

    public int getTotal() {
        return total;
    }

    public List<KeycloakUserSummary> getUsers() {
        return users;
    }
}
