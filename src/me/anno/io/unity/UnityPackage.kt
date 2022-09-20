package me.anno.io.unity

import me.anno.io.files.FileReference
import me.anno.io.zip.*
import me.anno.io.zip.InnerFile.Companion.createRegistry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import java.util.zip.GZIPInputStream

object UnityPackage {

    fun unpack(parent: FileReference, callback: InnerFolderCallback) {

        val getStream = { TarArchiveInputStream(GZIPInputStream(parent.inputStreamSync())) }
        val rawArchive = InnerTarFile.createZipRegistryArchive(parent, getStream)
        try {
            val unityArchive = UnityPackageFolder(parent)
            val registry = createRegistry(unityArchive)
            for (value in rawArchive.listChildren()) {
                // the name of the file is the guid (unique unity resource id)
                val pathname0 = value.getChild("pathname")
                if (pathname0.exists && pathname0.length() in 1 until 1024) {
                    val guid = value.name
                    val name = pathname0.readTextSync()
                    val meta = value.getChild("asset.meta")
                    val metaFile = if (meta is InnerTarFile) {
                        createEntryArchive(parent, "$name.meta", meta, registry)
                    } else null
                    val asset = value.getChild("asset")
                    val assetFile = if (asset is InnerTarFile) {
                        createEntryArchive(parent, name, asset, registry)
                    } else null
                    if (metaFile != null && assetFile != null) {
                        unityArchive.project.register(guid, assetFile)
                    }
                }
            }
            callback(
                if (registry.size == 1) {
                    // only return the unity archive, if we found at least one valid entry
                    rawArchive
                } else {
                    // create artificial assets?
                    // it would be really helpful, if we could read non-packaged unity files as well ->
                    // don't do it here, handle .mat files and such as Asset files
                    unityArchive
                }, null
            )
        } catch (e: Exception) {
            callback(rawArchive, e)
        }
    }

    fun createEntryArchive(
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
        file.size = original.size
        file.compressedSize = original.compressedSize
        file.data = original.data
        file.signature = original.signature
        return file
    }

}