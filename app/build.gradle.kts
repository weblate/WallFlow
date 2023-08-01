@file:Suppress("UnstableApiUsage")

import com.android.build.api.variant.FilterConfiguration.FilterType.ABI
import java.util.Properties

val localProperties = Properties().apply {
    load(rootProject.file("local.properties").reader())
}

val abiCodes = mapOf("x86" to 0, "x86_64" to 1, "armeabi-v7a" to 2, "arm64-v8a" to 3)

@Suppress("DSL_SCOPE_VIOLATION") // Remove in Gradle v8.1: https://youtrack.jetbrains.com/issue/KTIJ-19369
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.hilt.gradle)
    alias(libs.plugins.ksp)
}

kapt {
    correctErrorTypes = true
}

android {
    namespace = "com.ammar.wallflow"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.ammar.wallflow"
        minSdk = 23
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // testInstrumentationRunnerArguments["useTestStorageService"] = "true"

        vectorDrawables {
            useSupportLibrary = true
        }

        // Enable room auto-migrations
        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
            arg("room.generateKotlin", "true")
        }
    }

    signingConfigs {
        create("release") {
            storeFile = file(localProperties.getProperty("release.jks.file", ""))
            storePassword = localProperties.getProperty("release.jks.password", "")
            keyAlias = localProperties.getProperty("release.jks.key.alias", "")
            keyPassword = localProperties.getProperty("release.jks.key.password", "")
        }
    }

    buildTypes {
        getByName("debug") {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-dev"
            isDebuggable = true
        }

        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }

    flavorDimensions += "feature"
    productFlavors {
        create("base") {
            dimension = "feature"
        }

        create("plus") {
            dimension = "feature"
            applicationIdSuffix = ".plus"
            versionNameSuffix = "-plus"
        }
    }

    splits {
        // Configures multiple APKs based on ABI.
        abi {
            // Enables building multiple APKs per ABI.
            isEnable = gradle.startParameter.taskNames.isNotEmpty()
                    && gradle.startParameter.taskNames[0].contains("Release")

            // Resets the list of ABIs that Gradle should create APKs for to none.
            reset()

            // Specifies a list of ABIs that Gradle should create APKs for.
            include("x86", "x86_64", "arm64-v8a", "armeabi-v7a")

            // Specifies that we want to also generate a universal APK that includes all ABIs.
            isUniversalApk = false
        }
    }

    // applicationVariants.all(ApplicationVariantAction())
    androidComponents {
        onVariants { variant ->
            variant.outputs.forEach { output ->
                val name = output.filters.find { it.filterType == ABI }?.identifier
                val baseAbiCode = abiCodes[name]
                if (baseAbiCode != null) {
                    output.versionCode.set(baseAbiCode + (output.versionCode.get() ?: 0) * 100)
                }
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
        aidl = false
        buildConfig = true
        renderScript = false
        shaders = false
    }

    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.androidxComposeCompiler.get()
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            merges += "META-INF/LICENSE.md"
            merges += "META-INF/LICENSE-notice.md"
        }
    }

    kotlin {
        jvmToolchain {
            languageVersion.set(JavaLanguageVersion.of(11))
        }

        this.sourceSets {
            debug {
                kotlin.srcDir("build/generated/ksp/debug/kotlin")
            }
            release {
                kotlin.srcDir("build/generated/ksp/release/kotlin")
            }
        }
    }

    lint {
        warning += "AutoboxingStateCreation"
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions {
        if (project.findProperty("composeCompilerReports") == "true") {
            freeCompilerArgs += listOf(
                "-P",
                "plugin:androidx.compose.compiler.plugins.kotlin:reportsDestination=${project.buildDir.absolutePath}/compose_compiler"
            )
        }
        if (project.findProperty("composeCompilerMetrics") == "true") {
            freeCompilerArgs += listOf(
                "-P",
                "plugin:androidx.compose.compiler.plugins.kotlin:metricsDestination=${project.buildDir.absolutePath}/compose_compiler"
            )
        }
    }
}


val plusImplementation by configurations

dependencies {
    // Core Android dependencies
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.activity.compose)

    // Hilt Dependency Injection
    implementation(libs.hilt.android)
    kapt(libs.hilt.android.compiler)
    kapt(libs.androidx.hilt.compiler)
    // Hilt and instrumented tests.
    androidTestImplementation(libs.hilt.android.testing)
    kaptAndroidTest(libs.hilt.android.compiler)
    // Hilt and Robolectric tests.
    testImplementation(libs.hilt.android.testing)
    kaptTest(libs.hilt.android.compiler)

    // Arch Components
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    // implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Compose
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.ui.util)
    implementation(libs.androidx.compose.material) // only for pull to refresh component
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.window.size.cls)
    // Tooling
    debugImplementation(libs.androidx.compose.ui.tooling)
    // Compose Runtime tracing
    debugImplementation(libs.androidx.compose.runtime.tracing)
    // Instrumented tests
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Compose Destinations
    implementation(libs.compose.destinations.core)
    ksp(libs.compose.destinations.ksp)

    // Retrofit
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp.logging)
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.kotlin.serialization)

    // Paging
    implementation(libs.androidx.paging.runtime)
    implementation(libs.androidx.paging.compose)
    implementation(libs.androidx.room.paging)

    // Coil
    implementation(libs.coil.compose)

    // Accompanist
    implementation(libs.accompanist.placeholder.material)
    // implementation(libs.accompanist.permission)

    // jsoup
    implementation(libs.jsoup)

    // DataStore
    implementation(libs.androidx.datastore.preferences)

    // Work
    implementation(libs.androidx.work.ktx)
    implementation(libs.androidx.hilt.work)
    androidTestImplementation(libs.androidx.work.testing)

    // easycrop
    // implementation(libs.easycrop)
    implementation(libs.easycrop.fork)

    // tf-lite
    plusImplementation(libs.tflite.task.vision)
    plusImplementation(libs.tflite.gpu.delegate.plugin)
    plusImplementation(libs.tflite.gpu)
    plusImplementation(libs.tflite.gpu.api)

    // partial
    implementation(libs.partial)
    ksp(libs.partial.ksp)

    // modern storage permissions
    implementation(libs.modernstorage.permissions)

    // cloudy
    implementation(libs.cloudy)

    // telephoto
    implementation(libs.telephoto.zoomable.image.coil)

    // kotlinx collections immutable
    implementation(libs.kotlinx.collections.immutable)

    // Local tests: jUnit, coroutines, Android runner
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.kotlin.test.junit)

    // Instrumented tests: jUnit rules and runners
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.kotlin.test.junit)
    androidTestUtil(libs.androidx.test.services)

    // mockk
    androidTestImplementation(libs.mockk.android)
}
