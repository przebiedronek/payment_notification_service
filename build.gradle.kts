plugins {
	jacoco
	`java-library`
	`maven-publish`
	id("org.springframework.boot") version "3.5.0"
	id("io.spring.dependency-management") version "1.1.7"
	id("com.google.protobuf") version "0.9.5"
	id("com.adarshr.test-logger") version "4.0.0"
}

group = "com.biedron.payment.notification"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenLocal()
	mavenCentral()
}

sourceSets {
	main {
		java {
			srcDirs("src/main/java", "build/generated/source/proto/main/java")
		}
	}
}

protobuf {
	protoc {
		artifact = "com.google.protobuf:protoc:4.31.1"
	}

	generateProtoTasks {
		all().forEach { task ->
			task.builtins {
				java {
				}
			}
		}
	}
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.projectlombok:lombok")
	compileOnly("org.projectlombok:lombok")
	annotationProcessor("org.projectlombok:lombok")
	implementation("com.google.protobuf:protobuf-java:4.31.1")
	implementation("com.google.protobuf:protobuf-java-util:4.31.1")
	implementation("org.springframework.kafka:spring-kafka")
	implementation("org.apache.kafka:kafka-clients")
	implementation("net.logstash.logback:logstash-logback-encoder:8.1")
	implementation("org.json:json:20250517")
	runtimeOnly("org.postgresql:postgresql:42.5.1")
	testImplementation("org.awaitility:awaitility:4.2.2")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.springframework.kafka:spring-kafka-test")
	testImplementation("org.testcontainers:junit-jupiter")
	testImplementation("org.testcontainers:kafka")
    testImplementation("org.wiremock:wiremock-standalone:3.3.1")
	testImplementation("com.h2database:h2")
	testImplementation("org.projectlombok:lombok")
	testCompileOnly("org.projectlombok:lombok:1.18.30")
	testAnnotationProcessor("org.projectlombok:lombok:1.18.30")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
	useJUnitPlatform()
}