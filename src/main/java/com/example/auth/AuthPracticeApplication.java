package com.example.auth;

import dev.samstevens.totp.spring.autoconfigure.TotpAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import(TotpAutoConfiguration.class)
public class AuthPracticeApplication {

	public static void main(String[] args) {
		SpringApplication.run(AuthPracticeApplication.class, args);
	}

}
