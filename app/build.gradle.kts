import org.gradle.api.attributes.Bundling
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.dependency.analysis)
}

android {
    namespace = "io.github.alexanderwilhelmsenberg.oneuiorganizer"
    compileSdk = 37
    compileSdkMinor = 0

    defaultConfig {
        applicationId = "io.github.alexanderwilhelmsenberg.oneuiorganizer"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0-dev"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        abortOnError = true
        warningsAsErrors = true
        // These version-policy checks conflict with frozen baseline decisions: API 37 is compile-only,
        // and Kotlin 2.4.20's fully-supported ceiling is AGP 9.3.1 / Gradle 9.7.0.
        disable += setOf("AndroidGradlePluginVersion", "OldTargetApi")
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
        allWarningsAsErrors = true
    }
}

val ktlintCli = configurations.create("ktlintCli") {
    isCanBeConsumed = false
    isCanBeResolved = true
}

val ktlintCliDependency = libs.ktlint.cli.get().copy().apply {
    attributes {
        attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.EXTERNAL))
    }
}

val java17Launcher = javaToolchains.launcherFor {
    languageVersion = JavaLanguageVersion.of(17)
}

dependencies {
    implementation(libs.androidx.activity)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.compose.runtime.saveable)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.text)
    implementation(libs.androidx.compose.ui.unit)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.foundation.layout)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.datastore.core)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.kotlin.test.junit)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestRuntimeOnly(libs.androidx.test.runner)
    debugRuntimeOnly(libs.androidx.compose.ui.test.manifest)

    add(ktlintCli.name, ktlintCliDependency)
}

tasks.register<JavaExec>("ktlintCheck") {
    group = "verification"
    description = "Check Kotlin formatting with ktlint 1.8.0 on the Java 17 toolchain."
    classpath = ktlintCli
    mainClass.set("com.pinterest.ktlint.Main")
    javaLauncher.set(java17Launcher)
    workingDir(rootProject.projectDir)
    args(
        "--relative",
        "**/src/**/*.kt",
        "**.kts",
        "!**/build/**"
    )
}

tasks.register<JavaExec>("ktlintFormat") {
    group = "formatting"
    description = "Format Kotlin sources with ktlint 1.8.0 on the Java 17 toolchain."
    classpath = ktlintCli
    mainClass.set("com.pinterest.ktlint.Main")
    javaLauncher.set(java17Launcher)
    workingDir(rootProject.projectDir)
    args(
        "-F",
        "--relative",
        "**/src/**/*.kt",
        "**.kts",
        "!**/build/**"
    )
}
