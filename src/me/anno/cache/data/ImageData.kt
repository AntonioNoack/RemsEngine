package me.anno.cache.data

import me.anno.cache.ICacheData
import me.anno.cache.instances.VideoCache.getVideoFrame
import me.anno.config.DefaultConfig
import me.anno.gpu.GFX
import me.anno.gpu.GFXState.renderPurely
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.shader.Renderer.Companion.copyRenderer
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.GPUFiltering
import me.anno.gpu.texture.Texture2D
import me.anno.image.Image
import me.anno.image.ImageCPUCache
import me.anno.image.ImageReadable
import me.anno.image.ImageTransform
import me.anno.image.hdr.HDRImage
import me.anno.image.raw.toImage
import me.anno.image.tar.TGAImage
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.files.Signature
import me.anno.utils.Sleep.waitForGFXThreadUntilDefined
import me.anno.utils.types.Strings.getImportType
import me.anno.video.formats.gpu.GPUFrame
import org.apache.commons.imaging.Imaging
import org.apache.logging.log4j.LogManager
import java.io.InputStream
import javax.imageio.ImageIO

class ImageData(file: FileReference) : ICacheData {

    companion object {

        @JvmStatic
        val imageTimeout get() = DefaultConfig["ui.image.frameTimeout", 5000L]

        @JvmStatic
        private val LOGGER = LogManager.getLogger(ImageData::class)

        @JvmStatic
        fun getRotation(src: FileReference): ImageTransform? {
            if (src == InvalidRef || src.isDirectory) return null
            // which files can contain exif metadata?
            // according to https://exiftool.org/TagNames/EXIF.html,
            // JPG, TIFF, PNG, JP2, PGF, MIFF, HDP, PSP and XC, AVI and MOV
            return findRotation(src)
        }

        @JvmStatic
        fun frameToFramebuffer(frame: GPUFrame, w: Int, h: Int, result: ImageData) {
            val tmp = Framebuffer("webp-temp", w, h, 1, 1, false, DepthBufferType.NONE)
            useFrame(tmp, copyRenderer) {
                renderPurely {
                    val shader = frame.get2DShader()
                    shader.use()
                    frame.bind(0, GPUFiltering.LINEAR, Clamping.CLAMP)
                    frame.bindUVCorrection(shader)
                    GFX.flat01.draw(shader)
                    GFX.check()
                    result.texture = tmp.textures[0]
                }
            }
            GFX.check()
            tmp.destroyExceptTextures(false)
        }
    }

    var texture: Texture2D? = null
    var framebuffer: Framebuffer? = null
    var hasFailed = false

    init {
        if (file is ImageReadable) {
            val texture = Texture2D("image-data", 1024, 1024, 1)
            texture.create(file.toString(), file.readImage(), true)
            this.texture = texture
        } else {
            val cpuImage = ImageCPUCache.getImageWithoutGenerator(file)
            if (cpuImage != null) {
                val texture = Texture2D("image-data", cpuImage.width, cpuImage.height, 1)
                cpuImage.createTexture(texture, sync = true, checkRedundancy = true)
                this.texture = texture
            } else when (Signature.findNameSync(file)) {
                "hdr" -> {
                    val img = HDRImage(file)
                    val w = img.width
                    val h = img.height
                    val texture = Texture2D("image-data", w, h, 1)
                    img.createTexture(texture, sync = false, checkRedundancy = true)
                    this.texture = texture
                }
                "dds", "media", "webp" -> useFFMPEG(file)
                else -> {
                    val image = ImageCPUCache[file, 50, false]
                    if (image != null) {
                        val texture = Texture2D("image-data", image.width, image.height, 1)
                        texture.create(file.toString(), image, true)
                        texture.rotation = getRotation(file)
                        this.texture = texture
                    } else {
                        when (val fileExtension = file.lcExtension) {
                            // "hdr" -> loadHDR(file)
                            "tga" -> loadTGA(file)
                            // ImageIO says it can do webp, however it doesn't understand most pics...
                            // tga was incomplete as well -> we're using our own solution
                            "webp" -> useFFMPEG(file)
                            else -> tryGetImage0(file, fileExtension)
                        }
                    }
                }
            }
        }
    }

    fun useFFMPEG(file: FileReference) {
        // calculate required scale? no, without animation, we don't need to scale it down ;)
        val frame = waitForGFXThreadUntilDefined(true) {
            getVideoFrame(file, 1, 0, 0, 1.0, imageTimeout, false)
        }
        frame.waitToLoad()
        GFX.addGPUTask("ImageData.useFFMPEG", frame.w, frame.h) {
            frameToFramebuffer(frame, frame.w, frame.h, this)
        }
    }

    fun loadTGA(file: FileReference) {
        val img = file.inputStreamSync().use { stream ->
            TGAImage.read(stream, false)
        }
        val texture = Texture2D("image-data", img.width, img.height, 1)
        texture.create(img, sync = false, checkRedundancy = true)
        this.texture = texture
    }

    // find jpeg rotation by checking exif tags...
    // they may appear on other images as well, so we don't filter for tags
    // this surely could be improved for improved performance...
    // get all tags:
    /*for (directory in metadata.directories) {
        for (tag in directory.tags) {
            (tag)
        }
    }*/

    private fun tryGetImage0(file: FileReference, fileExtension: String) {
        // read metadata information from jpegs
        // read the exif rotation header
        // because some camera images are rotated incorrectly
        if (fileExtension.getImportType() == "Video") {
            useFFMPEG(file)
        } else tryGetImage1(file)
    }

    private fun tryGetImage1(file: FileReference) {
        val image = tryGetImage(file)
        if (image != null) {
            val texture = Texture2D("image-data", 1024, 1024, 1)
            texture.create(file.toString(), image, checkRedundancy = true)
            texture.rotation = getRotation(file)
            this.texture = texture
        } else {
            LOGGER.warn("Could not load {}", file)
            hasFailed = true
        }
    }

    private fun tryGetImage(file: FileReference): Image? {
        if (file is ImageReadable) return file.readImage()
        return tryGetImage(file, file.inputStreamSync())
    }

    private fun tryGetImage(file: FileReference, stream: InputStream): Image? {
        if (file is ImageReadable) return file.readImage()
        // try ImageIO first, then Imaging, then give up (we could try FFMPEG, but idk, whether it supports sth useful)
        val image = try {
            ImageIO.read(stream)
        } catch (e: Exception) {
            null
        } ?: try {
            Imaging.getBufferedImage(stream)
        } catch (e: Exception) {
            LOGGER.warn("Cannot read image from input $file", e)
            return null
        }
        return image.toImage()
    }

    override fun destroy() {
        // framebuffer destroys the texture, too
        framebuffer?.destroy() ?: texture?.destroy()
    }

}