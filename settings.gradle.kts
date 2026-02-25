pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven { url = uri("https://jitpack.io") }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS) // Mantenemos el modo estricto
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") } // Repositorio AÑADIDO DE NUEVO
        maven { url = uri("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/kotlin-js-wrappers") }
    }
}

rootProject.name = "EntroYa"
include(":app")
