plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.chc.filetransferandroid"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.chc.filetransferandroid"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // 添加签名配置
    signingConfigs {
        create("release") {
            val storePath = System.getenv("RELEASE_STORE_FILE") ?: "debug.keystore"
            println("使用密钥文件: $storePath")

            storeFile = file(storePath)
            storePassword = System.getenv("RELEASE_STORE_PASSWORD") ?: ""
            keyAlias = System.getenv("RELEASE_KEY_ALIAS") ?: ""
            keyPassword = System.getenv("RELEASE_KEY_PASSWORD") ?: ""

            if (!storeFile!!.exists()) {
                println("❌ 错误: 密钥文件不存在!")
                println("当前路径: ${System.getProperty("user.dir")}")
                println("文件路径: ${storeFile!!.absolutePath}")
                println("目录内容:")
                file(storeFile!!.parent).listFiles()?.forEach { println(" - ${it.name}") }
            } else {
                println("✅ 密钥文件存在")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // 应用release签名配置
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    implementation(libs.androidx.core.splashscreen)
    implementation(libs.jmdns)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.okhttp)
    implementation(project(":ktor-server"))

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}