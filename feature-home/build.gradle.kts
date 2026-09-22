import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "feature-home"
            isStatic = true
        }
    }

    androidLibrary {
        namespace = "com.yusufteker.planora.feature.home"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }

        withHostTest {
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.core)
            implementation(projects.shared)

            // Compose
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.materialIconsExtended)
            implementation(libs.compose.ui)

            // Lifecycle
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)

            // Koin
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)

            // Kotlinx
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.compose.components.resources)

            // Paging
            implementation(libs.paging.compose)
            implementation(libs.paging.common)

            implementation(libs.kotlinx.datetime)

            // Markdown & Rich Editor
            implementation(libs.compose.markdown)
            implementation(libs.compose.markdown.m3)
            implementation(libs.richeditor.compose)

            // Peekaboo Image Picker
            implementation(libs.peekaboo.image.picker)
            implementation(libs.compottie)
            implementation(libs.compottie.dot)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.turbine)
            @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
            implementation(compose.uiTest)
        }
    }
}
