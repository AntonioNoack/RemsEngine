package me.anno.build

import me.anno.io.files.Reference.getReference
import me.anno.utils.OS
import me.anno.utils.OS.documents
import me.anno.utils.OS.home
import me.anno.utils.process.BetterProcessBuilder.Companion.readLines
import java.io.File
import java.io.FileNotFoundException
import kotlin.concurrent.thread

/**
 * automated build/export process for Android, so we don't need (to open) AndroidStudio
 * */
fun main() {

    // this shouldn't be hardcoded...
    val enginePath = documents.getChild("IdeaProjects/RemsEngine")
    if (!enginePath.exists) throw FileNotFoundException("Missing $enginePath")

    // this is linked by using a relative path... not the best solution...
    val builtJar = enginePath.getChild("out/artifacts/Android/RemsEngine.jar")
    if (!builtJar.exists) throw FileNotFoundException("Missing $builtJar")

    val sdkPaths = listOf(
        getReference(System.getenv("ANDROID_HOME") ?: ""), // for Linux
        getReference(home.getChild("tools/android_sdk")), // for Linux
        getReference(home.getChild("AppData/Local/Android/Sdk")), // for Windows
    )

    val androidSDKPath = sdkPaths.first { it.exists && it.isDirectory }
    val adb = androidSDKPath.getChild(if (OS.isWindows) "platform-tools/adb.exe" else "platform-tools/adb")
    if (!adb.exists) throw FileNotFoundException("Missing $adb")

    val androidPortPath = documents.getChild("IdeaProjects/RemsEngineAndroid")
    androidPortPath.tryMkdirs()

    fun execute(args: List<String>) {
        val builder = ProcessBuilder(args)
        builder.directory(File(androidPortPath.absolutePath))
        val process = builder.start()
        thread(name = "cmd($args):error") { readLines(process.errorStream, true) }
        thread(name = "cmd($args):input") { readLines(process.inputStream, false) }
        process.waitFor()
    }

    if (androidPortPath.getChild(".git").exists) {
        execute(listOf("git", "pull"))
    } else {
        execute(listOf("git", "clone", "--recursive", "https://github.com/AntonioNoack/RemsEngine-Android", "."))
    }

    val gradlew = androidPortPath.getChild(if (OS.isWindows) "gradlew.bat" else "gradlew")
    if (!gradlew.exists) throw FileNotFoundException("Missing $gradlew")

    execute(listOf(gradlew.absolutePath, "build"))
    execute(listOf(adb.absolutePath, "install", "-r", "app/build/outputs/apk/debug/app-debug.apk"))
    val path = "me.anno.remsengine/me.anno.remsengine.android.MainActivity"
    execute(listOf(adb.absolutePath, "shell", "am", "start", "-n", path))
}