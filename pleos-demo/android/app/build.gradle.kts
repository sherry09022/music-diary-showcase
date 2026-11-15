plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val webAssets = layout.buildDirectory.dir("generated/web-assets")
val syncWebAssets by tasks.registering(Sync::class) {
    from(rootProject.projectDir.parentFile) {
        include("index.html", "app.js", "core.mjs", "styles.css", "assets/**")
        exclude("**/*.mp4", "**/*.webm")
    }
    into(webAssets)
    doLast {
        check(webAssets.get().file("index.html").asFile.exists()) { "pleos-demo/index.html is missing" }
    }
}

android {
    namespace = "ai.projectshu.musicdiary.pleosdemo"
    compileSdk = 35
    useLibrary("android.car", false)
    defaultConfig {
        applicationId = "ai.projectshu.musicdiary.pleosdemo"
        minSdk = 28
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0-demo"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    sourceSets["main"].assets.srcDir(webAssets)
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    lint { abortOnError = true }
    androidResources { noCompress += listOf("mp3", "m4a", "ogg", "wav") }
}

tasks.named("preBuild") { dependsOn(syncWebAssets) }

dependencies {
    implementation("androidx.webkit:webkit:1.11.0")
    implementation("ai.pleos.playground:TextToSpeech:3.3.1")
    implementation("ai.pleos.playground:SpeechToText:3.3.1")
    implementation("ai.pleos.playground:Vehicle:2.3.2")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test:core:1.6.1")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.uiautomator:uiautomator:2.3.0")
}
