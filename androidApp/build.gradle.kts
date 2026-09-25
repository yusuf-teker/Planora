import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)

    // Baseline Profile Eklentisi:
    // Bu eklenti, :baselineprofile modülünde üretilen profil kurallarını (baseline-prof.txt)
    // otomatik olarak alıp release APK / AAB paketine dahil eder.
    alias(libs.plugins.androidBaselineProfile)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}

dependencies {
    implementation(projects.composeApp)
    implementation(projects.core)
    implementation(projects.featureAuth)
    implementation(projects.featureHome)
    implementation(projects.shared)

    implementation(libs.kotlinx.datetime)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation(libs.compose.uiToolingPreview)
    debugImplementation(libs.compose.uiTooling)
    debugImplementation(libs.leakcanary.android)

    // Baseline Profile Installer:
    // Google Play olmadan manuel yüklemelerde ya da cihaz ilk açıldığında
    // Android Runtime'a (ART) baseline profilini okutup AOT (Ahead-of-Time) derlemesini tetikleyen kütüphane.
    implementation(libs.androidx.profileinstaller)

    // Baseline Profile Üretici Modülü:
    // androidApp'in profil kurallarını :baselineprofile test modülünden almasını ve
    // ./gradlew :androidApp:generateBaselineProfile komutu verildiğinde o modülü tetiklemesini sağlar.
    baselineProfile(projects.baselineprofile)

    // Koin Android (for androidContext in PlanoraApplication)
    implementation(project.dependencies.platform(libs.koin.bom))
    implementation(libs.koin.android)
    implementation(libs.napier)
    implementation("com.google.firebase:firebase-messaging-ktx:24.0.0")
}

android {
    namespace = "com.yusufteker.planora"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.yusufteker.planora"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0.0"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        buildConfig = true
    }

    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }

    sourceSets {
        getByName("main") {
            assets.srcDirs(
                project(":core").file("build/generated/compose/assets")
            )
        }
    }
}

afterEvaluate {
    tasks.configureEach {
        if ((name.startsWith("merge") && name.endsWith("Assets")) || name.contains("Lint", ignoreCase = true)) {
            dependsOn(project(":core").tasks.matching { 
                (it.name.contains("ComposeResources") || it.name.contains("ValueResources")) && 
                !it.name.contains("Ios") && 
                !it.name.contains("Apple") && 
                !it.name.contains("Native")
            })
        }
    }
}