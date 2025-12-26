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
    ":modules:shared:presentation",
    ":modules:backend:api",
    ":modules:backend:dal"
    // frontend admin web will be wired via bun/Vite, not Gradle
)
