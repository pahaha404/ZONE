pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "ZONE"

include(
    ":app",
    ":core:model",
    ":core:database",
    ":core:datastore",
    ":core:common",
    ":core:ui",
    ":feature:library",
    ":feature:session",
    ":feature:player",
    ":feature:sensors",
    ":feature:focuscamera",
    ":feature:report",
)

