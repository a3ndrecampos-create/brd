import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

// Bluesoft Cosmos: base de produtos brasileira usada como primeira tentativa na
// leitura de código de barras (fallback automático para a Open Beauty Facts se
// não houver token configurado ou a consulta falhar). Cadastre-se em
// https://cosmos.bluesoft.com.br para conseguir o token e o User-Agent, e
// configure-os no local.properties (NUNCA no build.gradle, pra não vazar no Git):
//   COSMOS_API_TOKEN=seu_token_aqui
//   COSMOS_USER_AGENT=seu_user_agent_aqui
//
// Lida no nível raiz do script (fora do bloco android{}) de propósito: dentro de
// defaultConfig{} o Kotlin DSL do Gradle não resolve "java.util.Properties"
// corretamente por causa dos receivers implícitos aninhados da AGP.
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { stream -> load(stream) }
}
val cosmosApiToken: String = localProperties.getProperty("COSMOS_API_TOKEN")
    ?: System.getenv("COSMOS_API_TOKEN")
    ?: ""
val cosmosUserAgent: String = localProperties.getProperty("COSMOS_USER_AGENT")
    ?: System.getenv("COSMOS_USER_AGENT")
    ?: ""

android {
    namespace = "com.beautymanager.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.beautymanager.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "COSMOS_API_TOKEN", "\"$cosmosApiToken\"")
        buildConfigField("String", "COSMOS_USER_AGENT", "\"$cosmosUserAgent\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.fragment.ktx)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.work.runtime.ktx)

    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.hilt.work)
    ksp(libs.hilt.compiler)

    implementation(libs.retrofit.core)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.coil.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.biometric)

    // Scanner de código de barras (leitura pela câmera do produto na hora do cadastro/venda)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.mlkit.barcode.scanning)

    // Gráficos do Dashboard/Relatórios são feitos com Canvas nativo do Compose
    // (ver presentation/dashboard/BarChart.kt) — evita depender de uma lib de
    // gráficos de terceiros ainda em beta para algo visualmente simples.

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
}
