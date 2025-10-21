package com.example.auth.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.yubico.webauthn.data.AuthenticatorAssertionResponse;
import com.yubico.webauthn.data.ClientAssertionExtensionOutputs;
import com.yubico.webauthn.data.PublicKeyCredential;

public class AuthenticationFinishRequest {
    private final PublicKeyCredential<AuthenticatorAssertionResponse, ClientAssertionExtensionOutputs> credential;

    @JsonCreator
    public AuthenticationFinishRequest(
            @JsonProperty("credential") PublicKeyCredential<AuthenticatorAssertionResponse, ClientAssertionExtensionOutputs> credential) {
        this.credential = credential;
    }

    public PublicKeyCredential<AuthenticatorAssertionResponse, ClientAssertionExtensionOutputs> getCredential() {
        return credential;
    }
}
