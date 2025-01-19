import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.0.21"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(fileTree("../lib") { include("commons-vfs2-2.8.0.jar") })
    implementation(fileTree("../lib/compress") { include("*.jar") })
    implementation(fileTree("../lib/unrar") { include("jUnRAR.jar") })
    implementation(project(":"))
    implementation(project(":KOML"))
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_1_8)
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

sourceSets {
    main {
        kotlin {
            setSrcDirs(listOf("src"))
        }
    }
}
