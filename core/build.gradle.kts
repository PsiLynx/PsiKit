plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-library`
    `maven-publish`
}

dependencies {
    implementation(libs.kotlinx.datetime)

    implementation(libs.bundles.jackson)
    implementation(libs.quickbuf)

    testImplementation(kotlin("test"))
    testImplementation(libs.junit.jupiter)
}

java {
    withSourcesJar()
    withJavadocJar()
}

publishing {
    publications {
        create<MavenPublication>("release") {
            from(components["java"])

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