package me.anno.export

import me.anno.engine.projects.GameEngineProject
import me.anno.export.Exclusion.excludeJNAFiles
import me.anno.export.Exclusion.excludeLWJGLFiles
import me.anno.export.Exclusion.excludeNonMinimalUI
import me.anno.export.Exclusion.excludeWebpFiles
import me.anno.export.Indexing.indexProject
import me.anno.export.idea.IdeaProject
import me.anno.export.reflect.Reflections.replaceReflections
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.json.saveable.JsonStringWriter
import me.anno.io.utils.StringMap
import me.anno.ui.base.progress.ProgressBar
import me.anno.utils.OS.res
import org.apache.logging.log4j.LogManager
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

// todo bug: when exporting DamagedHelmet as scene,
//  1.jpg is missing somehow...
object ExportProcess {

    private val LOGGER = LogManager.getLogger(ExportProcess::class)

    fun execute(project: GameEngineProject, settings: ExportSettings, progress: ProgressBar) {
        val sources = HashMap<String, ByteArray>(65536)
        val projects = settings.projectRoots.map(IdeaProject::loadProject)

        // todo inclusion order???
        for (projectI in projects) {
            indexProject(projectI, sources, settings, progress)
        }

        // todo option to add orbit-camera to export?

        excludeLWJGLFiles(sources, settings)
        excludeJNAFiles(sources, settings)
        excludeWebpFiles(sources, settings)
        if (settings.minimalUI) {
            excludeNonMinimalUI(sources, settings.useKotlynReflect)
        }
        if (settings.useKotlynReflect) {
            replaceReflections(sources)
        }

        // build .jar file from export settings and current project
        // - collect all required files
        //  - todo exclude those that were explicitly excluded
        //  - respect platform (Linux/Windows/MacOS) settings in config file
        // todo - option for Android build

        val assetMap = Packer.pack(
            (settings.includedAssets + settings.firstSceneRef + settings.iconOverride)
                .filter { it.exists },
            sources
        ) { packProgress, packTotal ->
            progress.unit = "Bytes"
            progress.total = (packTotal + 1).toDouble() // +1 to prevent unintentional finishing
            progress.progress = packProgress.toDouble()
        }

        // override icon if needed
        if (settings.iconOverride.exists) {
            sources["icon.png"] = sources[assetMap[settings.iconOverride]]
                ?: settings.iconOverride.readBytesSync()
        }
        val firstSceneRefMapped = assetMap[settings.firstSceneRef]
        val firstSceneRef =
            if (firstSceneRefMapped != null) res.getChild(firstSceneRefMapped)
            else settings.firstSceneRef
        sources["export.json"] = createConfigJson(settings, firstSceneRef)
        sources["META-INF/MANIFEST.MF"] = createManifest()
        progress.unit = "Files"
        progress.progress = 0.0
        progress.total = sources.size + 2.0 // 2.0 just for safety
        if (progress.isCancelled) return
        // build a zip from it
        settings.dstFile.getParent().tryMkdirs()
        writeJar(settings, sources, settings.dstFile, progress)
    }

    private fun createConfigJson(settings: ExportSettings, firstSceneRef: FileReference): ByteArray {
        val config = StringMap()
        config["firstScenePath"] = firstSceneRef
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

    private fun writeJar(
        settings: ExportSettings,
        sources: Map<String, ByteArray>,
        dst: FileReference,
        progress: ProgressBar
    ) {
        if (dst.lcExtension == "exe") {
            // how can we create an executable from a jar like Launch4j?
            //  -> create a minimal executable, and let it read from the end of itself
            //  - test if Java is available
            //       if yes, start
            //       if no, redirect user to search how to install Java in their browser / show a msg
            //  - we need an icon somehow...
            val loc = settings.windowsPlatforms.exeBaseLocation
            if (loc.exists) {
                loc.readBytes { bytes, err ->
                    if (bytes != null) {
                        dst.outputStream().use { output ->
                            output.write(bytes)
                            writeJarContents(sources, output, progress)
                        }
                    } else progress.cancel(true)
                    err?.printStackTrace()
                }
                return
            } else LOGGER.warn("Missing exe-base-location")
        }
        dst.outputStream().use { output ->
            writeJarContents(sources, output, progress)
        }
    }

    private fun writeJarContents(sources: Map<String, ByteArray>, output: OutputStream, progress: ProgressBar) {
        val zos = ZipOutputStream(output)
        for ((name, bytes) in sources) {
            writeZipEntry(zos, name, bytes)
            if (progress.isCancelled) break
            progress.progress++
        }
        zos.close()
        progress.finish()
    }

    private fun writeZipEntry(zos: ZipOutputStream, name: String, bytes: ByteArray) {
        val entry = ZipEntry(name)
        zos.putNextEntry(entry)
        zos.write(bytes)
        zos.closeEntry()
    }
}