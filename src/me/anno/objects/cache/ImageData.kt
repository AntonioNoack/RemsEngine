package me.anno.objects.cache

import com.drew.imaging.ImageMetadataReader
import com.drew.metadata.exif.ExifIFD0Directory
import me.anno.gpu.GFX
import me.anno.gpu.GFXx3D.shader3DUniforms
import me.anno.gpu.ShaderLib.shader3DYUV
import me.anno.gpu.framebuffer.Frame
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.GPUFiltering
import me.anno.gpu.texture.Texture2D
import me.anno.image.HDRImage
import me.anno.objects.Video.Companion.imageTimeout
import me.anno.objects.modes.RotateJPEG
import me.anno.video.VFrame
import org.apache.commons.imaging.Imaging
import org.joml.Matrix4f
import org.joml.Vector4f
import java.io.File
import java.io.IOException
import javax.imageio.ImageIO
import kotlin.concurrent.thread


class ImageData(file: File) : CacheData {

    var texture = Texture2D("image-data",1024, 1024, 1)
    var framebuffer: Framebuffer? = null

    companion object {

        fun getRotation(file: File): RotateJPEG? {
            var rotation: RotateJPEG? = null
            try {
                val metadata = ImageMetadataReader.readMetadata(file)
                for(dir in metadata.getDirectoriesOfType(ExifIFD0Directory::class.java)){
                    val desc = dir.getDescription(ExifIFD0Directory.TAG_ORIENTATION)?.toLowerCase() ?: continue
                    val mirror = "mirror" in desc
                    val mirrorHorizontal = mirror && "hori" in desc
                    val mirrorVertical = mirror && !mirrorHorizontal
                    val rotationDegrees = if("9" in desc) 90 else if("18" in desc) 180 else if("27" in desc) 270 else 0
                    if(mirrorHorizontal || mirrorVertical || rotationDegrees != 0) {
                        rotation = RotateJPEG(mirrorHorizontal, mirrorVertical, rotationDegrees)
                    }
                }
            } catch (e: Exception){ }
            return rotation
        }

        fun frameToFramebuffer(frame: VFrame, w: Int, h: Int, id: ImageData?): Framebuffer {
            val framebuffer = Framebuffer("webp-temp", w, h, 1, 1, false, Framebuffer.DepthBufferType.NONE)
            id?.framebuffer = framebuffer
            Frame(framebuffer){
                id?.texture = framebuffer.textures[0]
                val shader = frame.get3DShader()
                shader3DUniforms(shader, Matrix4f(), Vector4f(1f, 1f, 1f, 1f))
                frame.bind(0, GPUFiltering.TRULY_NEAREST, Clamping.CLAMP)
                if (shader == shader3DYUV) {
                    val w2 = frame.w
                    val h2 = frame.h
                    shader.v2("uvCorrection", w2.toFloat() / ((w2 + 1) / 2 * 2), h2.toFloat() / ((h2 + 1) / 2 * 2))
                }
                GFX.flat01.draw(shader)
                GFX.check()
            }
            GFX.check()
            return framebuffer
        }

    }

    init {

        val fileExtension = file.extension
        // find jpeg rotation by checking exif tags...
        // they may appear on other images as well, so we don't filter for tags
        // this surely could be improved for improved performance...
        val rotation = getRotation(file)
        // get all tags:
        /*for (directory in metadata.directories) {
            for (tag in directory.tags) {
                println(tag)
            }
        }*/
        when (fileExtension.toLowerCase()) {
            "hdr" -> {
                thread {
                    val img = HDRImage(file, true)
                    val w = img.width
                    val h = img.height
                    val pixels = img.pixelBuffer
                    GFX.addGPUTask(w, h) {
                        texture.setSize(w, h)
                        texture.create(pixels)
                    }
                }
            }
            // todo read metadata information from jpegs
            // todo read the exif rotation header
            // todo because some camera images are rotated incorrectly
            "png", "jpg", "jpeg" -> {
                texture.create({
                    ImageIO.read(file) ?: throw IOException("Format of $file is not supported.")
                }, false)
                texture.rotation = rotation
            }
            "webp" -> {
                // calculate required scale? no, without animation, we don't need to scale it down ;)
                val frame = Cache.getVideoFrame(file, 1, 0, 0, 1.0, imageTimeout, false)!!
                frame.waitToLoad()
                GFX.addGPUTask(frame.w, frame.h) {
                    frameToFramebuffer(frame, frame.w, frame.h, this)
                }
                // if(texture?.isLoaded == true) draw3D(stack, texture, color, nearestFiltering, tiling)
            }
            else -> {
                texture.create({
                    try {
                        Imaging.getBufferedImage(file) ?: throw IOException("Format of $file is not supported.")
                    } catch (e: Exception) {
                        throw IOException("Format of $file is not supported: ${e.message}.")
                    }
                }, false)
            }
        }
        /*if(file.name.endsWith(".hdr", true)){

        } else if(file.name.endsWith(".ico") || file.name.endsWith("")){
            texture.create {
                ImageIO.read(file) ?: throw IOException("Format of $file is not supported.")
            }
        }*/
    }

    override fun destroy() {
        // framebuffer destroys the texture, too
        framebuffer?.destroy() ?: texture.destroy()
    }

}