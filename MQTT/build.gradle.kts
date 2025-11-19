import com.android.build.api.dsl.androidLibrary
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.vanniktech.mavenPublish)
}

group = "io.github.hussein-al-zuhile"
version = "1.0.1"

kotlin {
    jvm()
    androidLibrary {
        namespace = "io.github.hussein-al-zuhile.kmp-mqtt"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        withJava() // enable java compilation support
        withHostTestBuilder {}.configure {}
        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }

        compilations.configureEach {
            compilerOptions.configure {
                jvmTarget.set(
                    JvmTarget.JVM_11
                )
            }
        }
    }
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            //noinspection UseTomlInstead
            api("de.kempmobil.ktor.mqtt:mqtt-core:0.8.0")
            api("de.kempmobil.ktor.mqtt:mqtt-client:0.8.0")

            api("org.jetbrains.kotlinx:kotlinx-io-bytestring:0.8.0")
            api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")

            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")

            implementation("io.github.microutils:kotlin-logging:3.0.5")
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

publishing {

    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/hussein-al-zuhile/KMP-MQTT")
        }
    }
}


mavenPublishing {
    publishToMavenCentral()

    signAllPublications()

    coordinates(group.toString(), "kmp-mqtt", version.toString())

    pom {
        name = "KMP - MQTT"
        description = "A library for MQTT easy plug and play usage, based on ktor-mqtt library."
        inceptionYear = "2025"
        url = "https://github.com/Hussein-Al-Zuhile/KMP-MQTT"
        licenses {
            license {
                name = "The Apache License, Version 2.0"
                url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                distribution = "https://www.apache.org/licenses/LICENSE-2.0.txt"
            }
        }
        developers {
            developer {
                id = "Hussein-Al-Zuhile"
                name = "Hussein Al-Zuhile"
                url = "https://github.com/Hussein-Al-Zuhile/"
                email = "hosenzuh@gmail.com"
                organization = "Hussein Al-Zuhile"
                organizationUrl = "https://github.com/Hussein-Al-Zuhile/"
            }
        }
        scm {
            url = "https://github.com/Hussein-Al-Zuhile/KMP-MQTT"
            connection = "scm:git:git://github.com/Hussein-Al-Zuhile/KMP-MQTT.git"
            developerConnection = "scm:git:ssh://git@github.com/Hussein-Al-Zuhile/KMP-MQTT.git"
        }
    }
}
