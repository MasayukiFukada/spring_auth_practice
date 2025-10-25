
package com.example.auth.controller;

import com.example.auth.dto.StartLoginRequest;
import com.example.auth.entity.User;
import com.example.auth.entity.WebAuthnCredential;
import com.example.auth.repository.UserRepository;
import com.example.auth.repository.WebAuthnCredentialRepository;
import com.example.auth.service.JpaUserDetailsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yubico.webauthn.*;
import com.yubico.webauthn.data.*;
import com.yubico.webauthn.exception.AssertionFailedException;
import com.yubico.webauthn.exception.RegistrationFailedException;
import jakarta.servlet.http.HttpSession;
import java.security.Principal;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/passkey")
public class WebAuthnController {

    private static final Logger log = LoggerFactory.getLogger(WebAuthnController.class);

    private final RelyingParty relyingParty;
    private final UserRepository userRepository;
    private final WebAuthnCredentialRepository credentialRepository;
    private final ObjectMapper objectMapper;
    private final JpaUserDetailsService userDetailsService;

    public WebAuthnController(
            RelyingParty relyingParty,
            UserRepository userRepository,
            WebAuthnCredentialRepository credentialRepository,
            ObjectMapper objectMapper,
            JpaUserDetailsService userDetailsService) {
        this.relyingParty = relyingParty;
        this.userRepository = userRepository;
        this.credentialRepository = credentialRepository;
        this.objectMapper = objectMapper;
        this.userDetailsService = userDetailsService;
    }

    @PostMapping("/register/start")
    @ResponseBody
    public PublicKeyCredentialCreationOptions startRegistration(Principal principal, HttpSession session) {
        User user = userRepository
                .findByName(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        StartRegistrationOptions options = StartRegistrationOptions.builder()
                .user(UserIdentity.builder()
                        .name(user.getName())
                        .displayName(user.getName())
                        .id(new com.yubico.webauthn.data.ByteArray(user.getPasskeyId()))
                        .build())
                .build();

        PublicKeyCredentialCreationOptions creationOptions = relyingParty.startRegistration(options);
        session.setAttribute("passkeyRegistration", creationOptions);
        return creationOptions;
    }

    @PostMapping("/register/finish")
    public ResponseEntity<String> finishRegistration(
            @RequestBody Map<String, Object> requestBody,
            Principal principal,
            HttpSession session) {
        User user = userRepository
                .findByName(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        PublicKeyCredentialCreationOptions creationOptions = (PublicKeyCredentialCreationOptions) session.getAttribute(
                "passkeyRegistration");
        session.removeAttribute("passkeyRegistration");

        if (creationOptions == null) {
            return ResponseEntity.badRequest().body("No registration in progress");
        }

        try {
            log.info("finishRegistration requestBody: {}", requestBody);
            PublicKeyCredential<AuthenticatorAttestationResponse, ClientRegistrationExtensionOutputs> credential = objectMapper.convertValue(
                    requestBody,
                    new com.fasterxml.jackson.core.type.TypeReference<PublicKeyCredential<AuthenticatorAttestationResponse, ClientRegistrationExtensionOutputs>>() {}
            );

            RegistrationResult result = relyingParty.finishRegistration(
                    FinishRegistrationOptions.builder()
                            .request(creationOptions)
                            .response(credential)
                            .build());

            WebAuthnCredential newCredential = new WebAuthnCredential();
            newCredential.setUser(user);
            newCredential.setCredentialId(result.getKeyId().getId().getBytes());
            newCredential.setPublicKeyCose(result.getPublicKeyCose().getBase64Url());
            newCredential.setSignatureCount(0); // Initial count is 0
            newCredential.setUvInitialized(false);

            credentialRepository.save(newCredential);

            return ResponseEntity.ok("Registration successful");
        } catch (RegistrationFailedException e) {
            log.error("Registration failed: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body("Registration failed: " + e.getMessage());
        } catch (Exception e) {
            log.error("An unexpected error occurred during registration: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body("An unexpected error occurred: " + e.getMessage());
        }
    }

    @PostMapping("/login/start")
    @ResponseBody
    public PublicKeyCredentialRequestOptions startLogin(
            @RequestBody(required = false) StartLoginRequest loginRequest, HttpSession session) {

        var optionsBuilder = StartAssertionOptions.builder()
                .userVerification(UserVerificationRequirement.DISCOURAGED);

        if (loginRequest != null && loginRequest.getUsername() != null && !loginRequest.getUsername().isEmpty()) {
            userRepository.findByName(loginRequest.getUsername()).ifPresent(user -> {
                var credentials = credentialRepository.findByUser(user).stream()
                        .map(cred -> PublicKeyCredentialDescriptor.builder()
                                .id(new ByteArray(cred.getCredentialId()))
                                .build())
                        .toList();
                optionsBuilder.username(user.getName());
            });
        }

        AssertionRequest assertionRequest = relyingParty.startAssertion(optionsBuilder.build());
        log.info("Generated AssertionRequest with allowCredentials: {}", assertionRequest.getPublicKeyCredentialRequestOptions().getAllowCredentials());
        session.setAttribute("passkeyLogin", assertionRequest);
        log.info("AssertionRequest stored in session: {}", assertionRequest);
        return assertionRequest.getPublicKeyCredentialRequestOptions();
    }

    @PostMapping("/login/finish")
    public ResponseEntity<String> finishLogin(@RequestBody Map<String, Object> requestBody, HttpSession session) {

        AssertionRequest assertionRequest = (AssertionRequest) session.getAttribute("passkeyLogin");
        session.removeAttribute("passkeyLogin");

        if (assertionRequest == null) {
            log.error("AssertionRequest is null in session. No login in progress or session expired.");
            return ResponseEntity.badRequest().body("No login in progress");
        }

        try {
            PublicKeyCredential<AuthenticatorAssertionResponse, ClientAssertionExtensionOutputs> credential = objectMapper.convertValue(
                    requestBody,
                    new com.fasterxml.jackson.core.type.TypeReference<PublicKeyCredential<AuthenticatorAssertionResponse, ClientAssertionExtensionOutputs>>() {}
            );

            log.info("Client sent credential ID: {}", credential.getId());
            log.info("Server expected allowCredentials: {}", assertionRequest.getPublicKeyCredentialRequestOptions().getAllowCredentials());

            AssertionResult result = relyingParty.finishAssertion(FinishAssertionOptions.builder()
                    .request(assertionRequest)
                    .response(credential)
                    .build());

            if (result.isSuccess()) {
                User user = userRepository
                        .findByPasskeyId(result.getUserHandle().getBytes())
                        .orElseThrow(() -> new IllegalArgumentException("User not found for passkey handle"));

                // Manually authenticate the user
                UserDetails userDetails = userDetailsService.loadUserByUsername(user.getName());
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(auth);

                return ResponseEntity.ok("Login successful for user: " + user.getName());
            } else {
                return ResponseEntity.badRequest().body("Login failed");
            }

        } catch (AssertionFailedException e) {
            log.error("Assertion failed", e);
            return ResponseEntity.badRequest().body("Login failed: " + e.getMessage());
        } catch (Exception e) {
            log.error("An unexpected error occurred during login", e);
            return ResponseEntity.badRequest().body("An unexpected error occurred: " + e.getMessage());
        }
    }
}
