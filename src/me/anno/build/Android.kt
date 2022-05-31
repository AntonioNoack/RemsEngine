package me.anno.build

import me.anno.io.files.FileReference.Companion.getReference
import me.anno.utils.OS
import me.anno.utils.OS.desktop
import me.anno.utils.OS.documents
import me.anno.utils.OS.downloads
import me.anno.utils.OS.home
import me.anno.utils.OS.pictures
import me.anno.utils.process.BetterProcessBuilder
import kotlin.math.min

fun main() {

    val sdkPaths = listOf(
        getReference(System.getenv("ANDROID_HOME") ?: ""), // for Linux
        getReference(home.getChild("tools/android_sdk")), // for Linux
        getReference(home.getChild("AppData/Local/Android/Sdk")), // for Windows
        getReference(home.getChild(".androidSDK")) // for me ^^
    )

    fun parseVersion(versionString: String): Double {
        // parse a version number...
        // idea: every "part" is worth 0.001, clamped
        val parts = versionString.split(".")
        var version = 0.0
        var relativeValue = 1.0
        val maxValuePerVersion = 1000.0
        for (part in parts) {
            val subParts = part.split('-')
            val cleaned = subParts[0]
            var value = cleaned.toDoubleOrNull() ?: continue
            // a clean version is worth more than "-alpha", "-beta" or similar;
            // we currently don't try to understand it deeply 😅, could make a list of thing though
            if (subParts.size == 1) value += 0.5
            version += relativeValue * min(value, maxValuePerVersion)
            relativeValue /= (maxValuePerVersion + 1.0)
        }
        return version
    }

    // ^^, should be specifiable manually
    val androidSDKPath = sdkPaths.first { it.exists && it.isDirectory }

    // https://developer.android.com/studio/command-line

    // aapt2 "Android Asset Packaging Tool":
    // compiler from resource into binary, single output
    //  we either precompiler the Android-engine, or build it on the fly...
    // then we have a list of jars, that shall be combined into a single game
    //
    // extension.info will have duplicates... -> allow other names, or is there a solution?
    // we can register them manually, or allow for extension.info.1 and such :)

    // the parent folder name is important

    val listOrResources = listOf(
        documents.getChild("IdeaProjects/AIPuzzle/app/src/main/res/drawable/ic_back.xml") to "res/drawable",
        pictures.getChild("fav128.png") to "res/drawable",
        // downloads.getChild("lib/java/lwjgl/joml-1.9.24.jar") to "app/src/main/java" // does not work, not a "resource"
    )

    // todo [aapt2] create the basic list of resources
    // todo [aapt2] compile a first APK
    // install aapt2 if missing: 'sdkmanager "build-tools;build-tools-version"'
    val aapt2Path = getReference(androidSDKPath, "build-tools")
        .listChildren()!!.maxByOrNull { parseVersion(it.name) }!! // choose the highest available version
        .getChild(if (OS.isLinux) "aapt2" else "aapt2.exe")

    // todo this is only resources... how do we compile code then? jars...
    // todo first compile
    // todo then link
    // todo sample files...

    // aapt2 compile path-to-input-files [options] -o output-directory/
    val output = desktop.getChild("android")
    output.tryMkdirs()

    val tmp = desktop.getChild("android-tmp")

    for ((resource, type) in listOrResources) {
        // map the resources to their temporary type
        // because the compiler wants it like that... a little stupid...
        val tmpResource = if (
            resource.absolutePath
                .endsWith("$type/${resource.name}")
        ) {
            resource
        } else {
            val tmpResource = tmp.getChild(type).getChild(resource.name)
            if (!tmpResource.exists) {
                tmpResource.getParent()!!.mkdirs()
                tmpResource.writeFile(resource)
            }
            tmpResource
        }
        BetterProcessBuilder(aapt2Path, 5, false)
            .add(
                // paths must use backslash instead of slashes, and there is no warning for doing it wrong :/
                // will this be the same on Linux?? to do test this
                "compile",
                tmpResource.absolutePath.replace('/', '\\'),
                "-v",
                "-o", output.absolutePath.replace('/', '\\')
            )
            .startAndPrint()
    }

    println(aapt2Path)

    // apk signer: for shipping

    // avd manager: manage virtual devices

    // sdk manager: package manager for sdk

    // apk analyzer: insight after build process

    // ADB "Android Debug Bridge": installs APKs onto system

}