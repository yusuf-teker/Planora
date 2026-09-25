plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinSerialization)
    application
}

application {
    mainClass.set("com.yusufteker.planora.server.ApplicationKt")
}

dependencies {
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.auth.jwt)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.logback.classic)
    
    // Database & ORM
    implementation(libs.exposed.core)
    implementation(libs.exposed.dao)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.javatime)
    implementation(libs.postgresql)
    implementation(libs.hikaricp)
    implementation(libs.flyway.core)
    implementation(libs.flyway.database.postgresql)
    
    // Security
    implementation(libs.bcrypt)

    // Dotenv
    implementation(libs.dotenv.kotlin)

    // Shared module (API Models)
    implementation(project(":shared"))

    // Firebase Admin
    implementation("com.google.firebase:firebase-admin:9.2.0") {
        exclude(group = "com.google.guava", module = "listenablefuture")
    }

    // Cloudinary
    implementation("com.cloudinary:cloudinary-http44:1.36.0")

    // JavaMail for SMTP Email sending
    implementation(libs.javax.mail)

    // Testing
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kotlin.test)
}
