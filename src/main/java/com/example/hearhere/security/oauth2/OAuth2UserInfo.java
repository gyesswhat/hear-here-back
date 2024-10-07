package com.example.hearhere.security.oauth2;

public interface OAuth2UserInfo {
    String getProviderId();
    String getProvider();
    String getName();
}
