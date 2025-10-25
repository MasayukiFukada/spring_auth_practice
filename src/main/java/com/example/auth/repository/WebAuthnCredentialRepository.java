package com.example.auth.repository;

import com.example.auth.entity.User;
import com.example.auth.entity.WebAuthnCredential;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WebAuthnCredentialRepository extends JpaRepository<WebAuthnCredential, Long> {
    List<WebAuthnCredential> findByUser(User user);
    Optional<WebAuthnCredential> findByCredentialId(byte[] credentialId);
}
