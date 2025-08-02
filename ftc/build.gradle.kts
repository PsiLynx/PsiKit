plugins {
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.android.library)

    `maven-publish`
}

android {
    namespace = "org.psilynx.psikit.ftc"
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

    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }
}

dependencies {
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlin.reflect)
    implementation(libs.bundles.ftcsdk)
    implementation(libs.bundles.jackson)

    coreLibraryDesugaring(libs.android.desugar)

    api(project(":core"))

    testImplementation(kotlin("test"))
    testImplementation(libs.junit.jupiter)
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])

                groupId = "org.psilynx.psikit"
                artifactId = project.name
                version = project.version.toString()
            }
        }

        repositories {
            maven {
                name = "dairyPrivate"
                url = uri("https://repo.dairy.foundation/private")
                credentials(PasswordCredentials::class)
            }
        }
    }
}