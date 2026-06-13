plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "iso.consolator"

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
    implementation(project(":abstract"))
    implementation(project(":context"))
    implementation(project(":database"))

    implementation(libs.androidx.activity)
    implementation(libs.androidx.fragment)
    implementation(libs.androidx.lifecycle)
    implementation(libs.androidx.lifecycle.livedata)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.http)
    implementation(libs.androidx.room)
    implementation(libs.ktx.json)
    implementation(libs.reflect)

    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}