plugins {
	java
	id("org.springframework.boot") version "3.5.6"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "com.example"
version = "0.0.1-SNAPSHOT"
description = "Demo Project Spring Auth"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(24)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	runtimeOnly("org.xerial:sqlite-jdbc")
	implementation("org.hibernate.orm:hibernate-community-dialects")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("dev.samstevens.totp:totp-spring-boot-starter:1.7.1")
	implementation("com.google.zxing:javase:3.5.3")
	implementation("com.yubico:webauthn-server-core:2.2.0")
	implementation("com.yubico:webauthn-server-attestation:2.2.0")
	implementation("com.fasterxml.jackson.datatype:jackson-datatype-jdk8:2.19.2")
	implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.19.2")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.springframework.security:spring-security-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
	useJUnitPlatform()
}
