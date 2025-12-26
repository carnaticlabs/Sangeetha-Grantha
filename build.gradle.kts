plugins {
    // Kotlin + Compose plugin aliases (versions from version catalog)
    alias(libs.plugins.jetbrainsCompose) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinAndroid) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinSerialization) apply false

    // Android Gradle Plugins
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.androidKotlinMultiplatformLibrary) apply false
}

allprojects {
    group = "com.sangita.grantha"
    version = "0.1.0-SNAPSHOT"
}

subprojects {
    // Enforce Java 25 toolchain for all Kotlin projects (matches Kailash Yatra setup)
    plugins.withType<org.jetbrains.kotlin.gradle.plugin.KotlinBasePluginWrapper> {
        extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension> {
            jvmToolchain(25)
        }
        tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask<*>>().configureEach {
            compilerOptions {
                freeCompilerArgs.addAll(
                    "-Xskip-prerelease-check",
                    "-opt-in=kotlin.uuid.ExperimentalUuidApi"
                )
            }
        }
    }
}
