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

val ktlintCli by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
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
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.kotlin.test.junit)

    ktlintCli(libs.ktlint.cli)
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
        "!**/build/**",
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
        "!**/build/**",
    )
}
