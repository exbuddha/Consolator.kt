plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "app.consolator"
    compileSdk = 36
    compileSdkPreview = "VanillaIceCream"

    buildFeatures.buildConfig = true

    defaultConfig {
        minSdk = 31

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("debug")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        targetSdk = 36
    }
}

kotlin {
    jvmToolchain(17)

    tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class.java).all {
        compilerOptions {
            progressiveMode = true
            freeCompilerArgs.add("-Xjvm-default=all")
        }
    }
}

dependencies {
    implementation(project(":context"))
    implementation(project(":database"))
    implementation(project(":framework"))

    implementation(libs.androidx.activity)
    implementation(libs.androidx.fragment)
    implementation(libs.reflect)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}