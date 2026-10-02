plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.testdone.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.testdone.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 20024
        versionName = "2.3.17"

        vectorDrawables { useSupportLibrary = true }
    }

    signingConfigs {
        create("release") {
            // Signing keystore repo me KABHI commit nahi hota — CI GitHub Secrets
            // se decode karta hai; local build: TD_* env vars set karo.
            val ksFile = rootProject.file("keystore/testdone-release.keystore")
            if (ksFile.exists()) {
                storeFile = ksFile
                storePassword = System.getenv("TD_STORE_PASS") ?: error("TD_STORE_PASS env var missing")
                keyAlias = System.getenv("TD_KEY_ALIAS") ?: error("TD_KEY_ALIAS env var missing")
                keyPassword = System.getenv("TD_KEY_PASS") ?: error("TD_KEY_PASS env var missing")
            }
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }


    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlinOptions {
        jvmTarget = "21"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    androidResources {
        noCompress += "json"
    }
}

dependencies {
    // ── Compose ─────────────────────────────────────────────────────────────
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.animation:animation")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // ── Core / lifecycle ────────────────────────────────────────────────────
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-process:2.8.7")

    // ── Navigation ──────────────────────────────────────────────────────────
    implementation("androidx.navigation:navigation-compose:2.8.5")

    // ── Room (offline-first storage) ────────────────────────────────────────
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // ── DataStore (preferences + session) ───────────────────────────────────
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // ── Network: OkHttp (content pack downloads) + kotlinx.serialization ─────
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    // ── Payments: Razorpay Checkout (v2.3.16 — DISABLED by default; the admin
    // console flips app_config/payments.enabled when you're ready to charge) ──
    implementation("com.razorpay:checkout:1.6.41")

    // ── Coroutines ──────────────────────────────────────────────────────────
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.9.0") // Task.await()

    // ── Firebase (Spark/free: Auth + Firestore + Storage-backed content) ─────
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    // v2.3.8: Remote Config — phone login mode flip (password ↔ SMS OTP) without
    // rebuilding the APK. Free on the Spark plan.
    implementation("com.google.firebase:firebase-config")

    // ── Credential Manager + Google ID (native one-tap Google sign-in) ──────
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")

    // ── Images ──────────────────────────────────────────────────────────────
    implementation("io.coil-kt:coil-compose:2.7.0")

    // ── Tests ───────────────────────────────────────────────────────────────
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
}
