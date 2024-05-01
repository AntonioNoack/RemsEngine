package me.anno.export

import me.anno.engine.projects.GameEngineProject
import me.anno.export.Exclusion.excludeJNAFiles
import me.anno.export.Exclusion.excludeLWJGLFiles
import me.anno.export.Exclusion.excludeNonMinimalUI
import me.anno.export.Exclusion.excludeWebpFiles
import me.anno.export.Indexing.indexProject
import me.anno.export.reflect.Reflections.replaceReflections
import me.anno.export.idea.IdeaProject
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.json.saveable.JsonStringWriter
import me.anno.io.utils.StringMap
import me.anno.ui.base.progress.ProgressBar
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

// todo how can we create an executable from a jar like Launch4j?
object ExportProcess {

    fun execute(project: GameEngineProject, settings: ExportSettings, progress: ProgressBar) {
        val sources = HashMap<String, ByteArray>(65536)
        val projects = settings.projectRoots.map(IdeaProject::loadProject)

        // todo inclusion order???
        for (projectI in projects) {
            indexProject(projectI, sources, settings, progress)
        }

        excludeLWJGLFiles(sources, settings)
        excludeJNAFiles(sources, settings)
        excludeWebpFiles(sources, settings)
        if (settings.minimalUI) {
            excludeNonMinimalUI(sources, settings.useKotlynReflect)
        }
        if (settings.useKotlynReflect) {
            replaceReflections(sources)
        }

        // todo build .jar file from export settings and current project
        //  - collect all required files
        //  - exclude those that were explicitly excluded
        //  - respect platform (Linux/Windows/MacOS) settings in config file
        //  - option for Android build

        // override icon if needed
        if (settings.iconOverride.exists) {
            sources["icon.png"] = settings.iconOverride.readBytesSync()
        }
        sources["export.json"] = createConfigJson(settings)
        sources["META-INF/MANIFEST.MF"] = createManifest()
        progress.total = sources.size + 2.0 // 2.0 just for safety
        if (progress.isCancelled) return
        // build a zip from it
        settings.dstFile.getParent().tryMkdirs()
        writeJar(sources, settings.dstFile, progress)
    }

    private fun createConfigJson(settings: ExportSettings): ByteArray {
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

    private fun writeJar(sources: Map<String, ByteArray>, dst: FileReference, progress: ProgressBar) {
        val zos = ZipOutputStream(dst.outputStream())
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