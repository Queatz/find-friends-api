val ktorVersion: String by project
val kotlinVersion: String by project
val logbackVersion: String by project

plugins {
    application
    kotlin("jvm") version "1.6.10"
}

group = "org.morefriends"
version = "0.0.1"
application {
    mainClass.set("org.morefriends.ApplicationKt")
}

repositories {
    mavenCentral()
    maven { url = uri("https://maven.pkg.jetbrains.space/public/p/ktor/eap") }
}

dependencies {
    implementation("io.ktor:ktor-server-compression:$ktorVersion")
    implementation("io.ktor:ktor-server-cors:$ktorVersion")
    implementation("io.ktor:ktor-server-default-headers:$ktorVersion")
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-gson:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("com.arangodb:arangodb-java-driver:6.16.0")
    implementation("com.arangodb:velocypack:2.5.4")
    implementation("com.arangodb:velocypack-module-jdk8:1.1.1")
    implementation("com.googlecode.libphonenumber:libphonenumber:8.12.42")
    implementation("com.twilio.sdk:twilio:8.25.1")
    implementation("com.sun.mail:javax.mail:1.6.2")
    testImplementation("io.ktor:ktor-server-tests:$ktorVersion")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlinVersion")
}
