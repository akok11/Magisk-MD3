plugins {
    id("com.android.application")
    kotlin("plugin.parcelize")
    id("com.android.legacy-kapt")
    id("androidx.navigation.safeargs.kotlin")
}

setupMainApk()

kapt {
    correctErrorTypes = true
    useBuildCache = true
    mapDiagnosticLocations = true
    javacOptions {
        option("-Xmaxerrs", "1000")
    }
}

android {
    buildFeatures {
        // LSPosed 风格通常需要 DataBinding 配合 ViewBinding 处理复杂的 Material 3 逻辑
        dataBinding = true
        viewBinding = true 
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
    }

    defaultConfig {
        proguardFile("proguard-rules.pro")
        // 确保 targetSdk 足够高以支持 Material You 动态配色
        targetSdk = 35 
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
        }
    }

    // 解决 Rikka 库可能存在的命名空间冲突
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(project(":core"))
    coreLibraryDesugaring(libs.jdk.libs)

    // --- RikkaX / LSPosed 核心 UI 组件堆栈 ---
    implementation(libs.rikka.core)             // 基础核心逻辑
    implementation(libs.rikka.appcompat)        // LSPosed 特色的 Activity 基类
    implementation(libs.rikka.material)         // 提供 M3 风格的各种小部件
    implementation(libs.rikka.layoutinflater)   // 关键：用于注入 M3 视图
    implementation(libs.rikka.insets)           // 沉浸式状态栏/导航栏处理
    implementation(libs.rikka.recyclerview)     // LSPosed 丝滑滚动的列表

    // --- 导航与架构 ---
    implementation(libs.navigation.fragment.ktx)
    implementation(libs.navigation.ui.ktx)
    implementation(libs.fragment.ktx)

    // --- 基础 UI 库 ---
    implementation(libs.constraintlayout)
    implementation(libs.swiperefreshlayout)
    implementation(libs.recyclerview)
    implementation(libs.transition)
    implementation(libs.appcompat)
    
    // 这里的 libs.material 必须指向 1.13.0+ 以获得 M3 支持
    implementation(libs.material)

    // 旧版兼容，视情况可逐步剔除
    implementation(libs.indeterminate.checkbox)

    // Kapt 运行环境
    kapt(kotlin("stdlib"))
}
