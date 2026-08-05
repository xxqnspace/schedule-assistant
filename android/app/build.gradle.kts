plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.scheduleassistant.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.scheduleassistant.app"
        minSdk = 26
        targetSdk = 34
        // 修复（L10）：版本号由 CI 构建序号注入（本地默认 1）
        versionCode = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 1
        versionName = System.getenv("GITHUB_RUN_NUMBER")?.let { "1.0.$it" } ?: "1.0.0"
    }

    signingConfigs {
        create("release") {
            // 仅在 CI 通过环境变量（GitHub Secrets）提供密钥时生效；
            // 本地未设置不影响 assembleDebug，也不强制生成 release 包。
            val storeFilePath = System.getenv("KEYSTORE_FILE")
            val storePassword = System.getenv("KEYSTORE_PASSWORD")
            val keyAlias = System.getenv("KEY_ALIAS")
            val keyPassword = System.getenv("KEY_PASSWORD")
            if (storeFilePath != null && storePassword != null && keyAlias != null && keyPassword != null) {
                storeFile = file(storeFilePath)
                this.storePassword = storePassword
                this.keyAlias = keyAlias
                this.keyPassword = keyPassword
            }
        }
    }

    buildTypes {
        release {
            // 修复（L8）：开启混淆与资源裁剪（Room/JSON 的 keep 规则见 proguard-rules.pro）
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // 配置了密钥才启用签名；未配置时 assembleRelease 会提示缺少密钥（本地调试用 debug 即可）
            signingConfig = signingConfigs.getByName("release")
        }
    }

    // 修复（L10）：lint 发现问题不中断构建（报告仍生成）
    lint {
        abortOnError = false
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
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Compose BOM 统一管理 Compose 版本
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.2")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.2")

    // 本地存储：Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    implementation("androidx.core:core-ktx:1.13.1")

    // ⑧ 背景图加载（URL / 本地文件）
    implementation("io.coil-kt:coil-compose:2.6.0")

    debugImplementation("androidx.compose.ui:ui-tooling")

    // 单元测试（L10）：ScheduleUtils/DateUtils 纯逻辑
    testImplementation("junit:junit:4.13.2")
}
