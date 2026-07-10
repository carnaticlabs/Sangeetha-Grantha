plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
}

kotlin {
    androidLibrary {
        namespace = "com.sangita.grantha.shared.domain"
        compileSdk = 36
        minSdk = 24
    }
    jvm()

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    jvmToolchain(25)

    // -Xexplicit-backing-fields removed: part of the language since Kotlin 2.4
    // (the flag now only emits a redundancy warning — zero-warnings target).

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlinx.datetime)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.bundles.ktor.client)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotlinx.coroutines.test)
            }
        }

        val jvmMain by getting {
            dependencies {
                implementation(libs.kotlinx.datetime)
            }
        }
        val jvmTest by getting

        val androidMain by getting {
            dependencies {
                implementation(libs.ktor.client.okhttp)
            }
        }

        val iosX64Main by getting {
            dependencies {
                implementation(libs.ktor.client.darwin)
                implementation(libs.kotlinx.datetime)
            }
        }
        val iosArm64Main by getting {
            dependencies {
                implementation(libs.ktor.client.darwin)
                implementation(libs.kotlinx.datetime)
            }
        }
        val iosSimulatorArm64Main by getting {
            dependencies {
                implementation(libs.ktor.client.darwin)
                implementation(libs.kotlinx.datetime)
            }
        }

        val iosX64Test by getting
        val iosArm64Test by getting
        val iosSimulatorArm64Test by getting
    }

    targets.withType(org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget::class.java)
        .configureEach {
            binaries.framework {
                baseName = "domain"
                isStatic = true
            }
            binaries.all {
                if (buildType.name.equals("DEBUG", ignoreCase = true)) {
                    // 'none' renamed to 'noop' in Kotlin/Native 2.4
                    freeCompilerArgs += listOf("-Xbinary=sourceInfoType=noop")
                }
            }
        }
}
