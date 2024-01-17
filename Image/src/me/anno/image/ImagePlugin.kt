package me.anno.image

import me.anno.extensions.plugins.Plugin
import me.anno.gpu.drawing.SVGxGFX
import me.anno.gpu.shader.renderer.Renderer
import me.anno.gpu.texture.Filtering
import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.ImageToTexture
import me.anno.gpu.texture.TextureLib
import me.anno.graph.hdb.HDBKey
import me.anno.image.exr.EXRReader
import me.anno.image.gimp.GimpImage
import me.anno.image.jpg.ExifOrientation
import me.anno.image.jpg.JPGThumbnails
import me.anno.image.qoi.QOIReader
import me.anno.image.raw.toImage
import me.anno.image.svg.SVGMesh
import me.anno.image.svg.SVGMeshCache
import me.anno.image.tar.TGAReader
import me.anno.io.files.FileReference
import me.anno.io.files.inner.InnerFolderCache
import me.anno.io.files.thumbs.Thumbs
import me.anno.utils.Color
import me.anno.video.ffmpeg.MediaMetadata
import net.sf.image4j.codec.ico.ICOReader
import org.joml.Matrix4fArrayList
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO
import kotlin.math.max
import kotlin.math.roundToInt

class ImagePlugin : Plugin() {
    override fun onEnable() {
        super.onEnable()

        // image loading
        ImageCache.registerStreamReader("tga") { TGAReader.read(it, false) }
        ImageCache.registerStreamReader("gimp") { GimpImage.read(it) }
        ImageCache.registerStreamReader("exr") { EXRReader.read(it) }
        ImageCache.registerStreamReader("qoi") { QOIReader.read(it) }
        ImageCache.registerStreamReader("ico") { ICOReader.read(it) }

        // image loading with extra details
        InnerFolderCache.register("gimp", GimpImage.Companion::readAsFolder)
        InnerFolderCache.register("svg", SVGMesh.Companion::readAsFolder)

        // extracting size information quickly
        MediaMetadata.registerSignatureHandler(100) { file, signature, dst ->
            if (signature == "gimp") {
                // Gimp files are a special case, which is not covered by FFMPEG
                dst.ready = false
                file.inputStream { it, exc ->
                    if (it != null) {
                        dst.setImage(GimpImage.findSize(it))
                    } else exc?.printStackTrace()
                    dst.ready = true
                }
                true
            } else false
        }
        MediaMetadata.registerSignatureHandler(100) { _, signature, dst ->
            if (signature == "qoi") {
                // we have a simple reader, so use it :)
                dst.setImageByStream(QOIReader::findSize)
            } else false
        }
        MediaMetadata.registerSignatureHandler(100) { _, signature, dst ->
            if (signature == "ico") {
                dst.setImageByStream(ICOReader::findSize)
            } else false
        }
        MediaMetadata.registerSignatureHandler(100) { file, signature, dst ->
            if (signature == "" || signature == null) {
                when (file.lcExtension) {
                    "tga" -> dst.setImageByStream(TGAReader::findSize)
                    "ico" -> dst.setImageByStream(ICOReader::findSize)
                    else -> false // unknown
                }
            } else false
        }

        // thumbnails
        Thumbs.registerSignature("qoi", Thumbs::generateImage)
        Thumbs.registerSignature("jpg", ::generateJPGFrame)
        Thumbs.registerSignature("ico", ::generateICOFrame)
        Thumbs.registerExtension("tga", ::generateTGAFrame)
        Thumbs.registerExtension("ico", ::generateICOFrame)
        Thumbs.registerExtension("svg", ::generateSVGFrame)

        // rotating jpegs
        ImageToTexture.findExifRotation = ExifOrientation::findRotation
    }

    private fun generateJPGFrame(
        srcFile: FileReference, dstFile: HDBKey, size: Int,
        callback: (ITexture2D?, Exception?) -> Unit
    ) {
        JPGThumbnails.extractThumbnail(srcFile) { bytes ->
            if (bytes != null) {
                try {
                    val image = ImageIO.read(ByteArrayInputStream(bytes))
                    Thumbs.transformNSaveNUpload(srcFile, true, image.toImage(), dstFile, size, callback)
                } catch (e: Exception) {
                    Thumbs.generateImage(srcFile, dstFile, size, callback)
                }
            } else Thumbs.generateImage(srcFile, dstFile, size, callback)
        }
    }

    private fun generateTGAFrame(
        srcFile: FileReference, dstFile: HDBKey, size: Int,
        callback: (ITexture2D?, Exception?) -> Unit
    ) {
        srcFile.inputStream { it, exc ->
            if (it != null) {
                val src = it.use { TGAReader.read(it, false) }
                Thumbs.findScale(src, srcFile, size, callback) { dst ->
                    Thumbs.saveNUpload(srcFile, false, dstFile, dst, callback)
                }
            }
            exc?.printStackTrace()
        }
    }

    private fun generateICOFrame(
        srcFile: FileReference, dstFile: HDBKey, size: Int,
        callback: (ITexture2D?, Exception?) -> Unit
    ) {
        srcFile.inputStream { it, exc ->
            if (it != null) {
                val image = it.use { ICOReader.read(it, size) }
                Thumbs.transformNSaveNUpload(srcFile, false, image, dstFile, size, callback)
            } else exc?.printStackTrace()
        }
    }

    private fun generateSVGFrame(
        srcFile: FileReference, dstFile: HDBKey, size: Int,
        callback: (ITexture2D?, Exception?) -> Unit
    ) {

        val buffer = SVGMeshCache[srcFile, ImageToTexture.imageTimeout, false]!!
        val bounds = buffer.bounds!!
        val maxSize = max(bounds.maxX, bounds.maxY)
        val w = (size * bounds.maxX / maxSize).roundToInt()
        val h = (size * bounds.maxY / maxSize).roundToInt()

        if (w < 2 || h < 2) return

        val transform = Matrix4fArrayList()
        transform.scale(bounds.maxY / bounds.maxX, 1f, 1f)
        Thumbs.renderToImage(srcFile, false, dstFile, false, Renderer.colorRenderer, false, callback, w, h) {
            SVGxGFX.draw3DSVG(
                transform,
                buffer,
                TextureLib.whiteTexture,
                Color.white4,
                Filtering.NEAREST,
                TextureLib.whiteTexture.clamping,
                null
            )
        }
    }
}