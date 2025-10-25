package com.example.auth.security;

import com.example.auth.entity.User;
import com.example.auth.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.samstevens.totp.code.CodeVerifier;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class OtpAuthenticationFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;
    private final CodeVerifier codeVerifier;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OtpAuthenticationFilter(UserRepository userRepository, CodeVerifier codeVerifier) {
        this.userRepository = userRepository;
        this.codeVerifier = codeVerifier;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Only process POST requests to /api/otp/login
        if (!request.getMethod().equals(HttpMethod.POST.name()) || !request.getRequestURI().equals("/api/otp/login")) {
            filterChain.doFilter(request, response);
            return;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // Only process if the user has the temporary OTP_PENDING role
        if (authentication == null || !authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_OTP_PENDING"))) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.getWriter().write("{\"error\": \"OTP login not initiated\"}");
            return;
        }

        try {
            OtpLoginRequest otpLoginRequest = objectMapper.readValue(request.getInputStream(), OtpLoginRequest.class);
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            User user = userRepository.findByName(userDetails.getUsername()).orElse(null);

            if (user == null || !codeVerifier.isValidCode(user.getSecret(), otpLoginRequest.getCode())) {
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                response.getWriter().write("{\"error\": \"Invalid OTP code\"}");
                return;
            }

            // OTP is valid, grant final authentication
            UsernamePasswordAuthenticationToken finalAuth = new UsernamePasswordAuthenticationToken(
                    userDetails, null, userDetails.getAuthorities()); // Use original authorities
            finalAuth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(finalAuth);

            response.setStatus(HttpStatus.OK.value());
            response.getWriter().write("{\"message\": \"Login successful\"}");

        } catch (Exception e) {
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            response.getWriter().write("{\"error\": \"Invalid request\"}");
        }
    }

    private static class OtpLoginRequest {
        private String code;

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }
    }
}
