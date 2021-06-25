package me.anno.cache.data

import com.drew.imaging.ImageMetadataReader
import com.drew.metadata.exif.ExifIFD0Directory
import me.anno.cache.instances.VideoCache.getVideoFrame
import me.anno.gpu.GFX
import me.anno.gpu.drawing.GFXx3D.shader3DUniforms
import me.anno.gpu.RenderSettings.useFrame
import me.anno.gpu.framebuffer.Frame
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.shader.Renderer
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.GPUFiltering
import me.anno.gpu.texture.Texture2D
import me.anno.image.HDRImage
import me.anno.io.FileReference
import me.anno.objects.Video.Companion.imageTimeout
import me.anno.objects.modes.RotateJPEG
import me.anno.utils.Nullable.tryOrException
import me.anno.utils.Nullable.tryOrNull
import me.anno.utils.Sleep.waitUntilDefined
import me.anno.utils.types.Strings.getImportType
import me.anno.video.VFrame
import org.apache.commons.imaging.Imaging
import org.apache.logging.log4j.LogManager
import java.awt.image.BufferedImage
import java.io.File
import java.util.*
import javax.imageio.ImageIO

class ImageData(file: FileReference) : ICacheData {

    var texture = Texture2D("image-data", 1024, 1024, 1)
    var framebuffer: Framebuffer? = null

    companion object {

        private val LOGGER = LogManager.getLogger(ImageData::class)

        fun getRotation(file: FileReference): RotateJPEG? = getRotation(file.file)

        fun getRotation(file: File): RotateJPEG? {
            var rotation: RotateJPEG? = null
            try {
                val metadata = ImageMetadataReader.readMetadata(file)
                for (dir in metadata.getDirectoriesOfType(ExifIFD0Directory::class.java)) {
                    val desc = dir.getDescription(ExifIFD0Directory.TAG_ORIENTATION)?.lowercase(Locale.getDefault())
                        ?: continue
                    val mirror = "mirror" in desc
                    val mirrorHorizontal = mirror && "hori" in desc
                    val mirrorVertical = mirror && !mirrorHorizontal
                    val rotationDegrees =
                        if ("9" in desc) 90 else if ("18" in desc) 180 else if ("27" in desc) 270 else 0
                    if (mirrorHorizontal || mirrorVertical || rotationDegrees != 0) {
                        rotation = RotateJPEG(mirrorHorizontal, mirrorVertical, rotationDegrees)
                    }
                }
            } catch (e: Exception) {
            }
            return rotation
        }

        fun frameToFramebuffer(frame: VFrame, w: Int, h: Int, id: ImageData?): Framebuffer {
            val framebuffer = Framebuffer("webp-temp", w, h, 1, 1, false, Framebuffer.DepthBufferType.NONE)
            id?.framebuffer = framebuffer
            useFrame(framebuffer, Renderer.colorRenderer) {
                Frame.bind()
                id?.texture = framebuffer.textures[0]
                val shader0 = frame.get3DShader()
                val shader = shader0.value
                shader.use()
                shader3DUniforms(shader, null, -1)
                frame.bind(0, GPUFiltering.TRULY_NEAREST, Clamping.CLAMP)
                frame.bindUVCorrection(shader)
                GFX.flat01.draw(shader)
                GFX.check()
            }
            GFX.check()
            return framebuffer
        }

    }

    fun useFFMPEG(file: FileReference) {
        // calculate required scale? no, without animation, we don't need to scale it down ;)
        val frame = waitUntilDefined(true) {
            getVideoFrame(file, 1, 0, 0, 1.0, imageTimeout, false)
        }
        frame.waitToLoad()
        GFX.addGPUTask(frame.w, frame.h) {
            frameToFramebuffer(frame, frame.w, frame.h, this)
        }
        // if(texture?.isLoaded == true) draw3D(stack, texture, color, nearestFiltering, tiling)
    }

    init {

        val fileExtension = file.extension
        // find jpeg rotation by checking exif tags...
        // they may appear on other images as well, so we don't filter for tags
        // this surely could be improved for improved performance...
        // get all tags:
        /*for (directory in metadata.directories) {
            for (tag in directory.tags) {
                println(tag)
            }
        }*/
        when (fileExtension.lowercase(Locale.getDefault())) {
            "hdr" -> {
                val img = HDRImage(file.file, true)
                val w = img.width
                val h = img.height
                GFX.addGPUTask(w, h) {
                    texture.setSize(w, h)
                    texture.create(img.pixelBuffer, img.byteBuffer)
                }
            }
            "webp" -> {
                useFFMPEG(file)
            }
            else -> {
                // read metadata information from jpegs
                // read the exif rotation header
                // because some camera images are rotated incorrectly
                if (fileExtension.getImportType() == "Video") {
                    useFFMPEG(file)
                } else {
                    val image = tryGetImage(file.file)
                    if (image != null) {
                        texture.create("ImageData", { image }, false)
                        texture.rotation = getRotation(file.file)
                    } else LOGGER.warn("Could not load $file")
                }
            }
        }
    }

    fun tryGetImage(file: File): BufferedImage? {
        // try ImageIO first, then Imaging, then give up (we could try FFMPEG, but idk, whether it supports sth useful)
        val image = tryOrNull { ImageIO.read(file) } ?: tryOrException { Imaging.getBufferedImage(file) }
        if (image is Exception) LOGGER.warn("Cannot read image from file $file: ${image.message}")
        return image as? BufferedImage
    }

    override fun destroy() {
        // framebuffer destroys the texture, too
        framebuffer?.destroy() ?: texture.destroy()
    }

}