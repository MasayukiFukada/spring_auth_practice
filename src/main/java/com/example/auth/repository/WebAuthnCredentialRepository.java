package com.example.auth.repository;

import com.example.auth.entity.WebAuthnCredential;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WebAuthnCredentialRepository extends JpaRepository<WebAuthnCredential, Long> {

    Optional<WebAuthnCredential> findByCredentialId(byte[] credentialId);
}
