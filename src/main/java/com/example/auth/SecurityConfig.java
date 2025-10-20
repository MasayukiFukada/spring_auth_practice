package com.example.auth;

import com.example.auth.security.OtpAuthenticationFilter;
import com.example.auth.security.OtpAuthenticationSuccessHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final OtpAuthenticationSuccessHandler otpAuthenticationSuccessHandler;
    private final OtpAuthenticationFilter otpAuthenticationFilter;

    public SecurityConfig(OtpAuthenticationSuccessHandler otpAuthenticationSuccessHandler,
                          OtpAuthenticationFilter otpAuthenticationFilter) {
        this.otpAuthenticationSuccessHandler = otpAuthenticationSuccessHandler;
        this.otpAuthenticationFilter = otpAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .headers(headers -> headers
                .contentSecurityPolicy(csp -> csp
                    .policyDirectives(
                        "script-src 'self' https://cdn.tailwindcss.com https://cdn.jsdelivr.net/npm/rxjs@7.8.2/dist/bundles/rxjs.umd.min.js; " +
                        "img-src 'self' data: https://chart.googleapis.com; " +
                        "object-src 'none';"
                    )
                )
            )
            .csrf(csrf -> csrf.disable()) // APIベースなのでCSRFは一旦無効に
            .addFilterAfter(otpAuthenticationFilter, UsernamePasswordAuthenticationFilter.class) // Add our custom filter
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers("/", "/main.html", "/css/**", "/main.js", "/main.js.orig", "/api/register", "/error").permitAll()
                .requestMatchers("/api/otp/login").hasAuthority("ROLE_OTP_PENDING") // Only allow access if OTP is pending
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginProcessingUrl("/api/login") // ログインAPIのエンドポイント
                .successHandler(otpAuthenticationSuccessHandler)
                .failureHandler((req, res, ex) -> res.setStatus(HttpStatus.UNAUTHORIZED.value()))
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/api/logout") // ログアウトAPIのエンドポイント
                .logoutSuccessHandler((req, res, auth) -> res.setStatus(HttpStatus.OK.value()))
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)) // 未認証時のレスポンス
            );
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
