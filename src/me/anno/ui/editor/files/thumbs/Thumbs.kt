package me.anno.ui.editor.files.thumbs

import me.anno.gpu.GFX
import me.anno.gpu.blending.BlendDepth
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.texture.Texture2D
import me.anno.image.HDRImage
import me.anno.io.config.ConfigBasics
import me.anno.objects.cache.Cache
import me.anno.objects.cache.ImageData
import me.anno.objects.cache.TextureCache
import me.anno.utils.*
import me.anno.video.FFMPEGMetadata.Companion.getMeta
import me.anno.video.Frame
import net.boeckling.crc.CRC64
import org.apache.commons.imaging.Imaging
import org.lwjgl.opengl.GL11.*
import java.awt.image.BufferedImage
import java.io.File
import java.lang.Exception
import javax.imageio.ImageIO
import kotlin.concurrent.thread
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object Thumbs {

    val folder = File(ConfigBasics.cacheFolder, "thumbs")

    fun File.getCacheFile(size: Int): File {
        val hashReadLimit = 256
        var hash = this.lastModified() xor (454781903L * this.length())
        if (!isDirectory) {
            val reader = inputStream().buffered()
            val bytes = reader.readNBytes(hashReadLimit)
            reader.close()
            hash = hash xor CRC64.fromInputStream(bytes.inputStream()).value
        }
        var hashString = hash.toULong().toString(16)
        while (hashString.length < 16) hashString = "0$hashString"
        return File(folder, "$size/${hashString.substring(0, 2)}/${hashString.substring(2)}.$destinationFormat")
    }


    private val sizes = intArrayOf(32, 64, 128, 256, 512)
    private val neededSizes = IntArray(sizes.last() + 1)
    private const val timeout = 5000L

    init {
        var index = 0
        for (size in sizes) {
            while (index <= size) {
                neededSizes[index++] = size
            }
        }
    }

    fun getSize(neededSize: Int): Int {
        return if (neededSize < neededSizes.size) {
            neededSizes[neededSize]
        } else {
            sizes.last()
        }
    }

    data class ThumbnailKey(val file: File, val size: Int)

    fun getThumbnail(file: File, neededSize: Int): Texture2D? {
        val size = getSize(neededSize)
        val key = ThumbnailKey(file, size)
        return (Cache.getEntry(key, timeout, false) {
            val cache = TextureCache(null)
            thread { generate(file, size) { cache.texture = it } }
            cache
        } as TextureCache?)?.texture as? Texture2D
    }

    // png/bmp/jpg?
    val destinationFormat = "png"
    fun generate(srcFile: File, size: Int, callback: (Texture2D) -> Unit) {

        if (size < 16) return // does not need to generate image (?)
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

            fun upload(dst: BufferedImage){
                val rotation = ImageData.getRotation(srcFile)
                GFX.addGPUTask(dst.width, dst.height) {
                    val texture = Texture2D(dst)
                    texture.rotation = rotation
                    callback(texture)
                }
            }

            fun saveNUpload(dst: BufferedImage){
                dstFile.parentFile.mkdirs()
                ImageIO.write(dst, "png", dstFile)
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
                if(w == sw && h == sh){
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

            fun generateVideoFrame(wantedTime: Double){

                val meta = getMeta(srcFile, false)!!
                if(max(meta.videoWidth, meta.videoHeight) < size) return generate(srcFile, size/2, callback)

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

                if(w > GFX.width || h > GFX.height){
                    // cannot use this large size...
                    // would cause issues
                    return generate(srcFile, size, callback)
                }

                val fps = min(5.0, meta.videoFPS)
                val time = max(min(wantedTime, meta.videoDuration - 1/fps), 0.0)
                val index = (time * fps).roundToInt()

                var src: Frame? = null
                while(src == null){
                    src = Cache.getVideoFrame(srcFile, scale, index, 1, fps, 1000L, false)
                    Thread.sleep(1)
                }

                src.waitToLoad()

                // todo create an image from ffmpeg without using the gpu for downscaling
                // create frame buffer as target, and then read from it...
                GFX.addGPUTask(sw, sh){

                    // framebuffer to buffered image

                    val fb: Framebuffer? = null // FBStack[rw, rh, false]

                    GFX.clip(0, 0, w, h)

                    val bd = BlendDepth(null, false)
                    bd.bind()

                    // some thumbnails are broken, probably by overlapping time frames... but how? everything is synced...
                    // fixed by clearing the screen???
                    glClearColor(0f, 0f, 0f, 1f)
                    glClear(GL_COLOR_BUFFER_BIT)

                    GFX.draw2D(src)

                    // draw only the clicked area?
                    GFX.check()
                    fb?.bind() ?: Framebuffer.bindNull()
                    glFlush(); glFinish() // wait for everything to be drawn
                    glPixelStorei(GL_UNPACK_ALIGNMENT, 1)
                    val buffer = IntArray(w * h)
                    glReadPixels(0, GFX.height - h, w, h,
                        GL_RGBA, GL_UNSIGNED_BYTE, buffer
                    )
                    Framebuffer.unbind()

                    bd.unbind()

                    thread {
                        val dst = BufferedImage(w, h, 1)
                        val buffer2 = dst.raster.dataBuffer
                        for(i in 0 until w * h){
                            val col = buffer[i]
                            // swizzle colors, because rgba != argb
                            buffer2.setElem(i, rgba(col.b(), col.g(), col.r(), 255))
                        }
                        saveNUpload(dst)
                    }

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
                        val dst = src.createBufferedImage(w, h)
                        saveNUpload(dst)
                    }
                    "ico" -> transformNSaveNUpload(Imaging.getBufferedImage(srcFile))
                    else -> {
                        when (ext.getImportType()) {
                            "Video" -> generateVideoFrame(0.0)
                            "Image", "Cubemap" -> transformNSaveNUpload(Imaging.getBufferedImage(srcFile))
                            // else nothing to do
                        }
                    }
                }
            } catch (e: Exception){
                e.printStackTrace()
                println(srcFile)
            }

        }
    }

}