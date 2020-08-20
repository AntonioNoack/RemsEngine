package me.anno.objects.cache

import me.anno.gpu.GFX
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.texture.Texture2D
import me.anno.image.HDRImage
import me.anno.objects.Image
import me.anno.objects.LoopingState
import me.anno.video.Frame
import org.apache.commons.imaging.Imaging
import org.joml.Matrix4f
import org.joml.Matrix4fArrayList
import org.joml.Vector4f
import java.io.File
import java.io.IOException
import javax.imageio.ImageIO
import kotlin.concurrent.thread

class ImageData(file: File): CacheData {

    var texture = Texture2D(1024,1024,1)
    var framebuffer: Framebuffer? = null

    init {
        val fileExtension = file.extension
        when(fileExtension.toLowerCase()){
            "hdr" -> {
                thread {
                    val img = HDRImage(file, true)
                    val w = img.width
                    val h = img.height
                    val pixels = img.pixelBuffer
                    GFX.addGPUTask {
                        texture.setSize(w, h)
                        texture.create(pixels)
                        35
                    }
                }
            }
            "png", "jpg", "jpeg" -> {
                texture.create {
                    ImageIO.read(file) ?: throw IOException("Format of $file is not supported.")
                }
            }
            "webp" -> {
                // calculate required scale? no, without animation, we don't need to scale it down ;)
                var frame: Frame?
                while(true){
                    frame = Cache.getVideoFrame(file, 1, 0, 0, 1.0, Image.imageTimeout, LoopingState.PLAY_ONCE)
                    if(frame != null && frame.isLoaded) break
                    Thread.sleep(1)
                }
                frame!!
                GFX.addGPUTask {
                    val fw = frame.w
                    val fh = frame.h
                    val framebuffer = Framebuffer(fw, fh, 1, 1, false, Framebuffer.DepthBufferType.NONE)
                    this.framebuffer = framebuffer
                    framebuffer.bind()
                    texture = framebuffer.textures[0]
                    val shader = frame.get3DShader().shader
                    GFX.shader3DUniforms(shader, Matrix4f(), Vector4f(1f, 1f, 1f, 1f))
                    frame.bind(0, true)
                    if(shader == GFX.shader3DYUV.shader){
                        val w = frame.w
                        val h = frame.h
                        shader.v2("uvCorrection", w.toFloat()/((w+1)/2*2), h.toFloat()/((h+1)/2*2))
                    }
                    GFX.flat01.draw(shader)
                    GFX.check()
                    10
                }
                // if(texture?.isLoaded == true) GFX.draw3D(stack, texture, color, nearestFiltering, tiling)
            }
            else -> {
                texture.create {
                    Imaging.getBufferedImage(file) ?: throw IOException("Format of $file is not supported.")
                }
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