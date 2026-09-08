import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.dependency.analysis)
}

fun String.isNonStable(): Boolean {
    val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { uppercase().contains(it) }
    val stableVersion = "^[0-9,.v-]+(-r)?$".toRegex()
    return !stableKeyword && !stableVersion.matches(this)
}

tasks.withType<DependencyUpdatesTask>().configureEach {
    revision = "release"
    rejectVersionIf {
        candidate.version.isNonStable()
    }
}
