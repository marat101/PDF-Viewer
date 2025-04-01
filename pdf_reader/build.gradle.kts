import com.vanniktech.maven.publish.SonatypeHost

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    id("maven-publish")
    id("signing")
    id("com.vanniktech.maven.publish") version "0.31.0-rc2"
}

android {
    namespace = "ru.marat.pdfviewer"
    compileSdk = 35

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }

    defaultConfig {
        minSdk = 24
    }
    lint {
        targetSdk = 35
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

    implementation(libs.kotlinx.serialization.json)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    debugImplementation(libs.androidx.ui.tooling)
}

mavenPublishing {
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)

    signAllPublications()
    coordinates("io.github.marat101", "pdf-viewer", "1.0.0-alpha")
    pom {
        name = "PdfViewer"
        description = "Android PdfViewer library"
        url = "https://github.com/marat101/PDF-Viewer"
        licenses {
            license {
                name = "The Apache License, Version 2.0"
                url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                distribution = "https://www.apache.org/licenses/LICENSE-2.0.txt"
            }
        }
        developers {
            developer {
                id.set("marat101")
                name.set("Marat")
                email.set("maratnv101@gmail.com")
            }
        }
        scm {
            url.set("https://github.com/marat101/PDF-Viewer")
            connection.set("scm:git:https://github.com/marat101/PDF-Viewer.git")
            developerConnection = "scm:git:ssh://git@github.com/marat101/PDF-Viewer.git"
        }
    }
}