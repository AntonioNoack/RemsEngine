import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.0.21"
}

repositories {
    mavenCentral()
}

dependencies {
    // todo replace all of them with proper links to repositories
    implementation(fileTree("../lib") {
        include("*.jar")
    })
    implementation(fileTree("../lib/compress") { include("*.jar") })
    implementation(fileTree("../lib/customized/recast4j") { include("Recast4j.jar") })
    implementation(fileTree("../lib/customized/box2d") { include("JBox2d.jar") })
    implementation(project(":"))
    implementation(project(":KOML"))
    implementation(project(":Box2d"))
    implementation(project(":Bullet"))
    implementation(project(":Export"))
    implementation(project(":Image"))
    implementation(project(":JVM"))
    implementation(project(":Lua"))
    implementation(project(":Mesh"))
    implementation(project(":Network"))
    implementation(project(":OpenXR"))
    implementation(project(":PDF"))
    implementation(project(":Recast"))
    implementation(project(":SDF"))
    implementation(project(":Unpack"))
    implementation(project(":Video"))
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.11.4")
    // todo parameterized tests might be nice
    // testImplementation("org.junit.jupiter:junit-jupiter-params:5.11.4")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.11.4")
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
    test {
        kotlin {
            setSrcDirs(listOf("src"))
        }
        resources {
            setSrcDirs(listOf("assets"))
        }
    }
}
