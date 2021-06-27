package me.anno.ui.editor.files.thumbs

import me.anno.cache.data.ImageData
import me.anno.cache.instances.LastModifiedCache
import me.anno.cache.instances.MeshCache
import me.anno.cache.instances.TextureCache.getLateinitTexture
import me.anno.cache.instances.VideoCache.getVideoFrame
import me.anno.gpu.GFX
import me.anno.gpu.RenderState.renderPurely
import me.anno.gpu.RenderState.useFrame
import me.anno.gpu.SVGxGFX
import me.anno.gpu.TextureLib
import me.anno.gpu.TextureLib.whiteTexture
import me.anno.gpu.drawing.DrawTextures.drawTexture
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.framebuffer.Frame
import me.anno.gpu.shader.Renderer
import me.anno.gpu.texture.*
import me.anno.image.HDRImage
import me.anno.io.FileReference
import me.anno.io.config.ConfigBasics
import me.anno.objects.Video
import me.anno.objects.documents.pdf.PDFCache
import me.anno.utils.Color.a
import me.anno.utils.Color.b
import me.anno.utils.Color.g
import me.anno.utils.Color.r
import me.anno.utils.Color.rgba
import me.anno.utils.LOGGER
import me.anno.utils.Sleep.waitUntilDefined
import me.anno.utils.Threads.threadWithName
import me.anno.utils.input.readNBytes2
import me.anno.utils.types.Strings.getImportType
import me.anno.video.FFMPEGMetadata.Companion.getMeta
import net.boeckling.crc.CRC64
import org.apache.commons.imaging.Imaging
import org.joml.Matrix4fArrayList
import org.joml.Vector4f
import org.lwjgl.opengl.GL11.*
import java.awt.image.BufferedImage
import java.io.File
import java.util.*
import javax.imageio.ImageIO
import kotlin.concurrent.thread
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * creates and caches small versions of image and video resources
 * */
object Thumbs {

    private val folder = File(ConfigBasics.cacheFolder.file, "thumbs")
    private val sizes = intArrayOf(32, 64, 128, 256, 512)
    private val neededSizes = IntArray(sizes.last() + 1)
    private const val timeout = 5000L

    private fun FileReference.getCacheFile(size: Int): File {
        val hashReadLimit = 256
        val info = LastModifiedCache[this]
        var hash = info.lastModified xor (454781903L * this.length())
        if (!info.isDirectory) {
            val reader = inputStream().buffered()
            val bytes = reader.readNBytes2(hashReadLimit, false)
            reader.close()
            hash = hash xor CRC64.fromInputStream(bytes.inputStream()).value
        }
        var hashString = hash.toULong().toString(16)
        while (hashString.length < 16) hashString = "0$hashString"
        return File(folder, "$size/${hashString.substring(0, 2)}/${hashString.substring(2)}.$destinationFormat")
    }

    init {
        var index = 0
        for (size in sizes) {
            while (index <= size) {
                neededSizes[index++] = size
            }
        }
    }

    private fun getSize(neededSize: Int): Int {
        return if (neededSize < neededSizes.size) {
            neededSizes[neededSize]
        } else sizes.last()
    }

    fun getThumbnail(file: FileReference, neededSize: Int): ITexture2D? {
        val size = getSize(neededSize)
        val key = ThumbnailKey(file, size)
        return getLateinitTexture(key, timeout) { callback ->
            thread { generate(file, size, callback) }
        }.texture
    }

    private fun upload(srcFile: FileReference, dst: BufferedImage, callback: (Texture2D) -> Unit) {
        val rotation = ImageData.getRotation(srcFile)
        GFX.addGPUTask(dst.width, dst.height) {
            val texture = Texture2D(dst)
            texture.rotation = rotation
            callback(texture)
        }
    }

    private fun saveNUpload(srcFile: FileReference, dstFile: File, dst: BufferedImage, callback: (Texture2D) -> Unit) {
        dstFile.parentFile.mkdirs()
        ImageIO.write(dst, destinationFormat, dstFile)
        upload(srcFile, dst, callback)
    }

