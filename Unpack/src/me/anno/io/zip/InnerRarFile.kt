package me.anno.io.zip

import com.github.junrar.Archive
import com.github.junrar.exception.RarException
import com.github.junrar.rarfile.FileHeader
import me.anno.io.files.FileFileRef
import me.anno.io.files.FileReference
import me.anno.io.files.Signature
import me.anno.io.files.inner.InnerFile
import me.anno.io.files.inner.InnerFolder
import me.anno.io.files.inner.InnerFolderCache
import me.anno.io.files.inner.SignatureFile
import me.anno.io.zip.internal.RarVolume
import me.anno.utils.async.Callback
import me.anno.utils.async.Callback.Companion.map
import java.io.ByteArrayOutputStream
import java.io.IOException

class InnerRarFile(
    absolutePath: String,
    relativePath: String,
    parent: FileReference
) : InnerFile(absolutePath, relativePath, false, parent), SignatureFile {

    override var signature: Signature? = null

    companion object {

        @JvmStatic
        fun fileFromStreamRar(file: FileReference, callback: Callback<Archive>) {
            if (file is FileFileRef) {
                callback.ok(Archive(file.file))
            } else {
                file.readBytes(callback.map { bytes ->
                    // probably for loading a set of files,
                    // which will/would be combined into a single one
                    Archive { archive, volume ->
                        // see FileArchive as an example
                        if (volume == null) {
                            RarVolume(archive, file, bytes)
                        } else TODO("joined rar-s not yet supported")
                    }
                })
            }
        }

        @JvmStatic
        fun createZipRegistryRar(
            zipFileLocation: FileReference,
            callback: Callback<InnerFolder>,
            archive: Archive
        ) {
            val (file, registry) = createMainFolder(zipFileLocation)
            var hasReadEntry = false
            var e: Exception? = null
            try {
                if (archive.isEncrypted) {
                    file.isEncrypted = true
                    callback.err(IOException("RAR is encrypted"))
                    return
                }
                while (true) {
                    val entry = archive.nextFileHeader() ?: break
                    hasReadEntry = true
                    createEntryRar(zipFileLocation.absolutePath, archive, entry, registry)
                }
                archive.close()
            } catch (e2: IOException) {
                e = e2
                // e.printStackTrace()
            } catch (e2: RarException) {
                e = e2
                // e.printStackTrace()
            }
            if (hasReadEntry) callback.ok(file)
            else callback.err(e ?: IOException("Zip was empty"))
        }

        @JvmStatic
        private fun createEntryRar(
            zipFileLocation: String,
            archive: Archive,
            header: FileHeader,
            registry: HashMap<String, InnerFile>
        ): InnerFile {
            val (parent, path) = InnerFolderCache.splitParent(header.fileNameString)
            val file = registry.getOrPut(path) {
                val parent2 = registry.getOrPut(parent) { createFolderEntryRar(zipFileLocation, parent, registry) }
                if (header.isDirectory) {
                    InnerFolder("$zipFileLocation/$path", path, parent2)
                } else {
                    InnerRarFile("$zipFileLocation/$path", path, parent2)
                }
            }
            file.lastModified = header.mTime?.time ?: 0L
            file.lastAccessed = header.aTime?.time ?: 0L
            file.creationTime = header.cTime?.time ?: 0L
            file.compressedSize = header.fullPackSize
            file.size = header.fullUnpackSize
            file.isEncrypted = header.isEncrypted
            if (!file.isDirectory) {
                if (file.isEncrypted) {
                    file.data = "This file is encrypted :/".toByteArray()
                } else {
                    val bos = ByteArrayOutputStream()
                    archive.extractFile(header, bos)
                    bos.close()
                    val data = bos.toByteArray()
                    file.data = data
                    file as SignatureFile
                    file.signature = Signature.find(data)
                }
            }
            return file
        }

        @JvmStatic
        private fun createFolderEntryRar(
            zipFileLocation: String,
            entry: String,
            registry: HashMap<String, InnerFile>
        ): InnerFile {
            val (parent, path) = InnerFolderCache.splitParent(entry)
            val file = registry.getOrPut(path) {
                val parent2 = registry.getOrPut(parent) { createFolderEntryRar(zipFileLocation, parent, registry) }
                InnerFolder("$zipFileLocation/$path", path, parent2)
            }
            file.lastModified = 0L
            file.size = 0
            file.data = null
            return file
        }
    }
}