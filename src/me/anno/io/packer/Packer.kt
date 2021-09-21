package me.anno.io.packer

import me.anno.io.files.FileReference
import me.anno.io.files.FileReference.Companion.createZipFile
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

    private val LOGGER = LogManager.getLogger(Packer::class)

    // todo apply this to our engine

    // in a game, there are assets, so
    // todo - we need to pack assets
    // done - it would be nice, if FileReferences could point to local files as well
    // todo always ship the editor with the game? would make creating mods easier :)
    // (and cheating, but there always will be cheaters, soo...)

    /**
     * packs resources,
     * and reports the progress automatically to the console
     * */
    fun packWithReporting(
        resources: List<FileReference>,
        ensurePrivacy: Boolean, dst: FileReference, createMap: Boolean,
        updatingMillis: Int = 500
    ): Map<FileReference, FileReference> {
        val startTime = System.nanoTime()
        var lastTime = startTime
        var lastSize = 0L
        val updatingNanos = updatingMillis * 1000_000L
        val map = pack(resources, ensurePrivacy, dst, createMap) { size, total ->
            if (size < total) {
                val time = System.nanoTime()
                if (lastTime == 0L || abs(time - lastTime) > updatingNanos) {
                    val deltaSize = size - lastSize
                    val deltaTime = time - lastTime
                    val bandwidth =
                        if (deltaTime == 0L) "NaN" else (deltaSize * 1e9 / deltaTime).toLong().formatFileSize()
                    val percent = (size * 100.0) / total
                    LOGGER.info(
                        "Packing ${dst.name}, " +
                                "${size.formatFileSize()}/${total.formatFileSize()}, " +
                                "${percent.f1()}%, " +
                                "$bandwidth/s"
                    )
                    lastSize = size
                    lastTime = time
                }
            }
        }
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
        val getStream = { createZipFile(dst) }
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
                    val input = resource.inputStream()
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
            } catch (e: Exception) {
                LOGGER.warn("Issue when copying $resource: ${e.message}")
                e.printStackTrace()
            }
            zos.closeEntry()
            if (createMap) map!![resource] = InnerZipFile("$absolute/$name", getStream, name, dst)
        }
        zos.close()
        return map ?: emptyMap()
    }

}