    private fun transformNSaveNUpload(
        srcFile: FileReference,
        src: BufferedImage,
        dstFile: File,
        size: Int,
        callback: (Texture2D) -> Unit
    ) {
        val sw = src.width
        val sh = src.height
        if (max(sw, sh) < size) {
            return generate(srcFile, size / 2, callback)
        }
        val (w, h) = scale(sw, sh, size)
        if (w == sw && h == sh) {
            upload(srcFile, src, callback)
        } else {
            val dst = BufferedImage(
                w, h,
                if (src.colorModel.hasAlpha()) BufferedImage.TYPE_INT_ARGB
                else BufferedImage.TYPE_INT_RGB
            )
            val gfx = dst.createGraphics()
            gfx.drawImage(src, 0, 0, w, h, null)
            gfx.dispose()
            saveNUpload(srcFile, dstFile, dst, callback)
        }
    }

    private fun renderToBufferedImage(
        srcFile: FileReference,
        dstFile: File,
        callback: (Texture2D) -> Unit,
        w: Int, h: Int, render: () -> Unit
    ) {

        val buffer = IntArray(w * h)

        GFX.addGPUTask(w, h) {

            GFX.check()

            val fb2 = FBStack["generateVideoFrame", w, h, 4, false, 8]

            renderPurely {

                useFrame(0, 0, w, h, false, fb2, Renderer.colorRenderer) {

                    Frame.bind()

                    glClearColor(0f, 0f, 0f, 0f)
                    glClear(GL_COLOR_BUFFER_BIT)

                    drawTexture(
                        0, 0, w, h,
                        TextureLib.colorShowTexture,
                        -1, Vector4f(4f, 4f, 0f, 0f)
                    )

                    render()

                }

                // cannot read from separate framebuffer, only from null... why ever...
                useFrame(0, 0, w, h, false, null, Renderer.colorRenderer) {

                    fb2.bindTexture0(0, GPUFiltering.TRULY_NEAREST, Clamping.CLAMP)
                    GFX.copy()

                    // draw only the clicked area?
                    glFlush(); glFinish() // wait for everything to be drawn
                    glPixelStorei(GL_UNPACK_ALIGNMENT, 1)

                    glReadPixels(0, 0, w, h, GL_RGBA, GL_UNSIGNED_BYTE, buffer)

                    GFX.check()

                }

            }

            threadWithName("Thumbs::renderToBufferedImage()") {
                val dst = BufferedImage(w, h, 2)
                val buffer2 = dst.raster.dataBuffer
                for (i in 0 until w * h) {
                    val col = buffer[i]
                    // swizzle colors, because rgba != argb
                    buffer2.setElem(i, rgba(col.b(), col.g(), col.r(), col.a()))
                }
                saveNUpload(srcFile, dstFile, dst, callback)
            }

        }
    }

    private fun generateVideoFrame(
        srcFile: FileReference,
        dstFile: File,
        size: Int,
        callback: (Texture2D) -> Unit,
        wantedTime: Double
    ) {

        val meta = getMeta(srcFile, false)!!
        if (max(meta.videoWidth, meta.videoHeight) < size) return generate(srcFile, size / 2, callback)

        val scale = floor(max(meta.videoWidth, meta.videoHeight).toFloat() / size).toInt()

        val sw = meta.videoWidth / scale
        val sh = meta.videoHeight / scale

        val (w, h) = scale(sw, sh, size)
        if (w < 2 || h < 2) return

        if (w > GFX.width || h > GFX.height) {
            // cannot use this large size...
            // would cause issues
            return generate(srcFile, size, callback)
        }

        val fps = min(5.0, meta.videoFPS)
        val time = max(min(wantedTime, meta.videoDuration - 1 / fps), 0.0)
        val index = (time * fps).roundToInt()

        // LOGGER.info("requesting frame $index / $time / $fps fps from $srcFile")

        val src = waitUntilDefined(true) {
            getVideoFrame(srcFile, scale, index, 1, fps, 1000L, true)
        }

        // LOGGER.info("got frame for $srcFile")

        src.waitToLoad()

        // LOGGER.info("loaded frame for $srcFile")

        renderToBufferedImage(srcFile, dstFile, callback, w, h) {
            drawTexture(src)
        }

        // LOGGER.info("rendered $srcFile")

    }

