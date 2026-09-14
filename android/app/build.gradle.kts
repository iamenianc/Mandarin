import com.android.build.gradle.tasks.MergeSourceSetFolders
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.time.Instant
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

val repoRootDir = rootProject.projectDir.parentFile
val generatedContentAssetsDir = layout.buildDirectory.dir("generated/contentAssets")
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use { load(it) }
    }
}

android {
    namespace = "com.learnhuayu.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.learnhuayu"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }

    signingConfigs {
        if (keystorePropertiesFile.exists()) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = if (keystorePropertiesFile.exists()) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    sourceSets {
        getByName("main") {
            assets.srcDir(generatedContentAssetsDir)
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

val validateContent by tasks.registering(Exec::class) {
    group = "verification"
    description = "Validates the bundled content corpus with node assets/content/validate.mjs."
    workingDir(repoRootDir)
    commandLine("node", "assets/content/validate.mjs")
}

val syncContentAssets by tasks.registering(Sync::class) {
    group = "build"
    description = "Copies validated content and audio into the generated app assets directory."
    dependsOn(validateContent)
    into(generatedContentAssetsDir)
    from(repoRootDir.resolve("assets/content")) { into("content") }
    from(repoRootDir.resolve("assets/audio/reference")) { into("audio/reference") }
    from(repoRootDir.resolve("assets/audio/drills")) { into("audio/drills") }
    from(repoRootDir.resolve("assets/audio/samples")) { into("audio/samples") }
}

tasks.named("preBuild") {
    dependsOn(validateContent)
}

tasks.withType<MergeSourceSetFolders>().configureEach {
    dependsOn(syncContentAssets)
}

tasks.matching { it.name.contains("lint", ignoreCase = true) }.configureEach {
    dependsOn(syncContentAssets)
}

val deployToDrive by tasks.registering {
    group = "distribution"
    description = "Builds the release APK and copies it to the Google Drive apps folder."
    dependsOn("assembleRelease")

    val driveDirPath = (project.findProperty("driveDir") as String?) ?: "G:/My Drive/myApps"
    val apkFileName = (project.findProperty("apkName") as String?) ?: "LearnHuayu.apk"
    val releaseApk = layout.buildDirectory.file("outputs/apk/release/app-release.apk")

    doLast {
        val driveDir = file(driveDirPath)
        if (!driveDir.isDirectory) {
            throw GradleException(
                "Google Drive folder not found: ${driveDir.absolutePath}. Start Google Drive for Desktop, or pass -PdriveDir=<path>.",
            )
        }
        val apk = releaseApk.get().asFile
        if (!apk.isFile) {
            throw GradleException("Release APK not found: ${apk.absolutePath}")
        }
        val destination = driveDir.resolve(apkFileName)
        apk.copyTo(destination, overwrite = true)
        println("Deployed ${destination.name} (${destination.length()} bytes) to ${destination.absolutePath}")
        println("Deployed at ${Instant.ofEpochMilli(destination.lastModified())}")
    }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:audio"))
    implementation(project(":core:data"))
    implementation(project(":core:ai"))
    implementation(project(":core:assessment"))
    implementation(project(":core:ui"))
    implementation(project(":feature:home"))
    implementation(project(":feature:tones"))
    implementation(project(":feature:vocabulary"))
    implementation(project(":feature:listening"))
    implementation(project(":feature:speech"))
    implementation(project(":feature:fundamentals"))
    implementation(project(":feature:field"))
    implementation(project(":feature:raymond"))
    implementation(project(":feature:conversation"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.core)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}
