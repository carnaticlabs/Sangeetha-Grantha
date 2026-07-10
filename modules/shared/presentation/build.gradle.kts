plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
}

kotlin {
    androidLibrary {
        namespace = "com.sangita.grantha.shared.presentation"
        compileSdk = 36
        minSdk = 24
    }

    // iosX64 (Intel simulator) dropped: Compose Multiplatform 1.11.x no longer
    // publishes x64 iOS artifacts. The pure-Kotlin :domain module still targets it.
    iosArm64()
    iosSimulatorArm64()

    jvmToolchain(25)

    // -Xexplicit-backing-fields removed: part of the language since Kotlin 2.4
    // (the flag now only emits a redundancy warning — zero-warnings target).

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":modules:shared:domain"))

                implementation(libs.compose.runtime)
                implementation(libs.compose.foundation)
                implementation(libs.compose.material3)
                implementation(libs.compose.material.icons.extended)
                implementation(libs.compose.ui)

                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.datetime)
                implementation(libs.kotlinx.serialization.json)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotlinx.coroutines.test)
            }
        }

        val androidMain by getting {
            dependencies {
                implementation(libs.ktor.client.okhttp)
                implementation(libs.androidx.activity.compose)
                implementation(libs.androidx.core.ktx)
            }
        }

        val iosArm64Main by getting
        val iosSimulatorArm64Main by getting

        val iosArm64Test by getting
        val iosSimulatorArm64Test by getting
    }

    targets.withType(org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget::class.java)
        .configureEach {
            binaries.framework {
                baseName = "presentation"
                isStatic = false
            }
            binaries.all {
                if (buildType.name.equals("DEBUG", ignoreCase = true)) {
                    // 'none' renamed to 'noop' in Kotlin/Native 2.4
                    freeCompilerArgs += listOf("-Xbinary=sourceInfoType=noop")
                }
            }
        }
}
