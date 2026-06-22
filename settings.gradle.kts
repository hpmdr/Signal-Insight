pluginManagement {
    repositories {
        if (System.getenv("GITHUB_ACTIONS") == "true") {
            google()
        } else {
            maven {
                url = uri("https://maven.aliyun.com/repository/google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        if (System.getenv("GITHUB_ACTIONS") == "true") {
            google()
        } else {
            maven {
                url = uri("https://maven.aliyun.com/repository/google")
            }
        }
        mavenCentral()
    }
}

rootProject.name = "Signal Insight"
include(":app")
 