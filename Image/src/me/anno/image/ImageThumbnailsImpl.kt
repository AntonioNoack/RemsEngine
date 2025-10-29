package me.anno.image

import me.anno.gpu.shader.renderer.Renderer
import me.anno.gpu.texture.Filtering
import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.TextureLib
import me.anno.gpu.texture.TextureReader
import me.anno.graph.hdb.HDBKey
import me.anno.image.aseprite.AseSprite
import me.anno.image.aseprite.AsepriteReader
import me.anno.image.aseprite.AsepriteToImage.frameToImage
import me.anno.image.jpg.JPGThumbnails
import me.anno.image.svg.DrawSVGs
import me.anno.image.svg.SVGMeshCache
import me.anno.image.tga.TGAReader
import me.anno.image.thumbs.ImageThumbnails.generateImage
import me.anno.image.thumbs.ThumbnailCache
import me.anno.image.thumbs.ThumbsRendering
import me.anno.io.files.FileReference
import me.anno.jvm.images.BIImage.toImage
import me.anno.utils.Color
import me.anno.utils.async.Callback
import me.anno.utils.async.Callback.Companion.mapAsync
import me.anno.utils.types.Floats.roundToIntOr
import net.sf.image4j.codec.ico.ICOReader
import org.joml.Matrix4fArrayList
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO
import kotlin.math.max

object ImageThumbnailsImpl {

    fun generateJPGFrame(
        srcFile: FileReference, dstFile: HDBKey, size: Int,
        callback: Callback<ITexture2D>
    ) {
        JPGThumbnails.extractThumbnail(srcFile) { bytes ->
            if (bytes != null) {
                ThumbnailCache.worker += {
                    generateJPGFrame2(srcFile, dstFile, size, callback, bytes)
                }
            } else generateImage(srcFile, dstFile, size, callback)
        }
    }

    private fun generateJPGFrame2(
        srcFile: FileReference, dstFile: HDBKey, size: Int,
        callback: Callback<ITexture2D>, bytes: ByteArray?
    ) {
        try {
            val image = ImageIO.read(ByteArrayInputStream(bytes))
            ThumbnailCache.transformNSaveNUpload(srcFile, true, image.toImage(), dstFile, size, callback)
        } catch (_: Exception) {
            generateImage(srcFile, dstFile, size, callback)
        }
    }

    fun generateTGAFrame(
        srcFile: FileReference, dstFile: HDBKey, size: Int,
        callback: Callback<ITexture2D>
    ) {
        srcFile.inputStream { it, exc ->
            if (it != null) {
                val src = it.use { TGAReader.read(it, false) }
                if (src is Image) {
                    ThumbnailCache.findScale(src, srcFile, size, callback) { dst ->
                        ThumbnailCache.saveNUpload(srcFile, false, dstFile, dst, callback)
                    }
                } else callback.err(src as? Exception)
            }
            exc?.printStackTrace()
        }
    }

    fun generateICOFrame(
        srcFile: FileReference, dstFile: HDBKey, size: Int,
        callback: Callback<ITexture2D>
    ) {
        srcFile.inputStream { it, exc ->
            if (it != null) {
                val image = it.use { ICOReader.read(it, size) }
                if (image is Image) ThumbnailCache.transformNSaveNUpload(srcFile, false, image, dstFile, size, callback)
                else callback.err(image as? Exception)
            } else exc?.printStackTrace()
        }
    }

    fun generateAsepriteFrame(
        srcFile: FileReference, dstFile: HDBKey, size: Int,
        callback: Callback<ITexture2D>
    ) {
        srcFile.inputStream { it, exc ->
            if (it != null) {
                val sprite = it.use { stream -> AsepriteReader.read(stream) }
                if (sprite is AseSprite) {
                    val image = sprite.frameToImage(sprite.frames.first())
                    ThumbnailCache.transformNSaveNUpload(srcFile, false, image, dstFile, size, callback)
                } else callback.err(sprite as? Exception)
            } else exc?.printStackTrace()
        }
    }

    fun generateSVGFrame(
        srcFile: FileReference, dstFile: HDBKey, size: Int,
        callback: Callback<ITexture2D>
    ) {
        SVGMeshCache[srcFile, TextureReader.imageTimeout]
            .waitFor(callback.mapAsync { buffer, cb1 ->
                val bounds = buffer.bounds
                val maxSize = max(bounds.maxX, bounds.maxY)
                val w = (size * bounds.maxX / maxSize).roundToIntOr()
                val h = (size * bounds.maxY / maxSize).roundToIntOr()
                if (w >= 2 && h >= 2) {
                    ThumbsRendering.renderToImage(
                        srcFile, false, dstFile, false,
                        Renderer.colorRenderer, false, cb1, w, h
                    ) {
                        val transform = Matrix4fArrayList()
                        transform.scale(bounds.maxY / bounds.maxX, -1f, 1f)
                        DrawSVGs.draw3DSVG(
                            transform, buffer,
                            TextureLib.whiteTexture, Color.white4,
                            Filtering.NEAREST, TextureLib.whiteTexture.clamping,
                            null
                        )
                    }
                } else callback.err(null)
            })
    }
}