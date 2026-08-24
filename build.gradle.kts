plugins {
    kotlin("jvm") version "2.2.21"
    kotlin("plugin.spring") version "2.2.21"
    kotlin("plugin.jpa") version "2.3.20"
    id("org.springframework.boot") version "4.0.3"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.fancia.backend"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(24)
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
    }
}

fun RepositoryHandler.codeArtifactRepo(repoName: String) {
    val baseUrl = System.getenv("ARTIFACT_REPO_URL") ?: project.findProperty("ARTIFACT_REPO_URL") as String?
        ?: return
    maven {
        url = uri("$baseUrl/$repoName/")
        credentials {
            username = System.getenv("ARTIFACT_REPO_USER") ?: project.findProperty("ARTIFACT_REPO_USER") as String? ?: "aws"
            password =
                System.getenv("ARTIFACT_REPO_PASSWORD") ?: project.findProperty("ARTIFACT_REPO_PASSWORD") as String? ?: ""
        }
    }
}

repositories {
    mavenCentral()
    maven { url = uri("https://repo.spring.io/snapshot") }
    codeArtifactRepo("fancia-backend-shared-common")
    codeArtifactRepo("fancia-backend-shared-user")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:2025.1.1")
    }
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.data:spring-data-commons")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.postgresql:postgresql")
    implementation("org.apache.commons:commons-lang3:3.18.0")
    implementation("software.amazon.awssdk:sts:2.42.4")
    implementation("io.awspring.cloud:spring-cloud-aws-starter-secrets-manager:4.0.0")
    implementation("com.amazonaws.secretsmanager:aws-secretsmanager-jdbc:2.0.4")
    implementation("com.fancia.backend.shared:common:0.0.1-SNAPSHOT")
    implementation("com.fancia.backend.shared:user:0.0.1-SNAPSHOT")
    implementation("com.amazonaws:aws-lambda-java-core:1.2.3")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("io.kotest:kotest-runner-junit5:6.1.7")
    testImplementation("io.kotest:kotest-assertions-core:6.1.7")
    testImplementation("io.kotest:kotest-extensions-spring:6.1.7")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.jar {
    enabled = false
}

val lambdaClasspathDir = layout.buildDirectory.dir("lambda-classpath")

tasks.register<Sync>("prepareLambdaClasspath") {
    group = "build"
    description = "Flatten Spring Boot jar into Lambda classpath layout"
    dependsOn(tasks.bootJar)
    into(lambdaClasspathDir)

    from(zipTree(tasks.bootJar.flatMap { it.archiveFile })) {
        include("BOOT-INF/classes/**")
        eachFile {
            relativePath = RelativePath(true, *relativePath.segments.drop(2).toTypedArray())
        }
        includeEmptyDirs = false
    }
    from(zipTree(tasks.bootJar.flatMap { it.archiveFile })) {
        include("BOOT-INF/lib/**")
        eachFile {
            relativePath = RelativePath(true, "lib", *relativePath.segments.drop(2).toTypedArray())
        }
        includeEmptyDirs = false
    }
}

tasks.register<Zip>("lambdaZip") {
    group = "build"
    description = "AWS Lambda Zip package (BOOT-INF flattened for Java handler)"
    archiveFileName.set("${project.name}-lambda.zip")
    destinationDirectory.set(layout.buildDirectory.dir("distributions"))
    isZip64 = true

    dependsOn("prepareLambdaClasspath")
    from(lambdaClasspathDir)
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.test {
    environment(System.getenv())
}
