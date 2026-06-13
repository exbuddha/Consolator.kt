plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "app.consolator"

    compileSdk {
        version = release(37) {
            minorApiLevel = 0
        }
    }

    defaultConfig {
        minSdk = 31

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures.buildConfig = true

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    testOptions {
        targetSdk = 37
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
    implementation(project(":context"))
    implementation(project(":framework"))

    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}