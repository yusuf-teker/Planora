import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.sqldelight)
    alias(libs.plugins.buildkonfig)
}


kotlin {
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "core"
            isStatic = true
            linkerOpts("-framework", "NaturalLanguage", "-framework", "CoreML")
        }
    }

    androidLibrary {
        namespace = "com.yusufteker.planora.core"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }

        withHostTest {
        }
    }

    sourceSets {
        androidMain.dependencies {
            
        }
        commonMain.dependencies {
            // Compose
            api(libs.compose.runtime)
            api(libs.compose.foundation)
            api(libs.compose.material3)
            api(libs.compose.ui)
            api(libs.compose.components.resources)

            // Logging (KMP)
            api(libs.napier)

            // Settings
            api(libs.multiplatform.settings)

            // Lifecycle
            api(libs.androidx.lifecycle.viewmodelCompose)
            api(libs.androidx.lifecycle.runtimeCompose)

            // Navigation 3
            api(libs.jetbrains.navigation3.ui)

            // Koin
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)

            // Kotlinx
            api(libs.kotlinx.serialization.json)
            api(libs.kotlinx.coroutines.core)
            api(libs.kotlinx.datetime)
            api(libs.compose.materialIconsExtended)
            implementation(libs.kotlinx.datetime)


            // DataStore
            api(libs.datastore.preferences.core)

            // Ktor Client
            api(libs.ktor.client.core)
            api(libs.ktor.client.content.negotiation)
            api(libs.ktor.client.logging)
            api(libs.ktor.client.auth)
            api(libs.ktor.serialization.kotlinx.json)

            // Paging & Database
            api(libs.paging.common)
            api(libs.sqldelight.coroutines)

            // Coil
            api(libs.coil.compose)
            api(libs.coil.network.ktor)

            // Peekaboo Image Picker
            api(libs.peekaboo.image.picker)

            implementation(project(":shared"))
        }

        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.androidx.security.crypto)
            implementation(libs.sqldelight.android.driver)
            implementation(project.dependencies.platform("com.google.firebase:firebase-bom:33.1.2"))
            implementation(libs.firebase.analytics)
            implementation(libs.firebase.messaging)
            implementation(libs.firebase.crashlytics)
            // Google AI Edge SDK — eklendiğinde Gemini Nano otomatik kullanılır.
            // compileOnly(libs.google.ai.edge.aicore)
        }

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
            implementation(libs.sqldelight.native.driver)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

compose.resources {
    publicResClass = true
}

sqldelight {
    databases {
        create("PlanoraDatabase") {
            packageName.set("com.yusufteker.planora.core.database")
        }
    }
}

// Workaround for Compose Multiplatform bug with androidMultiplatformLibrary
tasks.matching { it.name == "copyAndroidMainComposeResourcesToAndroidAssets" }.configureEach {
    val dir = layout.buildDirectory.dir("generated/compose/assets")
    try {
        val method = this::class.java.methods.firstOrNull { it.name == "getOutputDirectory" }
        val prop = method?.invoke(this) as? org.gradle.api.file.DirectoryProperty
        prop?.set(dir)
    } catch (e: Exception) {}
}

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use { load(it) }
    }
}

buildkonfig {
    packageName = "com.yusufteker.planora.core.config"
    objectName = "BuildConfig"

    defaultConfigs {
        buildConfigField(
            com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING,
            "GEMINI_API_KEY",
            localProperties.getProperty("GEMINI_API_KEY") ?: System.getenv("GEMINI_API_KEY") ?: ""
        )
        buildConfigField(
            com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING,
            "API_NINJAS_KEY",
            localProperties.getProperty("API_NINJAS_KEY") ?: System.getenv("API_NINJAS_KEY") ?: ""
        )
    }
}

