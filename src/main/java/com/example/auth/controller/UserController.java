package com.example.auth.controller;

import com.example.auth.entity.User;
import com.example.auth.repository.UserRepository;
import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecretGenerator secretGenerator;
    private final CodeVerifier codeVerifier;
    private final QrGenerator qrGenerator;


    public UserController(UserRepository userRepository, PasswordEncoder passwordEncoder, SecretGenerator secretGenerator, CodeVerifier codeVerifier, QrGenerator qrGenerator) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.secretGenerator = secretGenerator;
        this.codeVerifier = codeVerifier;
        this.qrGenerator = qrGenerator;
    }

    @PostMapping("/register")
    public ResponseEntity<String> registerUser(@RequestBody RegisterRequest registerRequest) {
        if (userRepository.findByName(registerRequest.getName()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("User already exists");
        }

        User newUser = new User();
        newUser.setName(registerRequest.getName());
        newUser.setPassword(passwordEncoder.encode(registerRequest.getPassword()));

        userRepository.save(newUser);

        return ResponseEntity.status(HttpStatus.CREATED).body("User registered successfully");
    }

    @GetMapping("/me")
    public ResponseEntity<Principal> getMe(Principal principal) {
        return ResponseEntity.ok(principal);
    }

    @PostMapping("/otp/setup")
    public ResponseEntity<?> setupOtp(Principal principal) {
        User user = userRepository.findByName(principal.getName())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        String secret = secretGenerator.generate();
        user.setSecret(secret);
        userRepository.save(user);

        QrData qrData = new QrData.Builder()
                .label(user.getName())
                .secret(secret)
                .issuer("SpringAuthPractice")
                .build();

        String qrCodeImage;
        try {
            byte[] imageData = qrGenerator.generate(qrData);
            qrCodeImage = "data:image/png;base64," + Base64.getEncoder().encodeToString(imageData);
        } catch (QrGenerationException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to generate QR code");
        }

        Map<String, String> response = new HashMap<>();
        response.put("secret", secret);
        response.put("qrUrl", qrCodeImage);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/otp/verify")
    public ResponseEntity<?> verifyOtp(@RequestBody OtpVerifyRequest verifyRequest, Principal principal) {
        User user = userRepository.findByName(principal.getName())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        if (!codeVerifier.isValidCode(user.getSecret(), verifyRequest.getCode())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid OTP code");
        }

        user.setOtpEnabled(true);
        userRepository.save(user);

        return ResponseEntity.ok("OTP enabled successfully");
    }


    // Inner class for the registration request body
    public static class RegisterRequest {
        private String name;
        private String password;

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
    }

    // Inner class for OTP verification request
    public static class OtpVerifyRequest {
        private String code;

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }
    }
}
