plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
}

kotlin {
    android {
        namespace = "com.sangita.grantha.shared.mobile"
        compileSdk = 37
        minSdk = 24
        withHostTest {}
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
    jvm()

    iosArm64()
    iosSimulatorArm64()

    jvmToolchain(25)

    sourceSets {
        commonMain.dependencies {
            api(project(":modules:shared:domain"))
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.bundles.ktor.client)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
        }
        jvmMain.dependencies {
            implementation(libs.ktor.client.cio)
        }
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
            implementation(libs.androidx.core.ktx)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
    }

    targets.withType(org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget::class.java)
        .configureEach {
            binaries.all {
                if (buildType.name.equals("DEBUG", ignoreCase = true)) {
                    freeCompilerArgs += listOf("-Xbinary=sourceInfoType=noop")
                }
            }
        }
}
