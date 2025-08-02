plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.android) apply false

    alias(libs.plugins.android.library) apply false

    alias(libs.plugins.dokka)

    `maven-publish`
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}

dependencies {
    dokka(project(":core"))
    dokka(project(":ftc"))
}

allprojects {

}