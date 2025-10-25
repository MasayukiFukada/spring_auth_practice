package com.example.auth.service;

import com.example.auth.repository.UserRepository;
import com.example.auth.repository.WebAuthnCredentialRepository;
import com.yubico.webauthn.RelyingParty;
import com.yubico.webauthn.data.RelyingPartyIdentity;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

@Configuration
public class WebAuthnConfig {

    @Bean
    public RelyingParty relyingParty(WebAuthnCredentialRepository credentialRepo, UserRepository userRepo) {
        RelyingPartyIdentity rpIdentity = RelyingPartyIdentity.builder()
                .id("localhost") // RP ID (ドメイン)
                .name("Spring Auth Practice")
                .build();

        return RelyingParty.builder()
                .identity(rpIdentity)
                .credentialRepository(new JpaCredentialRepository(credentialRepo, userRepo))
                .origins(Set.of("https://localhost:8443")) // 許可するオリジン
                .build();
    }
}