package me.anno.export

import me.anno.engine.projects.GameEngineProject
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.json.saveable.JsonStringWriter
import me.anno.io.utils.StringMap
import me.anno.utils.OS.documents
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

// todo how can we create an executable from a jar like Launch4j?
object ExportProcess {
    fun execute(project: GameEngineProject, settings: ExportSettings) {
        val sources = HashMap<String, ByteArray>(65536)
        indexJar(sources, documents.getChild("IdeaProjects/RemsEngine/out/artifacts/universal/RemsEngine.jar"))
        settings.assetsToInclude
        settings.excludedClasses
        settings.modulesToInclude
        // todo build .jar file from export settings and current project
        //  - collect all required files
        //  - exclude those that were explicitly excluded
        // override icon if needed
        if (settings.iconOverride.exists) {
            sources["icon.png"] = settings.iconOverride.readBytesSync()
        }
        sources["export.json"] = createConfigJson(project, settings)
        sources["META-INF/MANIFEST.MD"] = createManifest()
        // build a zip from it
        writeJar(sources, settings.dstFile)
    }

    private fun createConfigJson(project: GameEngineProject, settings: ExportSettings): ByteArray {
        val config = StringMap()
        config["firstScenePath"] = settings.firstScenePath
        config["gameTitle"] = settings.gameTitle
        config["configName"] = settings.configName
        config["versionNumber"] = settings.versionNumber
        return JsonStringWriter.toText(config, InvalidRef).encodeToByteArray()
    }

    private fun createManifest(): ByteArray {
        val str = "Manifest-Version: 1.0\n" +
                "Main-Class: me.anno.export.ExportedGame\n"
        return str.encodeToByteArray()
    }

    private fun indexJar(sources: MutableMap<String, ByteArray>, src: FileReference) {
        val input = ZipInputStream(src.inputStreamSync())
        while (true) {
            val entry = input.nextEntry ?: break
            sources[entry.name] = input.readBytes()
        }
        input.close()
    }

    private fun writeJar(sources: Map<String, ByteArray>, dst: FileReference) {
        val zos = ZipOutputStream(dst.outputStream())
        for ((name, bytes) in sources) {
            val entry = ZipEntry(name)
            zos.putNextEntry(entry)
            zos.write(bytes)
            zos.closeEntry()
        }
        zos.close()
    }
}