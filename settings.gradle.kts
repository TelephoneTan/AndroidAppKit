import java.util.Properties

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
//
val properties = Properties()
val propertiesFile = File(rootDir, "project.local.properties")
if (propertiesFile.exists()) {
    propertiesFile.inputStream().use { properties.load(it) }
}
//
val githubUser: String? = properties.getProperty("gpr.github.user")
val githubKey: String? = properties.getProperty("gpr.github.key")
//
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://maven.pkg.github.com/TelephoneTan/JavaHTTPRequest")
            credentials {
                username = githubUser ?: System.getenv("GITHUB_USERNAME")
                password = githubKey ?: System.getenv("GITHUB_TOKEN")
            }
        }
    }
}

rootProject.name = "AppKit"
include(":app")
include(":appKit")
