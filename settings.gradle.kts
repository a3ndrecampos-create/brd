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

rootProject.name = "TaPago"

include(":app")
include(":core:designsystem")
include(":core:database")
include(":core:network")
include(":core:common")
include(":feature:tracking")
include(":feature:photoshare")
