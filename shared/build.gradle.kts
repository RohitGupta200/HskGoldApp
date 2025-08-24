import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    kotlin("plugin.serialization") version "2.2.0"
}

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    
    jvm()
    
    // Configure JVM toolchain
    jvmToolchain(11)
    
    sourceSets {
        val commonMain by getting {
            dependencies {
                // Ktor
                implementation("io.ktor:ktor-client-core:2.3.4")
                implementation("io.ktor:ktor-client-content-negotiation:2.3.4")
                implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.4")
                implementation("io.ktor:ktor-client-logging:2.3.4")
                implementation("io.ktor:ktor-client-auth:2.3.4")
                
                // Serialization
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
                
                // Coroutines
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
                
                // Koin for dependency injection (core only in common)
                implementation("io.insert-koin:koin-core:3.4.3")
                
                // DateTime for token expiration
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.1")

                // Compose dependencies required by admin UI under shared
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.ui)
                implementation(compose.material3)
                implementation(compose.materialIconsExtended)
            }
        }
        
        val androidMain by getting {
            dependencies {
                // Ktor Android engine
                implementation("io.ktor:ktor-client-android:2.3.4")

                // Koin Android for androidContext()
                implementation("io.insert-koin:koin-android:3.4.3")

                // AndroidX Security Crypto for EncryptedSharedPreferences
                implementation("androidx.security:security-crypto:1.1.0-alpha06")
            }
        }

        
        val iosMain by creating {
            dependsOn(commonMain)
            
            dependencies {
                // Ktor iOS engine
                implementation("io.ktor:ktor-client-darwin:2.3.4")
            }
        }
        
        // Connect iOS source sets to the main source set
        val iosX64Main by getting { dependsOn(iosMain) }
        val iosArm64Main by getting { dependsOn(iosMain) }
        val iosSimulatorArm64Main by getting { dependsOn(iosMain) }
        
        val jvmMain by getting {
            dependsOn(commonMain)
            dependencies {
                // Ktor JVM client dependencies for desktop/JVM usage
                implementation("io.ktor:ktor-client-java:2.3.4")
                implementation("io.ktor:ktor-client-content-negotiation:2.3.4")
                implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.4")
                implementation("io.ktor:ktor-client-logging:2.3.4")
                implementation("io.ktor:ktor-client-auth:2.3.4")
            }
        }
        
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

android {
    namespace = "org.cap.gold"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
        buildConfigField("boolean", "DEBUG", "true")
    }
    
    buildFeatures {
        buildConfig = true
        viewBinding = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    }
