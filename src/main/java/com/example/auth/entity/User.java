package com.example.auth.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.Set;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private boolean otpEnabled = false;

    private String secret;

    @Column(nullable = false, unique = true)
    private byte[] passkeyId;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private Set<WebAuthnCredential> credentials;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isOtpEnabled() {
        return otpEnabled;
    }

    public void setOtpEnabled(boolean otpEnabled) {
        this.otpEnabled = otpEnabled;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public byte[] getPasskeyId() {
        return passkeyId;
    }

    public void setPasskeyId(byte[] passkeyId) {
        this.passkeyId = passkeyId;
    }

    public Set<WebAuthnCredential> getCredentials() {
        return credentials;
    }

    public void setCredentials(Set<WebAuthnCredential> credentials) {
        this.credentials = credentials;
    }
}
