import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvm("desktop")
    
    sourceSets {
        val desktopMain by getting
        
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.ui)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtime.compose)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.gson)
            implementation(libs.datastore)
            implementation(libs.datastore.preferences)
        }
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
            implementation(libs.compose.desktop.preview)
            implementation(libs.java.jna)
            implementation(libs.java.jna.affinity)
            implementation(libs.zip4j)
            implementation(libs.vinceglb.compose.filekit)
            implementation(libs.native.params)
        }
    }
}


compose.desktop {
    application {
        mainClass = "com.thomas.zipcracker.MainKt"

        nativeDistributions {
            buildTypes.release.proguard {
                isEnabled.set(false)
            }

            targetFormats(TargetFormat.Msi, TargetFormat.Deb)
            packageName = "ZipCracker"
            packageVersion = "1.1.0"
            description = "ZipCracker"
            copyright = "© 2024 Thomas. All rights reserved"
            vendor = "Thomas"
            licenseFile.set(file("LICENSE"))
            includeAllModules = true

            appResourcesRootDir.set(project.layout.projectDirectory.dir("res"))

            windows {
                iconFile.set(file("ZipCracker.ico"))
                dirChooser = true
                menuGroup = "ZipCracker"
                perUserInstall = true
                installationPath = "D:\\"
                includeAllModules = true
            }

            linux {
                modules("jdk.security.auth")
                debMaintainer = "minhhaihoang2312@gmail.com"
                menuGroup = "ZipCracker"
            }
        }
    }
}