    private fun generateSVGFrame(
        srcFile: FileReference,
        dstFile: File,
        size: Int,
        callback: (Texture2D) -> Unit
    ) {

        val buffer = MeshCache.getSVG(srcFile, Video.imageTimeout, false)!!

        val maxSize = max(buffer.maxX, buffer.maxY)
        val w = (size * buffer.maxX / maxSize).roundToInt()
        val h = (size * buffer.maxY / maxSize).roundToInt()

        if (w < 2 || h < 2) return

        val transform = Matrix4fArrayList()
        transform.scale(2f / (buffer.maxX / buffer.maxY).toFloat(), -2f, 2f)
        renderToBufferedImage(srcFile, dstFile, callback, w, h) {
            SVGxGFX.draw3DSVG(
                null, 0.0,
                transform, buffer, whiteTexture,
                Vector4f(1f), Filtering.NEAREST,
                whiteTexture.clamping, null
            )
        }

    }

    // png/bmp/jpg?
    private const val destinationFormat = "png"
    private fun generate(srcFile: FileReference, size: Int, callback: (Texture2D) -> Unit) {

        if (size < 1) return

        val dstFile = srcFile.getCacheFile(size)
        if (dstFile.exists()) {

            // LOGGER.info("cached preview for $srcFile exists")
            val image = ImageIO.read(dstFile)
            val rotation = ImageData.getRotation(srcFile)
            GFX.addGPUTask(size, size) {
                val texture = Texture2D(image)
                texture.rotation = rotation
                callback(texture)
            }

        } else {

            // LOGGER.info("cached preview for $srcFile needs to be created")

            // generate the image,
            // upload the result to the gpu
            // save the file

            try {
                when (val ext = srcFile.extension.lowercase(Locale.getDefault())) {
                    "hdr" -> {
                        val src = HDRImage(srcFile.file, true)
                        val sw = src.width
                        val sh = src.height
                        if (max(sw, sh) < size) return generate(srcFile, size / 2, callback)
                        val (w, h) = scale(sw, sh, size)
                        if (w < 2 || h < 2) return
                        val dst = src.createBufferedImage(w, h)
                        saveNUpload(srcFile, dstFile, dst, callback)
                    }
                    "svg" -> generateSVGFrame(srcFile, dstFile, size, callback)
                    // "png", "jpg", "jpeg" -> transformNSaveNUpload(ImageIO.read(srcFile))
                    // "ico" -> transformNSaveNUpload(Imaging.getBufferedImage(srcFile))
                    "pdf" -> {
                        val doc = PDFCache.getDocument(srcFile, false) ?: return
                        transformNSaveNUpload(
                            srcFile, PDFCache.getImage(doc, 1f, 0),
                            dstFile, size, callback
                        )
                    }
                    "webp", "tga" -> generateVideoFrame(srcFile, dstFile, size, callback, 0.0)
                    else -> {
                        val image = try {
                            ImageIO.read(srcFile.file)!!
                        } catch (e: Exception) {
                            try {
                                Imaging.getBufferedImage(srcFile.file)!!
                            } catch (e: Exception) {
                                when (val importType = ext.getImportType()) {
                                    "Video" -> {
                                        LOGGER.info("generating frame for $srcFile")
                                        generateVideoFrame(srcFile, dstFile, size, callback, 1.0)
                                        null
                                    }
                                    // else nothing to do
                                    else -> {
                                        LOGGER.info("ImageIO failed, Imaging failed, importType '$importType' != getImportType for $srcFile")
                                        null
                                    }
                                }
                            }
                        }
                        if (image != null) {
                            transformNSaveNUpload(srcFile, image, dstFile, size, callback)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                LOGGER.warn("${e.message}: $srcFile")
            }

        }
    }

    fun scale(w: Int, h: Int, size: Int): Pair<Int, Int> {
        return if (w > h) {
            Pair(size, h * size / w)
        } else {
            Pair(w * size / h, size)
        }
    }

}