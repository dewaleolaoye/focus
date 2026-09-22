import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)
}

val releaseMetadata = Properties().apply {
    val source = rootProject.file("release.properties")
    if (source.isFile) source.reader(Charsets.UTF_8).use { load(it) }
}
fun publicValue(key: String): String = releaseMetadata.getProperty(key, "").trim()
fun quoted(value: String): String = "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"")
    .replace("\n", "\\n").replace("\r", "\\r") + "\""
val uploadKeyPath = providers.environmentVariable("FOCUS_KEYSTORE_PATH").orNull
val uploadStorePassword = providers.environmentVariable("FOCUS_KEYSTORE_PASSWORD").orNull
val uploadAlias = providers.environmentVariable("FOCUS_KEY_ALIAS").orNull
val uploadKeyPassword = providers.environmentVariable("FOCUS_KEY_PASSWORD").orNull
val signingReady = listOf(uploadKeyPath, uploadStorePassword, uploadAlias, uploadKeyPassword).all { !it.isNullOrBlank() }

android {
    namespace = "com.usefocus.app"
    compileSdk { version = release(37) { minorApiLevel = 2 } }
    defaultConfig {
        applicationId = "com.usefocus.app"
        minSdk = 26
        targetSdk = 37
        versionCode = 4
        versionName = "1.3.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "DEVELOPER_NAME", quoted(publicValue("developerName")))
        buildConfigField("String", "PRIVACY_CONTACT_EMAIL", quoted(publicValue("contactEmail")))
        buildConfigField("String", "PRIVACY_POLICY_URL", quoted(publicValue("privacyPolicyUrl")))
    }
    flavorDimensions += "distribution"
    productFlavors {
        create("sideload") {
            dimension = "distribution"
            buildConfigField("boolean", "ACCESSIBILITY_APP_BLOCKING", "true")
        }
        create("play") {
            dimension = "distribution"
            buildConfigField("boolean", "ACCESSIBILITY_APP_BLOCKING", "true")
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    signingConfigs {
        if (signingReady) create("upload") {
            storeFile = rootProject.file(uploadKeyPath!!)
            storePassword = uploadStorePassword
            keyAlias = uploadAlias
            keyPassword = uploadKeyPassword
        }
    }
    buildTypes {
        debug {
            buildConfigField("String", "ADMOB_BANNER_AD_UNIT_ID", "\"ca-app-pub-3940256099942544/6300978111\"")
        }
        release {
            if (signingReady) signingConfig = signingConfigs.getByName("upload")
            buildConfigField("String", "ADMOB_BANNER_AD_UNIT_ID", "\"ca-app-pub-7311844816795976/3006812958\"")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
    testOptions { unitTests.isReturnDefaultValues = true }
    lint { abortOnError = true }
}

ksp { arg("room.schemaLocation", "$projectDir/schemas") }

dependencies {
    implementation(libs.play.services.ads)
    implementation(libs.user.messaging.platform)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.icons)
    implementation(libs.compose.material3)
    implementation(libs.compose.preview)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.compose)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.navigation.compose)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.coroutines.android)
    testImplementation(libs.junit)
    androidTestImplementation(libs.test.runner)
    androidTestImplementation(libs.test.junit)
}
