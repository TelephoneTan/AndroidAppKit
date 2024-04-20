import java.util.Properties

plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    `maven-publish`
}

android {
    namespace = "pub.telephone.appKit"
    compileSdk = 34

    defaultConfig {
        minSdk = 21

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8

        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions {
        jvmTarget = "1.8"
        freeCompilerArgs += listOf(
            "-Xjvm-default=all-compatibility",
        )
    }
    buildFeatures {
        viewBinding = true
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
publishing {
    repositories {
        maven {
            url = uri("https://maven.pkg.github.com/TelephoneTan/AndroidAppKit")
            credentials {
                username = githubUser ?: System.getenv("GITHUB_USERNAME")
                password = githubKey ?: System.getenv("GITHUB_TOKEN")
            }
        }
    }
    publications {
        register<MavenPublication>("release") {
            groupId = "pub.telephone"
            artifactId = "app-kit"
            version = "1.0.0"

            afterEvaluate {
                from(components["release"])
            }
        }
    }
}

dependencies {

    implementation(kotlin("reflect"))
    api(libs.http.request)
    api(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.webkit)
    coreLibraryDesugaring(libs.android.desugar)
    api(libs.androidx.ui.graphics.android)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}