import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.0.21"
}

repositories {
    mavenCentral()
}

dependencies {
    // todo replace all of them with proper links to repositories
    implementation(fileTree("lib") {
        include("*.jar")
    })
    implementation(project(":KOML"))
    implementation("org.jetbrains.kotlin:kotlin-reflect")
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
        resources {
            setSrcDirs(listOf("assets"))
        }
    }
}

tasks.jar {
    archiveBaseName.set("RemsEngine")
}

/*tasks.register<Jar>("Universal") {
    archiveBaseName.set("Universal")
    from(sourceSets.main.get().output)
    dependsOn(":JVM:build")
    classpath = files(
        configurations.runtimeClasspath.get(),
        project(":JVM").tasks.getByName("jar").outputs.files
    )
}

tasks.register<Jar>("Windows"){
    archiveBaseName.set("Windows")
    from(sourceSets.main.get().output)
}*/