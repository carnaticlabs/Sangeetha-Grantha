plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
}

kotlin {
    android {
        namespace = "com.sangita.grantha.shared.domain"
        compileSdk = 37
        minSdk = 24
        withHostTest {}
    }
    jvm()

    // iosX64 (Intel simulator) dropped: Compose Multiplatform 1.11+ no longer
    // publishes x64 iOS artifacts, and ARM CI/dev hosts cannot run that target.
    iosArm64()
    iosSimulatorArm64()

    jvmToolchain(25)

    // -Xexplicit-backing-fields removed: part of the language since Kotlin 2.4
    // (the flag now only emits a redundancy warning — zero-warnings target).

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.bundles.ktor.client)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
        jvmMain.dependencies {
            implementation(libs.kotlinx.datetime)
        }
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
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
