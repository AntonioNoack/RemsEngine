package me.anno.image

import me.anno.extensions.plugins.Plugin
import me.anno.gpu.texture.TextureReader
import me.anno.image.exr.EXRReader
import me.anno.image.gimp.GimpImage
import me.anno.image.jpg.ExifOrientation
import me.anno.image.qoi.QOIReader
import me.anno.image.svg.SVGMesh
import me.anno.image.tar.TGAReader
import me.anno.image.thumbs.ImageThumbnails
import me.anno.image.thumbs.Thumbs
import me.anno.io.MediaMetadata
import me.anno.io.files.inner.InnerFolderCache
import net.sf.image4j.codec.ico.ICOReader

class ImagePlugin : Plugin() {
    override fun onEnable() {
        super.onEnable()
        registerImageLoading()
        registerInnerFolder()
        registerMediaMetadata()
        registerThumbnails()
        registerRotatingJpegs()
    }

    private fun registerImageLoading() {
        ImageCache.registerDirectStreamReader("tga", TGAReader::read)
        ImageCache.registerDirectStreamReader("gimp", GimpImage::read)
        ImageCache.registerDirectStreamReader("exr", EXRReader::read)
        ImageCache.registerDirectStreamReader("qoi", QOIReader::read)
        ImageCache.registerDirectStreamReader("ico", ICOReader::read)
        ImageImpl.register()
    }

    private fun registerInnerFolder() {
        // image loading with extra details
        InnerFolderCache.registerSignatures("gimp", GimpImage.Companion::readAsFolder)
        InnerFolderCache.registerSignatures("svg", SVGMesh.Companion::readAsFolder)
    }

    private fun registerMediaMetadata() {
        MediaMetadata.registerSignatureHandler(100, "gimp") { file, signature, dst ->
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
        MediaMetadata.registerSignatureHandler(100, "qoi") { _, signature, dst ->
            if (signature == "qoi") {
                // we have a simple reader, so use it :)
                dst.setImageByStream(QOIReader::findSize)
            } else false
        }
        MediaMetadata.registerSignatureHandler(100, "ico") { _, signature, dst ->
            if (signature == "ico") {
                dst.setImageByStream(ICOReader::findSize)
            } else false
        }
        MediaMetadata.registerSignatureHandler(100, "tga/ico") { file, signature, dst ->
            if (signature == "" || signature == null) {
                when (file.lcExtension) {
                    "tga" -> dst.setImageByStream(TGAReader::findSize)
                    "ico" -> dst.setImageByStream(ICOReader::findSize)
                    else -> false // unknown
                }
            } else false
        }
    }

    private fun registerThumbnails() {
        Thumbs.registerSignatures("qoi", ImageThumbnails::generateImage)
        Thumbs.registerSignatures("jpg", ImageThumbnailsImpl::generateJPGFrame)
        Thumbs.registerSignatures("ico", ImageThumbnailsImpl::generateICOFrame)
        Thumbs.registerSignatures("xml", ImageThumbnailsImpl::generateSVGFrame)
        Thumbs.registerFileExtensions("tga", ImageThumbnailsImpl::generateTGAFrame)
        Thumbs.registerFileExtensions("ico", ImageThumbnailsImpl::generateICOFrame)
        Thumbs.registerFileExtensions("svg", ImageThumbnailsImpl::generateSVGFrame)
        ImageAsFolder.readIcoLayers = ICOReader::readAllLayers
    }

    private fun registerRotatingJpegs() {
        // rotating jpegs
        TextureReader.findExifRotation = ExifOrientation::findRotation
    }

    override fun onDisable() {
        super.onDisable()
        ImageCache.unregister("tga,gimp,exr,qoi,ico")
        InnerFolderCache.unregisterSignatures("gimp,svg")
        Thumbs.unregisterSignatures("qoi,jpg,ico")
        Thumbs.unregisterFileExtensions("tga,ico,tga,ico")
        MediaMetadata.unregister("gimp,qoi,ico,gimp")
        TextureReader.findExifRotation = null
    }
}