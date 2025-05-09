package me.anno.io.unity

import me.anno.io.files.FileReference
import me.anno.io.files.inner.InnerFile
import me.anno.io.files.inner.InnerFile.Companion.createRegistry
import me.anno.io.files.inner.InnerFolder
import me.anno.io.files.inner.InnerFolderCache
import me.anno.io.files.inner.InnerFolderCallback
import me.anno.io.yaml.generic.YAMLReader
import me.anno.io.zip.InnerTarFile
import me.anno.utils.types.Strings.indexOf2
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import java.util.zip.GZIPInputStream
import kotlin.math.min

object UnityPackage {

    fun unpack(parent: FileReference, callback: InnerFolderCallback) {
        InnerTarFile.createTarRegistryArchive(parent, { rawArchive, err ->
            if (rawArchive != null) unpack(parent, callback, rawArchive)
            else callback.err(err)
        }) { cb1 ->
            parent.inputStream { stream, err ->
                if (stream != null) cb1.ok(TarArchiveInputStream(GZIPInputStream(stream)))
                else err?.printStackTrace()
            }
        }
    }

    private fun firstLine(text: String): String {
        val i0 = min(text.indexOf2('\r'), text.indexOf2('\n'))
        return text.substring(0, i0)
    }

    private fun unpack(parent: FileReference, callback: InnerFolderCallback, rawArchive: InnerFolder) {
        try {
            val unityArchive = UnityPackageFolder(parent)
            val registry = createRegistry(unityArchive)
            for (value in rawArchive.listChildren()) {
                // the name of the file is the guid (unique unity resource id)
                val pathname0 = value.getChildImpl("pathname")
                if (pathname0.exists && pathname0.length() in 1 until 1024) {
                    val guid = value.name
                    val name = firstLine(pathname0.readTextSync())
                    val metaFile = value.getChildImpl("asset.meta")
                    val meta = if (metaFile is InnerTarFile) {
                        // needed for meta-resolution and detecting whether a folder is a Unity project
                        createArchiveEntry(parent, "$name.meta", metaFile, registry)
                        metaFile.inputStreamSync().bufferedReader().use { reader ->
                            YAMLReader.parseYAML(reader, true)
                        }
                    } else null
                    metaFile.hide()
                    val asset = value.getChildImpl("asset")
                    val assetFile = if (asset is InnerTarFile) {
                        createArchiveEntry(parent, name, asset, registry)
                    } else null
                    if (meta != null && assetFile != null) {
                        unityArchive.project.register(guid, assetFile, meta)
                    }
                }
            }
            callback.ok(
                if (registry.size == 1) {
                    // only return the unity archive, if we found at least one valid entry
                    rawArchive
                } else {
                    // create artificial assets?
                    // it would be really helpful, if we could read non-packaged unity files as well ->
                    // don't do it here, handle .mat files and such as Asset files
                    unityArchive
                }
            )
        } catch (e: Exception) {
            callback.call(rawArchive, e)
        }
    }

    fun createArchiveEntry(
        zipFile: FileReference,
        name: String,
        original: InnerTarFile,
        registry: HashMap<String, InnerFile>
    ): InnerFile {
        val (parent, path) = InnerFolderCache.splitParent(name)
        val getStream = original.getZipStream
        val file = registry.getOrPut(path) {
            val zipFileLocation = zipFile.absolutePath
            val parent2 = registry.getOrPut(parent) {
                InnerTarFile.createFolderEntryTar(zipFileLocation, parent, registry)
            }
            InnerTarFile("$zipFileLocation/$path", zipFile, getStream, path, parent2, original.readingPath)
        }
        file as InnerTarFile
        file.lastModified = original.lastModified
        file.lastAccessed = original.lastAccessed
        file.creationTime = original.creationTime
        file.size = original.size
        file.compressedSize = original.compressedSize
        file.data = original.data
        file.signature = original.signature
        return file
    }
}