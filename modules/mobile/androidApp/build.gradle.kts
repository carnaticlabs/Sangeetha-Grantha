plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.jetbrainsCompose)
}

android {
    namespace = "com.sangita.grantha.rasika"
    compileSdk = 37
    defaultConfig {
        applicationId = "com.sangita.grantha.rasika"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"
    }
    buildTypes {
        getByName("debug") {
            isDebuggable = true
        }
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

// ---------------------------------------------------------------------------
// Compose Multiplatform resource packaging workaround (TRACK-138).
//
// :modules:shared:presentation uses the AGP KMP library target
// (com.android.kotlin.multiplatform.library). The Compose Gradle plugin does not
// wire its Compose-resources -> Android-assets bridge for that target: the
// generated copyAndroidMainComposeResourcesToAndroidAssets task has no configured
// output directory, so runtime resources (Fraunces / Work Sans fonts, trinity.png)
// never reach the APK and Res.font.* / Res.drawable.* silently fall back.
//
// We copy the module's prepared commonMain composeResources into this app's assets
// under the package-qualified path the resource reader expects
// (composeResources/com.sangita.grantha.shared.presentation/...). iOS and JVM are
// unaffected — they assemble resources correctly. Remove once the Compose plugin
// supports the KMP android library target.
// Ref: application_documentation/05-frontend/mobile/track-138-visual-design.md
// ---------------------------------------------------------------------------
val presentationResPackage = "com.sangita.grantha.shared.presentation"
val syncPresentationComposeResources = tasks.register<Sync>("syncPresentationComposeResources") {
    val presentation = project(":modules:shared:presentation")
    dependsOn("${presentation.path}:prepareComposeResourcesTaskForCommonMain")
    from(
        presentation.layout.buildDirectory.dir(
            "generated/compose/resourceGenerator/preparedResources/commonMain/composeResources",
        ),
    )
    into(
        layout.buildDirectory.dir(
            "generated/composeResourcesAssets/composeResources/$presentationResPackage",
        ),
    )
}

android {
    sourceSets.getByName("main").assets.srcDir(
        layout.buildDirectory.dir("generated/composeResourcesAssets").get().asFile,
    )
}

tasks.matching { it.name.startsWith("merge") && it.name.endsWith("Assets") }
    .configureEach { dependsOn(syncPresentationComposeResources) }

dependencies {
    implementation(project(":modules:shared:presentation"))
    implementation(project(":modules:shared:mobile-data"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
}
