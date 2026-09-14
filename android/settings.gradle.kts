pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
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

rootProject.name = "LearnHuayu"

include(":app")
include(":core:model")
include(":core:audio")
include(":core:data")
include(":core:ai")
include(":core:assessment")
include(":core:ui")
include(":feature:home")
include(":feature:tones")
include(":feature:vocabulary")
include(":feature:listening")
include(":feature:speech")
include(":feature:fundamentals")
include(":feature:field")
include(":feature:raymond")
include(":feature:conversation")
