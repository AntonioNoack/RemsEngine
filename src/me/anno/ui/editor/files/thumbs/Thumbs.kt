package me.anno.ui.editor.files.thumbs

import me.anno.gpu.GFX
import me.anno.gpu.GFXx2D
import me.anno.gpu.GFXx2D.draw2D
import me.anno.gpu.SVGxGFX
import me.anno.gpu.TextureLib
import me.anno.gpu.TextureLib.whiteTexture
import me.anno.gpu.blending.BlendDepth
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.framebuffer.Frame
import me.anno.image.HDRImage
import me.anno.image.svg.SVGMesh
import me.anno.io.config.ConfigBasics
import me.anno.io.xml.XMLElement
import me.anno.io.xml.XMLReader
import me.anno.objects.Video
import me.anno.cache.data.ImageData
import me.anno.cache.instances.MeshCache
import me.anno.cache.instances.TextureCache.getLateinitTexture
import me.anno.cache.instances.VideoCache.getVideoFrame
import me.anno.gpu.buffer.StaticBuffer
import me.anno.gpu.texture.*
import me.anno.utils.*
import me.anno.utils.Color.a
import me.anno.utils.Color.b
import me.anno.utils.Color.g
import me.anno.utils.Color.r
import me.anno.utils.Color.rgba
import me.anno.utils.StringHelper.getImportType
import me.anno.video.FFMPEGMetadata.Companion.getMeta
import me.anno.video.VFrame
import net.boeckling.crc.CRC64
import org.apache.commons.imaging.Imaging
import org.joml.Matrix4fArrayList
import org.joml.Vector4f
import org.lwjgl.opengl.GL11.*
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.concurrent.thread
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object Thumbs {

    private val folder = File(ConfigBasics.cacheFolder, "thumbs")
    private val sizes = intArrayOf(32, 64, 128, 256, 512)
    private val neededSizes = IntArray(sizes.last() + 1)
    private const val timeout = 5000L

    private fun File.getCacheFile(size: Int): File {
        val hashReadLimit = 256
        var hash = this.lastModified() xor (454781903L * this.length())
        if (!isDirectory) {
            val reader = inputStream().buffered()
            val bytes = reader.readNBytes2(hashReadLimit)
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

    fun getThumbnail(file: File, neededSize: Int): ITexture2D? {
        val size = getSize(neededSize)
        val key = ThumbnailKey(file, size)
        return getLateinitTexture(key, timeout) { callback ->
            generate(file, size, callback)
        }.texture
    }

    // png/bmp/jpg?
    private const val destinationFormat = "png"
    private fun generate(srcFile: File, size: Int, callback: (Texture2D) -> Unit) {

        if(size < 1) return

        val dstFile = srcFile.getCacheFile(size)
        if (dstFile.exists()) {

            val image = ImageIO.read(dstFile)
            val rotation = ImageData.getRotation(srcFile)
            GFX.addGPUTask(size, size) {
                val texture = Texture2D(image)
                texture.rotation = rotation
                callback(texture)
            }

        } else {

            // generate the image,
            // upload the result to the gpu
            // save the file

            fun upload(dst: BufferedImage) {
                val rotation = ImageData.getRotation(srcFile)
                GFX.addGPUTask(dst.width, dst.height) {
                    val texture = Texture2D(dst)
                    texture.rotation = rotation
                    callback(texture)
                }
            }

            fun saveNUpload(dst: BufferedImage) {
                dstFile.parentFile.mkdirs()
                ImageIO.write(dst, destinationFormat, dstFile)
                upload(dst)
            }

            fun transformNSaveNUpload(src: BufferedImage) {
                val sw = src.width
                val sh = src.height
                if (max(sw, sh) < size) {
                    return generate(srcFile, size / 2, callback)
                }
                var w = sw
                var h = sh
                if (w > h) {
                    h = h * size / w
                    w = size
                } else {
                    w = w * size / h
                    h = size
                }
                if (w == sw && h == sh) {
                    upload(src)
                } else {
                    val dst = BufferedImage(
                        w, h,
                        if (src.colorModel.hasAlpha()) BufferedImage.TYPE_INT_ARGB
                        else BufferedImage.TYPE_INT_RGB
                    )
                    val gfx = dst.createGraphics()
                    gfx.drawImage(src, 0, 0, w, h, null)
                    gfx.dispose()
                    saveNUpload(dst)
                }
            }

            fun renderToBufferedImage(w: Int, h: Int, render: () -> Unit) {

                val buffer = IntArray(w * h)

                GFX.addGPUTask(w, h) {

                    GFX.check()

                    val fb2 = FBStack["generateVideoFrame", w, h, 8, false]

                    BlendDepth(null, false) {

                        Frame(0, 0, w, h, false, fb2) {

                            Frame.bind()

                            glClearColor(0f, 0f, 0f, 0f)
                            glClear(GL_COLOR_BUFFER_BIT)

                            GFXx2D.drawTexture(
                                0, 0, w, h,
                                TextureLib.colorShowTexture,
                                -1, Vector4f(4f, 4f, 0f, 0f)
                            )

                            render()

                        }

                        // cannot read from separate framebuffer, only from null... why ever...
                        Frame(0, 0, w, h, false, null) {

                            fb2.bindTexture0(0, GPUFiltering.TRULY_NEAREST, Clamping.CLAMP)
                            GFX.copy()

                            // draw only the clicked area?
                            glFlush(); glFinish() // wait for everything to be drawn
                            glPixelStorei(GL_UNPACK_ALIGNMENT, 1)

                            glReadPixels(0, 0, w, h, GL_RGBA, GL_UNSIGNED_BYTE, buffer)

                            GFX.check()

                        }

                    }

                    thread {
                        val dst = BufferedImage(w, h, 2)
                        val buffer2 = dst.raster.dataBuffer
                        for (i in 0 until w * h) {
                            val col = buffer[i]
                            // swizzle colors, because rgba != argb
                            buffer2.setElem(i, rgba(col.b(), col.g(), col.r(), col.a()))
                        }
                        saveNUpload(dst)
                    }

                }
            }

            fun generateVideoFrame(wantedTime: Double) {

                val meta = getMeta(srcFile, false)!!
                if (max(meta.videoWidth, meta.videoHeight) < size) return generate(srcFile, size / 2, callback)

                val scale = floor(max(meta.videoWidth, meta.videoHeight).toFloat() / size).toInt()

                val sw = meta.videoWidth / scale
                val sh = meta.videoHeight / scale

                var w = sw
                var h = sh
                if (w > h) {
                    h = h * size / w
                    w = size
                } else {
                    w = w * size / h
                    h = size
                }

                if(w < 2 || h < 2) return

                if (w > GFX.width || h > GFX.height) {
                    // cannot use this large size...
                    // would cause issues
                    return generate(srcFile, size, callback)
                }

                val fps = min(5.0, meta.videoFPS)
                val time = max(min(wantedTime, meta.videoDuration - 1 / fps), 0.0)
                val index = (time * fps).roundToInt()

                var src: VFrame? = null
                while (src == null) {
                    src = getVideoFrame(srcFile, scale, index, 1, fps, 1000L, true)
                    Thread.sleep(0, 1000)
                }

                src.waitToLoad()

                // todo create an image from ffmpeg without using the gpu for downscaling
                // create frame buffer as target, and then read from it...
                renderToBufferedImage(w, h) {
                    draw2D(src)
                }

            }

            fun generateSVGFrame() {

                val bufferData = MeshCache.getEntry(
                    srcFile.absolutePath, "svg", 0,
                    Video.imageTimeout,
                    false
                ) {
                    val svg = SVGMesh()
                    svg.parse(XMLReader.parse(srcFile.inputStream().buffered()) as XMLElement)
                    val buffer = svg.buffer!!
                    buffer.setBounds(svg)
                    buffer
                } as StaticBuffer

                val maxSize = max(bufferData.maxX, bufferData.maxY)
                val w = (size * bufferData.maxX / maxSize).roundToInt()
                val h = (size * bufferData.maxY / maxSize).roundToInt()

                if(w < 2 || h < 2) return

                val transform = Matrix4fArrayList()
                transform.scale(2f / (bufferData.maxX / bufferData.maxY).toFloat(), -2f, 2f)
                renderToBufferedImage(w, h) {
                    SVGxGFX.draw3DSVG(
                        null, 0.0,
                        transform, bufferData, whiteTexture,
                        Vector4f(1f), Filtering.NEAREST,
                        whiteTexture.clamping, null
                    )
                }

            }

            try {
                when (val ext = srcFile.extension.toLowerCase()) {
                    "png", "jpg", "jpeg" -> transformNSaveNUpload(ImageIO.read(srcFile))
                    "webp" -> generateVideoFrame(0.0)
                    "hdr" -> {
                        val src = HDRImage(srcFile, true)
                        val sw = src.width
                        val sh = src.height
                        if (max(sw, sh) < size) {
                            return generate(srcFile, size / 2, callback)
                        }
                        var w = sw
                        var h = sh
                        if (w > h) {
                            h = h * size / w
                            w = size
                        } else {
                            w = w * size / h
                            h = size
                        }
                        if(w < 2 || h < 2) return
                        val dst = src.createBufferedImage(w, h)
                        saveNUpload(dst)
                    }
                    "ico" -> transformNSaveNUpload(Imaging.getBufferedImage(srcFile))
                    "svg" -> generateSVGFrame()
                    else -> {
                        when (ext.getImportType()) {
                            "Video" -> generateVideoFrame(1.0)
                            "Image", "Cubemap" -> transformNSaveNUpload(Imaging.getBufferedImage(srcFile))
                            // else nothing to do
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                println(srcFile)
            }

        }
    }

}