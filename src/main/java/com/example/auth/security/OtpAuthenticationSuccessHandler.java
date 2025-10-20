package com.example.auth.security;

import com.example.auth.entity.User;
import com.example.auth.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Component
public class OtpAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OtpAuthenticationSuccessHandler(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        User user = userRepository.findByName(userDetails.getUsername()).orElse(null);

        response.setContentType("application/json");

        if (user != null && user.isOtpEnabled()) {
            // Grant temporary authority
            Authentication tempAuth = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                    userDetails, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_OTP_PENDING")));
            SecurityContextHolder.getContext().setAuthentication(tempAuth);

            response.setStatus(HttpStatus.OK.value());
            Map<String, Object> data = new HashMap<>();
            data.put("otpRequired", true);
            response.getWriter().write(objectMapper.writeValueAsString(data));
        } else {
            // Proceed with normal login
            response.setStatus(HttpStatus.OK.value());
            Map<String, Object> data = new HashMap<>();
            data.put("otpRequired", false);
            response.getWriter().write(objectMapper.writeValueAsString(data));
        }
    }
}
