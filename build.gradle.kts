plugins {
    java
    id("org.springframework.boot") version "3.3.0"
    id("io.spring.dependency-management") version "1.1.5"
    id("com.diffplug.spotless") version "6.25.0"
}

group = "ho.seongho"
version = "0.0.1"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:2023.0.1")
    }
}

val redissonVerseion by extra("3.41.0")
val jjwtVersion by extra("0.12.6")
var kafkaClientVersion by extra("3.3.0")
val bouncyCastleVersion by extra("1.79")
val tikaVersion by extra("3.0.0")
val j2htmlVersion by extra("1.6.0")
val jsoupVersion by extra("1.20.1")

repositories {
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    // Spring Boot Starter
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.redisson:redisson-spring-boot-starter:$redissonVerseion")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-mail")
    implementation("org.springframework.cloud:spring-cloud-starter-openfeign")
    implementation("org.springframework.boot:spring-boot-configuration-processor")
    // OAuth2
    implementation ("org.springframework.security:spring-security-oauth2-authorization-server")
    implementation ("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation ("org.springframework.boot:spring-boot-starter-oauth2-client")
    implementation ("org.springframework.security:spring-security-oauth2-jose")
    // For WebClient
    implementation ("org.springframework:spring-webflux")
    implementation ("io.projectreactor.netty:reactor-netty-http")
    // Lombok
    implementation("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")
    // JWT
    implementation("io.jsonwebtoken:jjwt-api:$jjwtVersion")
    implementation("io.jsonwebtoken:jjwt-impl:$jjwtVersion")
    implementation("io.jsonwebtoken:jjwt-jackson:$jjwtVersion")
    // Apache Kafka
    implementation("org.springframework.kafka:spring-kafka:$kafkaClientVersion")
    // Bouncy Castle
    implementation("org.bouncycastle:bcprov-jdk18on:$bouncyCastleVersion")
    // Metrics
    implementation("io.micrometer:micrometer-registry-prometheus")
    // AWS SDK
    implementation(platform("software.amazon.awssdk:bom:2.24.0"))
    implementation("software.amazon.awssdk:s3")
    implementation("software.amazon.awssdk:ses")
    implementation("software.amazon.awssdk:sns")
    implementation("software.amazon.awssdk:dynamodb")
    // Apache Tika
    implementation("org.apache.tika:tika-core:$tikaVersion")
    // HTML
    implementation("com.j2html:j2html:$j2htmlVersion")
    implementation("org.jsoup:jsoup:$jsoupVersion")
    runtimeOnly("com.mysql:mysql-connector-j")
    implementation("org.dom4j:dom4j:2.1.4")
    implementation("com.github.javaparser:javaparser-core:3.28.0")
    implementation("com.github.javaparser:javaparser-symbol-solver-core:3.28.0")
}

spotless {
    java {
        googleJavaFormat()
            .formatJavadoc(true)
        endWithNewline()
        formatAnnotations()
        removeUnusedImports()
        trimTrailingWhitespace()
    }
}

tasks.jar {
    isEnabled = false
}

tasks.withType<Test> {
    useJUnitPlatform()
    reports {
        html.required.set(false)
        junitXml.required.set(false)
    }
    maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2)
        .coerceAtLeast(1)
        .coerceAtMost(3)
}
