package com.sitionix.forgeit.consumer.auth;

public class UserProfileResponse {

    private String id;
    private String username;
    private boolean active;

    public UserProfileResponse() {
    }

    public UserProfileResponse(final String id, final String username, final boolean active) {
        this.id = id;
        this.username = username;
        this.active = active;
    }

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(final String username) {
        this.username = username;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(final boolean active) {
        this.active = active;
    }
}
