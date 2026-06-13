plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "net.consolator"

    compileSdk {
        version = release(37) {
            minorApiLevel = 0
        }
    }

    defaultConfig {
        applicationId = "net.consolator"
        minSdk = 31
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures.buildConfig = true

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debug")
            isMinifyEnabled = false

            optimization {
                enable = false
            }
        }

        release {
            isMinifyEnabled = true
            isShrinkResources = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

kotlin {
    jvmToolchain(21)

    tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class.java).all {
        compilerOptions {
            progressiveMode = true
            freeCompilerArgs.addAll(
                "-Xjvm-default=all",
                "-Xreturn-value-checker=disable",
            )
        }
    }
}

dependencies {
    implementation(project(":app"))
    implementation(project(":context"))
    implementation(project(":database"))
    implementation(project(":framework"))

    implementation(libs.androidx.activity)
    implementation(libs.androidx.fragment)
    implementation(libs.reflect)

    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}