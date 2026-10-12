plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.nama30.tv"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.nama30.tv"
        minSdk = 23
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.1"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Sideload build: signed with the local debug key so it installs without extra setup.
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    sourceSets["main"].assets.srcDir(layout.buildDirectory.dir("generated/webassets"))
}

// Bundle the Tizen web app (../../tizen-app) as assets/www, swapping Tizen-only scripts for the Android shim.
val syncWeb by tasks.registering(Sync::class) {
    from("../../tizen-app") {
        exclude("tools/**", "config.xml", "README.md", "icon-512.png", "js/lib/hls.min.js", "js/tmdb-token.example.js")
        filesMatching("index.html") {
            filter { line ->
                when {
                    line.contains("name=\"viewport\"") -> "<meta name=\"viewport\" content=\"width=1920, user-scalable=no\">"
                    line.contains("\$WEBAPIS") -> "<script src=\"android/shim.js\"></script>"
                    line.contains("hls.min.js") -> ""
                    else -> line
                }
            }
        }
        into("www")
    }
    from("src/main/web") { into("www") }
    into(layout.buildDirectory.dir("generated/webassets"))
}
tasks.named("preBuild") { dependsOn(syncWeb) }

dependencies {
    val media3 = "1.4.1"
    implementation("androidx.media3:media3-exoplayer:$media3")
    implementation("androidx.media3:media3-exoplayer-hls:$media3")
    implementation("androidx.media3:media3-datasource-okhttp:$media3")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("androidx.webkit:webkit:1.12.1")
}
