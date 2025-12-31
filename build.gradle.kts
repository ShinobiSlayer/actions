plugins {
	java
	id("org.springframework.boot") version "3.5.9"
	id("io.spring.dependency-management") version "1.1.7"
}

// Apply custom Docker plugin
apply<DockerPlugin>()

// Intorqa-style versioning: YYYY.MM.NNN-BRANCH
val codeVersion = "2025.12.001"
val branchVersion: String? by project
version = "${codeVersion}-${branchVersion ?: "DEV"}"

group = "com.github.shinobislayer"
description = "Test project for CI/CD with GitHub Actions, Docker Hub, and ArgoCD"

// Set build directory to match Intorqa pattern
layout.buildDirectory.set(file(".build"))

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
	useJUnitPlatform()
}

// Add printVersion task (used by CI/CD)
tasks.register("printVersion") {
	doLast {
		println(project.version)
	}
}

// Default tasks to run
defaultTasks("clean", "build", "docker")
