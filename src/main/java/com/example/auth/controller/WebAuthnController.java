package com.example.auth.controller;

import com.example.auth.dto.AuthenticationFinishRequest;
import com.example.auth.dto.RegistrationFinishRequest;
import com.example.auth.dto.StartLoginRequest;
import com.example.auth.entity.User;
import com.example.auth.entity.WebAuthnCredential;
import com.example.auth.repository.UserRepository;
import com.example.auth.repository.WebAuthnCredentialRepository;
import com.example.auth.service.JpaUserDetailsService;
import com.yubico.webauthn.*;
import com.yubico.webauthn.data.*;
import com.yubico.webauthn.exception.AssertionFailedException;
import com.yubico.webauthn.exception.RegistrationFailedException;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/passkey")
public class WebAuthnController {

    private static final Logger log = LoggerFactory.getLogger(WebAuthnController.class);

    private final RelyingParty relyingParty;
    private final UserRepository userRepository;
    private final WebAuthnCredentialRepository credentialRepository;
    private final JpaUserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;

    public WebAuthnController(RelyingParty relyingParty, UserRepository userRepository, WebAuthnCredentialRepository credentialRepository, JpaUserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        this.relyingParty = relyingParty;
        this.userRepository = userRepository;
        this.credentialRepository = credentialRepository;
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/register/start")
    public ResponseEntity<Object> startRegistration(Principal principal, HttpSession session) {
        User user = userRepository.findByName(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        StartRegistrationOptions options = StartRegistrationOptions.builder()
            .user(UserIdentity.builder()
                .name(user.getName())
                .displayName(user.getName())
                .id(new ByteArray(user.getPasskeyId()))
                .build())
            .build();
        
        PublicKeyCredentialCreationOptions creationOptions = relyingParty.startRegistration(options);
        // Log creation options for debugging extensions like appidExclude
        try {
            log.info("PublicKeyCredentialCreationOptions: {}", creationOptions);
        } catch (Exception e) {
            log.warn("Failed to log creationOptions", e);
        }

        // Store the original PublicKeyCredentialCreationOptions in session so finishRegistration can use it.
        session.setAttribute("passkeyRegistration", creationOptions);

        // Create a JSON-safe Map representation to return to the client, and remove any invalid
        // appidExclude value inside extensions (e.g., null). This prevents the browser-side
        // SyntaxError: "The `appidExclude` extension value is neither empty/null nor a valid URL.".
    ObjectMapper mapper = new ObjectMapper();
    // enable support for Optional and java.time types when converting
    mapper.registerModule(new Jdk8Module());
    mapper.registerModule(new JavaTimeModule());
        @SuppressWarnings("unchecked")
        Map<String, Object> map = mapper.convertValue(creationOptions, Map.class);
        if (map.containsKey("extensions") && map.get("extensions") instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> ext = (Map<String, Object>) map.get("extensions");
            if (ext.containsKey("appidExclude")) {
                Object val = ext.get("appidExclude");
                boolean remove = false;
                if (val == null) {
                    remove = true;
                } else if (!(val instanceof String)) {
                    // If not a string URL, remove it
                    remove = true;
                } else {
                    String s = (String) val;
                    if (s.isEmpty()) remove = true;
                    // Optionally, validate URL format here; if invalid, remove. Keep conservative: if it doesn't start with http, drop it.
                    if (!s.startsWith("http://") && !s.startsWith("https://")) remove = true;
                }
                if (remove) {
                    ext.remove("appidExclude");
                    log.info("Sanitized creationOptions: removed invalid appidExclude");
                }
            }
            if (ext.isEmpty()) {
                map.remove("extensions");
            }
        }

        return ResponseEntity.ok(map);
    }

    // Dev-only: reset a user's password to a provided plaintext password (local testing)
    @PostMapping("/dev/reset-password")
    public ResponseEntity<String> resetPassword(@RequestParam String username, @RequestParam String newPassword) {
        return userRepository.findByName(username).map(user -> {
            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);
            return ResponseEntity.ok("Password reset for " + username);
        }).orElse(ResponseEntity.badRequest().body("User not found"));
    }

    // Dev-only: create the test user 'user' with password 'testpass'
    @PostMapping("/dev/create-test-user")
    public ResponseEntity<String> createTestUser() {
        String username = "user";
        String rawPassword = "testpass";
        if (userRepository.findByName(username).isPresent()) {
            return ResponseEntity.ok("User already exists");
        }
        com.example.auth.entity.User u = new com.example.auth.entity.User();
        u.setName(username);
        u.setPassword(passwordEncoder.encode(rawPassword));
        u.setOtpEnabled(false);
        u.setSecret(null);
        u.setPasskeyId(java.util.UUID.randomUUID().toString().getBytes());
        userRepository.save(u);
        return ResponseEntity.ok("Created test user");
    }

    @PostMapping("/register/finish")
    public ResponseEntity<String> finishRegistration(@RequestBody RegistrationFinishRequest request, Principal principal, HttpSession session) {
        User user = userRepository.findByName(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        PublicKeyCredentialCreationOptions creationOptions = (PublicKeyCredentialCreationOptions) session.getAttribute("passkeyRegistration");
        session.removeAttribute("passkeyRegistration");

        if (creationOptions == null) {
            return ResponseEntity.badRequest().body("No registration in progress");
        }

        try {
            RegistrationResult result = relyingParty.finishRegistration(
                FinishRegistrationOptions.builder()
                    .request(creationOptions)
                    .response(request.getCredential())
                    .build()
            );

            WebAuthnCredential newCredential = new WebAuthnCredential();
            newCredential.setUser(user);
            newCredential.setCredentialId(result.getKeyId().getId().getBytes());
            newCredential.setPublicKeyCose(result.getPublicKeyCose().getBytes());
            newCredential.setSignatureCount(0); // Initial count is 0
            newCredential.setUvInitialized(false);

            credentialRepository.save(newCredential);

            return ResponseEntity.ok("Registration successful");
        } catch (RegistrationFailedException e) {
            return ResponseEntity.badRequest().body("Registration failed: " + e.getMessage());
        }
    }

    @PostMapping("/login/start")
    public ResponseEntity<AssertionRequest> startAuthentication(@RequestBody(required = false) StartLoginRequest request, HttpSession session) {
        AssertionRequest assertionRequest = relyingParty.startAssertion(
            StartAssertionOptions.builder()
                .username(request != null ? Optional.ofNullable(request.getUsername()) : Optional.empty())
                .build()
        );
        session.setAttribute("passkeyAuthentication", assertionRequest);
        return ResponseEntity.ok(assertionRequest);
    }

    @PostMapping("/login/finish")
    public ResponseEntity<String> finishAuthentication(@RequestBody AuthenticationFinishRequest request, HttpSession session) {
        AssertionRequest assertionRequest = (AssertionRequest) session.getAttribute("passkeyAuthentication");
        session.removeAttribute("passkeyAuthentication");

        if (assertionRequest == null) {
            return ResponseEntity.badRequest().body("No authentication in progress");
        }

        try {
            AssertionResult result = relyingParty.finishAssertion(
                FinishAssertionOptions.builder()
                    .request(assertionRequest)
                    .response(request.getCredential())
                    .build()
            );

            if (result.isSuccess()) {
                // Update signature count
                credentialRepository.findByCredentialId(result.getCredential().getCredentialId().getBytes()).ifPresent(cred -> {
                    cred.setSignatureCount(result.getSignatureCount());
                    credentialRepository.save(cred);
                });

                UserDetails userDetails = userDetailsService.loadUserByUsername(result.getUsername());
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(auth);
                return ResponseEntity.ok("Login successful");
            } else {
                return ResponseEntity.badRequest().body("Authentication failed");
            }
        } catch (AssertionFailedException e) {
            return ResponseEntity.badRequest().body("Authentication failed: " + e.getMessage());
        }
    }
}
