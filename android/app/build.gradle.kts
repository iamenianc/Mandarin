import com.android.build.gradle.tasks.MergeSourceSetFolders
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

val repoRootDir = rootProject.projectDir.parentFile
val generatedContentAssetsDir = layout.buildDirectory.dir("generated/contentAssets")

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

    buildTypes {
        release {
            isMinifyEnabled = false
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
}
