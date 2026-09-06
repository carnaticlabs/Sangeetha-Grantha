pluginManagement {
    repositories {
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

rootProject.name = "SangitaGranthaKMP"

include(
    ":modules:shared:domain",
    ":modules:shared:mobile-data",
    ":modules:shared:presentation",
    ":modules:mobile:androidApp",
    ":modules:backend:api",
    ":modules:backend:dal",
    ":modules:backend:test-support"
    // frontend admin web will be wired via bun/Vite, not Gradle
    // iOS host is the Xcode project under modules/mobile/iosApp
)
