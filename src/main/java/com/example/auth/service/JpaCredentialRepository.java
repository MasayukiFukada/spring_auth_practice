package com.example.auth.service;

import com.example.auth.entity.User;
import com.example.auth.repository.UserRepository;
import com.example.auth.repository.WebAuthnCredentialRepository;
import com.yubico.webauthn.CredentialRepository;
import com.yubico.webauthn.data.exception.Base64UrlException;
import com.yubico.webauthn.RegisteredCredential;
import com.yubico.webauthn.data.ByteArray;
import com.yubico.webauthn.data.PublicKeyCredentialDescriptor;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class JpaCredentialRepository implements CredentialRepository {

    private final WebAuthnCredentialRepository credentialRepository;
    private final UserRepository userRepository;

    public JpaCredentialRepository(WebAuthnCredentialRepository credentialRepository, UserRepository userRepository) {
        this.credentialRepository = credentialRepository;
        this.userRepository = userRepository;
    }

    @Override
    public Set<PublicKeyCredentialDescriptor> getCredentialIdsForUsername(String username) {
        return userRepository.findByName(username)
                .map(User::getCredentials)
                .orElse(Set.of())
                .stream()
                .map(cred -> PublicKeyCredentialDescriptor.builder().id(new ByteArray(cred.getCredentialId())).build())
                .collect(Collectors.toSet());
    }

    @Override
    public Optional<ByteArray> getUserHandleForUsername(String username) {
        return userRepository.findByName(username)
                .map(user -> new ByteArray(user.getPasskeyId()));
    }

    @Override
    public Optional<String> getUsernameForUserHandle(ByteArray userHandle) {
        return userRepository.findByPasskeyId(userHandle.getBytes())
                .map(User::getName);
    }

    @Override
    public Optional<RegisteredCredential> lookup(ByteArray credentialId, ByteArray userHandle) {
        return credentialRepository.findByCredentialId(credentialId.getBytes())
                .filter(cred -> new ByteArray(cred.getUser().getPasskeyId()).equals(userHandle))
                .stream() // ここにstream()を追加
                .map(cred -> {
                    try {
                        return RegisteredCredential.builder()
                                .credentialId(new ByteArray(cred.getCredentialId()))
                                .userHandle(new ByteArray(cred.getUser().getPasskeyId()))
                                .publicKeyCose(ByteArray.fromBase64Url(cred.getPublicKeyCose()))
                                .signatureCount(cred.getSignatureCount())
                                .build();
                    } catch (Base64UrlException e) {
                        return null; // 例外が発生した場合はnullを返す
                    }
                })
                .filter(java.util.Objects::nonNull)
                .findFirst();
    }

    @Override
    public Set<RegisteredCredential> lookupAll(ByteArray credentialId) {
         return credentialRepository.findAll().stream()
                .filter(cred -> new ByteArray(cred.getCredentialId()).equals(credentialId))
                .map(cred -> {
                    try {
                        return RegisteredCredential.builder()
                                .credentialId(new ByteArray(cred.getCredentialId()))
                                .userHandle(new ByteArray(cred.getUser().getPasskeyId()))
                                .publicKeyCose(ByteArray.fromBase64Url(cred.getPublicKeyCose()))
                                .signatureCount(cred.getSignatureCount())
                                .build();
                    } catch (Base64UrlException e) {
                        return null; // 例外が発生した場合はnullを返す
                    }
                })
                .filter(java.util.Objects::nonNull) // nullを除外
                .collect(Collectors.toCollection(HashSet::new));
    }
}
