package me.anno.io.packer

import me.anno.cache.instances.LastModifiedCache
import me.anno.io.files.FileReference
import me.anno.io.files.FileReference.Companion.createZipFile
import me.anno.io.zip.GetStreamCallback
import me.anno.io.zip.InnerImageFile
import me.anno.io.zip.InnerZipFile
import me.anno.utils.files.Files.formatFileSize
import me.anno.utils.types.Floats.f1
import me.anno.utils.types.Floats.f2
import org.apache.logging.log4j.LogManager
import java.nio.file.attribute.FileTime
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.imageio.ImageIO
import kotlin.math.abs

object Packer {

    // todo remove unnecessary classes, if possible
    // todo e.g. we only need a few image formats,
    // we don't need pdf etc. (except we export an editor), ...

    //////////////////////////////////////////
    //           features by size           //
    //////////////////////////////////////////

    // opengl          1.0  MB
    // openal          0.6  MB/platform

    // tar,7z,zip      0.6  MB
    // rotated jpegs   0.86 MB (definitively can be reduced)
    // xz              0.15 MB
    // rar (+vfs)      0.66 MB
    // psd,tiff,...    0.73 MB (Commons Imaging)

    // pdf             5.59 MB
    // JNA (trash)     2.88 MB

    // box2d           0.35 MB
    // bullet          0.76 MB

    // assimp         ~3.0  MB/platform

    // ogg (stb)       1.2  MB | 0.1 MB + 0.2MB/platform
    // fft             1.5  MB | JTransforms.jar, currently only used in Rem's Studio + could be reduced

    // ffmpeg         63.1  MB
    // ffprobe        63.0  MB

    // (dynamically loaded)
    // spellcheck    169.7  MB
    // spellcheck-en  82.6  MB


    // the best formats (probably)
    // images  png, jpg, hdr
    // audio  mp3
    // video  mp4
    // documents  md/svg/pdf?
    // mesh   Rem's Engine .json


    private val LOGGER = LogManager.getLogger(Packer::class)

    // todo apply this to our engine

    // in a game, there are assets, so
    // todo - we need to pack assets
    // done - it would be nice, if FileReferences could point to local files as well
    // always ship the editor with the game? would make creating mods easier :)
    // yes, low overhead + games should be able to be based on the editor
    // (and cheating, but there always will be cheaters, soo...)

    /**
     * packs resources,
     * and reports the progress automatically to the console
     * */
    fun packWithReporting(
        resources: List<FileReference>,
        ensurePrivacy: Boolean,
        dst: FileReference,
        createMap: Boolean,
        updatingMillis: Int = 500
    ): Map<FileReference, FileReference> {
        val startTime = System.nanoTime()
        var lastTime = startTime
        var lastSize = 0L
        val updatingNanos = updatingMillis * 1000_000L
        val map = pack(resources, ensurePrivacy, dst, createMap) { currentSize, totalSize ->
            if (currentSize < totalSize) {
                val currentTime = System.nanoTime()
                if (lastTime == 0L || abs(currentTime - lastTime) > updatingNanos) {
                    val deltaSize = currentSize - lastSize
                    val deltaTime = currentTime - lastTime
                    val builder = StringBuilder()
                    builder
                        .append("Packing ").append(dst.name).append(", ")
                        .append(currentSize.formatFileSize()).append('/')
                        .append(totalSize.formatFileSize()).append(", ")
                        .append(((currentSize * 100.0) / totalSize).f1()).append("%, ")
                    if (deltaTime > 0L) {
                        builder.append((deltaSize * 1e9 / deltaTime).toLong().formatFileSize())
                            .append("/s")
                    }
                    LOGGER.info(builder.toString())
                    lastSize = currentSize
                    lastTime = currentTime
                }
            }
        }
        LastModifiedCache.invalidate(dst) // just in case dst was cached again in the mean-time
        val size = resources.sumOf { it.length() }
        val compressedSize = dst.length()
        val endTime = System.nanoTime()
        val deltaTime = endTime - startTime
        val readingBandwidth = size * 1e9 / deltaTime
        val writingBandwidth = compressedSize * 1e9 / deltaTime
        LOGGER.warn(
            "Reading Bandwidth: ${readingBandwidth.toLong().formatFileSize()}/s, " +
                    "Writing Bandwidth: ${writingBandwidth.toLong().formatFileSize()}/s"
        )
        val compressionRatio = size.toDouble() / compressedSize
        LOGGER.info("Done packing $dst, ${compressedSize.formatFileSize()}/${size.formatFileSize()}, ${resources.size} files, compression ratio: ${compressionRatio.f2()}:1")
        return map
    }

    /**
     * packs all resources as raw files into a zip file
     * returns the map of new locations, so they can be replaced
     *
     * this is meant for shipping the game: all assets will be packed into a zip file
     * */
    fun pack(
        resources: List<FileReference>,
        ensurePrivacy: Boolean, // pack without names
        dst: FileReference,
        createMap: Boolean,
        reportProgress: (done: Long, total: Long) -> Unit = { _, _ -> }
    ): Map<FileReference, FileReference> {
        val totalSize = resources.sumOf { it.length() }
        var doneSize = 0L
        val zos = ZipOutputStream(dst.outputStream())
        val map = if (createMap) HashMap<FileReference, FileReference>(resources.size) else null
        val getStream = { callback: GetStreamCallback ->
            createZipFile(dst, callback)
        }
        val absolute = dst.absolutePath
        val buffer = ByteArray(1024)
        for ((index, resource) in resources.withIndex()) {
            if (resource.isDirectory) {
                LOGGER.warn("Directory $resource was ignored, because it's a directory!")
                continue
            }
            val name = if (ensurePrivacy) {
                "$index.${resource.extension}"
            } else {
                "${resource.nameWithoutExtension}.$index.${resource.extension}"
            }
            val entry = ZipEntry(name)
            if (!ensurePrivacy) {
                entry.lastModifiedTime = FileTime.fromMillis(resource.lastModified)
                entry.lastAccessTime = FileTime.fromMillis(resource.lastAccessed)
            }
            // write data
            zos.putNextEntry(entry)
            try {
                if (resource is InnerImageFile) {
                    // don't save it as bmp, use png instead
                    // if the original was a jpg, we should use jpg
                    val originalWasJpeg = resource.absolutePath.contains(".jpg/", true) ||
                            resource.absolutePath.contains(".jpeg/", true)
                    val extension = if (originalWasJpeg) "jpg" else "png"
                    val bi = resource.content.createBufferedImage()
                    ImageIO.write(bi, extension, zos)
                    doneSize += resource.compressedSize
                    reportProgress(doneSize, totalSize)
                } else {
                    // will only work in synchronous environments!
                    resource.inputStream { input, _ ->
                        input!!
                        while (true) {
                            val readLength = input.read(buffer)
                            if (readLength < 0) break
                            if (readLength > 0) {
                                zos.write(buffer, 0, readLength)
                                doneSize += readLength
                                try {
                                    reportProgress(doneSize, totalSize)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }
                        input.close()
                    }
                }
            } catch (e: Exception) {
                LOGGER.warn("Issue when copying $resource: ${e.message}")
                e.printStackTrace()
            }
            zos.closeEntry()
            map?.put(resource, InnerZipFile("$absolute/$name", dst, getStream, name, dst))
        }
        zos.close()
        return map ?: emptyMap()
    }

}
