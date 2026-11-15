import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

/** 루트 .env (KEY=VALUE). 없으면 빈 값으로 빌드되고, 앱은 ON_DEVICE 음성으로만 동작한다. */
val env = Properties().apply {
    val f = rootProject.file(".env")
    if (f.exists()) f.readLines()
        .map { it.trim() }
        .filter { it.isNotEmpty() && !it.startsWith("#") && it.contains("=") }
        .forEach { line -> setProperty(line.substringBefore("=").trim(), line.substringAfter("=").trim()) }
}
/** 릴리스 서명: 루트 keystore.properties (커밋 금지). 없으면 release 는 서명 없이 빌드된다. */
val signing = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

fun envField(key: String) = "\"" + (System.getenv(key) ?: env.getProperty(key, "")) + "\""

android {
    namespace = "com.yikihyub.drivingpromise"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.yikihyub.drivingpromise"
        // Pleos Playground 배포 앱은 API 28 이상 필수
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"

        buildConfigField("String", "PLEOS_PROJECT_ID", envField("PLEOS_PROJECT_ID"))
        buildConfigField("String", "PLEOS_CLIENT_ID", envField("PLEOS_CLIENT_ID"))
        buildConfigField("String", "PLEOS_CLIENT_SECRET", envField("PLEOS_CLIENT_SECRET"))
        buildConfigField("String", "PLEOS_CRN", envField("PLEOS_CRN"))
    }

    signingConfigs {
        if (signing.getProperty("storeFile") != null) create("release") {
            storeFile = file(signing.getProperty("storeFile"))
            storePassword = signing.getProperty("storePassword")
            keyAlias = signing.getProperty("keyAlias")
            keyPassword = signing.getProperty("keyPassword")
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("release")
            // 쓰지 않는 아이콘만 덜어 낸다 (proguard-rules.pro). SDK 클래스는 모두 유지
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    // CarUxRestrictionsManager(주행 중 UX 제한), CarPropertyManager(차속)
    useLibrary("android.car")
}

dependencies {
    // Pleos Connect SDK 5종
    implementation("ai.pleos.playground:Vehicle:2.3.2")
    implementation("ai.pleos.playground:FusedLocation:1.1.3")
    implementation("ai.pleos.playground:SpeechToText:3.3.1")
    implementation("ai.pleos.playground:TextToSpeech:3.3.1")
    implementation("ai.pleos.playground:NaviHelper:3.0.5")

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    implementation(platform("androidx.compose:compose-bom:2024.09.03"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
}
