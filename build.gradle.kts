plugins {
	java
	id("org.springframework.boot") version "4.1.0"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "com.capitec"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(25)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	// Spring application foundation.
	implementation("org.springframework.boot:spring-boot-starter")
	implementation("org.springframework.boot:spring-boot-starter-actuator")

	// Web API and request validation support.
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-validation")

	// Database access through Spring Data JPA.
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")

	// Authentication, authorization, and password hashing.
	implementation("org.springframework.boot:spring-boot-starter-security")

	// JSON serialization used by the JWT service.
	implementation("com.fasterxml.jackson.core:jackson-databind")

	// OpenAPI
	implementation("io.swagger.core.v3:swagger-annotations-jakarta:2.2.39")
	implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.3")

	// Type-safe DTO/entity mapping.
	implementation("org.mapstruct:mapstruct:1.6.3")

	// PDF statement generation.
	implementation("org.apache.pdfbox:pdfbox:3.0.7")

	// S3-compatible object storage client.
	implementation(platform("software.amazon.awssdk:bom:2.20.0"))
	implementation("software.amazon.awssdk:s3")

	// Lombok source-generation annotations.
	compileOnly("org.projectlombok:lombok")
	annotationProcessor("org.projectlombok:lombok")

	// MapStruct annotation processing, including Lombok integration.
	annotationProcessor("org.mapstruct:mapstruct-processor:1.6.3")
	annotationProcessor("org.projectlombok:lombok-mapstruct-binding:0.2.0")

	// PostgreSQL JDBC driver for runtime database connections.
	runtimeOnly("org.postgresql:postgresql")

	// Spring Boot test support.
	testImplementation("org.springframework.boot:spring-boot-starter-test")

	// Spring Security test helpers.
	testImplementation("org.springframework.security:spring-security-test")

	// Lombok support for test sources.
	testCompileOnly("org.projectlombok:lombok")
	testAnnotationProcessor("org.projectlombok:lombok")

	// MapStruct support for generated test sources.
	testAnnotationProcessor("org.mapstruct:mapstruct-processor:1.6.3")
	testAnnotationProcessor("org.projectlombok:lombok-mapstruct-binding:0.2.0")

	// JUnit Platform runtime launcher.
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")

	// In-memory database for Spring context tests using the test profile.
	testRuntimeOnly("com.h2database:h2")
}

tasks.withType<Test> {
	useJUnitPlatform()
	systemProperty("spring.profiles.active", "test")
}
