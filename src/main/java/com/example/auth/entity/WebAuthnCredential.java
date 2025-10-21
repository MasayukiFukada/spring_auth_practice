package com.example.auth.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "webauthn_credentials")
public class WebAuthnCredential {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, unique = true)
    private byte[] credentialId;

    @Lob
    @Column(nullable = false)
    private byte[] publicKeyCose;

    @Column(nullable = false)
    private long signatureCount;

    @Column(nullable = false)
    private boolean uvInitialized;

    // Getters and setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public byte[] getCredentialId() {
        return credentialId;
    }

    public void setCredentialId(byte[] credentialId) {
        this.credentialId = credentialId;
    }

    public byte[] getPublicKeyCose() {
        return publicKeyCose;
    }

    public void setPublicKeyCose(byte[] publicKeyCose) {
        this.publicKeyCose = publicKeyCose;
    }

    public long getSignatureCount() {
        return signatureCount;
    }

    public void setSignatureCount(long signatureCount) {
        this.signatureCount = signatureCount;
    }

    public boolean isUvInitialized() {
        return uvInitialized;
    }

    public void setUvInitialized(boolean uvInitialized) {
        this.uvInitialized = uvInitialized;
    }
}
