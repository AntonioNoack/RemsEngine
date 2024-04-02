package me.anno.export

import me.anno.engine.projects.GameEngineProject
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.json.saveable.JsonStringWriter
import me.anno.io.utils.StringMap
import me.anno.ui.base.progress.ProgressBar
import me.anno.utils.OS.documents
import me.anno.utils.structures.maps.Maps.removeIf
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

// todo how can we create an executable from a jar like Launch4j?
object ExportProcess {
    fun execute(project: GameEngineProject, settings: ExportSettings, progress: ProgressBar) {
        val sources = HashMap<String, ByteArray>(65536)
        val engineBuild = documents.getChild("IdeaProjects/RemsEngine/out/artifacts/universal/RemsEngine.jar")
        indexJar(sources, engineBuild, progress)
        excludeLWJGLFiles(sources, settings)
        excludeJNAFiles(sources, settings)
        settings.assetsToInclude
        settings.excludedClasses
        settings.modulesToInclude

        // todo build .jar file from export settings and current project
        //  - collect all required files
        //  - exclude those that were explicitly excluded
        //  - respect platform (Linux/Windows/MacOS) settings in config file
        //  - option for Android build

        // todo modules have dependencies,
        //  so find them (list of source projects?),
        //  read their dependency list,
        //  and apply it ourselves (-> we need to find the build folders, too)

        // override icon if needed
        if (settings.iconOverride.exists) {
            sources["icon.png"] = settings.iconOverride.readBytesSync()
        }
        sources["export.json"] = createConfigJson(project, settings)
        sources["META-INF/MANIFEST.MF"] = createManifest()
        progress.total = sources.size + 2.0 // 2.0 just for safety
        if (progress.isCancelled) return
        // build a zip from it
        settings.dstFile.getParent().tryMkdirs()
        writeJar(sources, settings.dstFile, progress)
    }

    private fun excludeLWJGLFiles(sources: HashMap<String, ByteArray>, settings: ExportSettings) {
        excludeFiles(sources, settings.linuxPlatforms.arm64, "linux/arm64/")
        excludeFiles(sources, settings.linuxPlatforms.arm32, "linux/arm32/")
        excludeFiles(sources, settings.linuxPlatforms.x64, "linux/x64/")
        excludeFiles(sources, settings.windowsPlatforms.arm64, "windows/arm64/")
        excludeFiles(sources, settings.windowsPlatforms.x86, "windows/x86/")
        excludeFiles(sources, settings.windowsPlatforms.x64, "windows/x64/")
        excludeFiles(sources, settings.macosPlatforms.arm64, "macos/arm64/")
        excludeFiles(sources, settings.macosPlatforms.x64, "macos/x64/")
    }

    private fun excludeJNAFiles(sources: HashMap<String, ByteArray>, settings: ExportSettings) {
        val anyMacos = settings.macosPlatforms.arm64 || settings.macosPlatforms.x64
        val anyLinux = settings.linuxPlatforms.arm32 || settings.linuxPlatforms.arm64 || settings.linuxPlatforms.x64
        val anyWindows = settings.windowsPlatforms.x86 || settings.windowsPlatforms.x64 ||
                settings.windowsPlatforms.arm64
        sources.removeIf { (key, _) ->
            if (key.startsWith("com/sun/jna/")) {
                key.startsWith("com/sun/jna/aix-") ||
                        key.startsWith("com/sun/jna/freebsd") ||
                        key.startsWith("com/sun/jna/linux-mips") ||
                        key.startsWith("com/sun/jna/linux-ppc") ||
                        key.startsWith("com/sun/jna/linux-s390x") ||
                        key.startsWith("com/sun/jna/darwin") ||
                        key.startsWith("com/sun/jna/sunos-") ||
                        key.startsWith("com/sun/jna/openbsd-") ||
                        key.startsWith("com/sun/jna/linux-armel/") ||
                        key.startsWith("com/sun/jna/linux-x86/") ||
                        key.startsWith("com/sun/jna/platform/wince") ||
                        key.startsWith("com/sun/jna/platform/unix/aix/") ||
                        key.startsWith("com/sun/jna/platform/unix/solaris/") ||
                        (!anyMacos && key.startsWith("com/sun/jna/platform/mac/")) ||
                        (!anyLinux && key.startsWith("com/sun/jna/platform/unix/")) || // correct??
                        (!anyLinux && key.startsWith("com/sun/jna/platform/linux/")) ||
                        (!anyWindows && key.startsWith("com/sun/jna/platform/win32/")) ||
                        (!settings.windowsPlatforms.arm64 && key.startsWith("com/sun/jna/win32-aarch64")) ||
                        (!settings.windowsPlatforms.x86 && key.startsWith("com/sun/jna/win32-x86/")) ||
                        (!settings.windowsPlatforms.x64 && key.startsWith("com/sun/jna/win32-x86-64/")) ||
                        (!settings.linuxPlatforms.arm64 && key.startsWith("com/sun/jna/linux-aarch64/")) ||
                        (!settings.linuxPlatforms.arm32 && key.startsWith("com/sun/jna/linux-arm/")) ||
                        (!settings.linuxPlatforms.x64 && key.startsWith("com/sun/jna/linux-x86-64/"))
            } else false
        }
    }

    private fun excludeFiles(sources: HashMap<String, ByteArray>, flag: Boolean, path: String) {
        if (!flag) {
            sources.removeIf {
                it.key.startsWith(path)
            }
        }
    }

    private fun createConfigJson(project: GameEngineProject, settings: ExportSettings): ByteArray {
        val config = StringMap()
        config["firstScenePath"] = settings.firstSceneRef
        config["gameTitle"] = settings.gameTitle
        config["configName"] = settings.configName
        config["versionNumber"] = settings.versionNumber
        return JsonStringWriter.toText(config, InvalidRef).encodeToByteArray()
    }

    private fun createManifest(): ByteArray {
        val str = "Manifest-Version: 1.0\n" +
                "Main-Class: me.anno.engine.ExportedGame\n"
        return str.encodeToByteArray()
    }

    private fun indexJar(sources: MutableMap<String, ByteArray>, src: FileReference, progress: ProgressBar) {
        val input = ZipInputStream(src.inputStreamSync())
        while (!progress.isCancelled) {
            val entry = input.nextEntry ?: break
            sources[entry.name] = input.readBytes()
            progress.total++
        }
        input.close()
    }

    private fun writeJar(sources: Map<String, ByteArray>, dst: FileReference, progress: ProgressBar) {
        val zos = ZipOutputStream(dst.outputStream())
        for ((name, bytes) in sources) {
            val entry = ZipEntry(name)
            zos.putNextEntry(entry)
            zos.write(bytes)
            zos.closeEntry()
            if (progress.isCancelled) break
            progress.progress++
        }
        zos.close()
        val done = !progress.isCancelled
        progress.finish(done)
    }
}