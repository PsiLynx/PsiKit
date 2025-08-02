plugins {
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.android.library)
}

android {
    namespace = "org.psilynx.psikit.test"
    //noinspection GradleDependency
    compileSdk = 33

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true

        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
        freeCompilerArgs += ("-Xjvm-default=all")
    }
}

dependencies {
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlin.reflect)
    implementation(libs.bundles.ftcsdk)

    coreLibraryDesugaring(libs.android.desugar)

    api(project(":core"))
    api(project(":ftc"))

    testImplementation(kotlin("test"))
    testImplementation(libs.junit.jupiter)
}