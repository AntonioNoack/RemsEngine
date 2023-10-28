package me.anno.image

import me.anno.cache.AsyncCacheData
import me.anno.gpu.texture.TextureLib.blackTexture
import me.anno.gpu.texture.TextureLib.missingTexture
import me.anno.gpu.texture.TextureLib.whiteTexture
import me.anno.image.raw.*
import me.anno.io.files.BundledRef
import me.anno.io.files.FileFileRef
import me.anno.io.files.FileReference
import me.anno.io.files.Signature
import me.anno.io.zip.InnerFolder
import me.anno.io.zip.SignatureFile
import me.anno.maths.Maths
import me.anno.utils.OS
import me.anno.utils.Sleep
import me.anno.video.ffmpeg.FFMPEGMetadata
import me.anno.video.ffmpeg.FFMPEGStream
import net.sf.image4j.codec.ico.ICOReader
import org.apache.commons.imaging.Imaging
import org.apache.logging.log4j.LogManager
import java.io.IOException
import javax.imageio.ImageIO

/**
 * an easy interface to read any image as rgba and individual channels
 * */
object ImageReader {

    private val LOGGER = LogManager.getLogger(ImageReader::class)

    @JvmStatic
    fun readAsFolder(file: FileReference, callback: (InnerFolder?, Exception?) -> Unit) {

        // todo white with transparency, black with transparency
        //  (overriding color)

        val folder = InnerFolder(file)

        // add the most common swizzles: r,g,b,a
        createComponent(file, folder, "r.png", "r", false)
        createComponent(file, folder, "g.png", "g", false)
        createComponent(file, folder, "b.png", "b", false)
        createComponent(file, folder, "a.png", "a", false)

        // bgra
        createComponent(file, folder, "bgra.png") {
            if (it is BGRAImage) it.base // bgra.bgra = rgba
            else BGRAImage(it)
        }

        // inverted components
        createComponent(file, folder, "1-r.png", "r", true)
        createComponent(file, folder, "1-g.png", "g", true)
        createComponent(file, folder, "1-b.png", "b", true)
        createComponent(file, folder, "1-a.png", "a", true)

        // grayscale, if not only a single channel
        createComponent(file, folder, "grayscale.png") {
            if (it.numChannels > 1) GrayscaleImage(it)
            else it
        }

        // rgb without alpha, if alpha exists
        createComponent(file, folder, "rgb.png") {
            if (it.hasAlphaChannel) OpaqueImage(it)
            else it
        }

        if (file.lcExtension == "ico") {
            Signature.findName(file) { sign ->
                if (sign == null || sign == "ico") {
                    file.inputStream { it, exc ->
                        if (it != null) {
                            val layers = ICOReader.readAllLayers(it)
                            for (index in layers.indices) {
                                val layer = layers[index]
                                folder.createImageChild("layer$index", layer)
                            }
                            it.close()
                            callback(folder, null)
                        } else {
                            exc?.printStackTrace()
                            callback(folder, null)
                        }
                    }
                } else callback(folder, null)
            }
            return
        }

        callback(folder, null)
    }

    @JvmStatic
    private fun createComponent(file: FileReference, folder: InnerFolder, name: String, createImage: (Image) -> Image) {
        folder.createLazyImageChild(name, lazy {
            val src = ImageCPUCache[file, false] ?: throw IOException("Missing image of $file")
            createImage(src)
        }, lazy {
            val src = ImageGPUCache[file, false] ?: throw IOException("Missing image of $file")
            createImage(GPUImage(src))
        })
    }

    @JvmStatic
    private fun createComponent(
        file: FileReference, folder: InnerFolder, name: String,
        swizzle: String, inverse: Boolean = false
    ) {
        when (swizzle.length) {
            1 -> createComponent(file, folder, name) {
                when {
                    it.numChannels == 0 -> GPUImage(missingTexture)
                    (swizzle == "a" && !it.hasAlphaChannel) -> GPUImage(whiteTexture)
                    (swizzle == "b" && it.numChannels < 3) || (swizzle == "g" && it.numChannels < 2) ->
                        GPUImage(blackTexture)
                    else -> ComponentImage(it, inverse, swizzle[0])
                }
            }
            else -> throw NotImplementedError(swizzle)
        }
    }

    fun shouldUseFFMPEG(signature: String?, file: FileReference): Boolean {
        if (OS.isWeb) return false // uncomment, when we support FFMPEG in the browser XD
        return signature == "dds" || signature == "media" || file.lcExtension == "webp"
    }

    fun shouldIgnore(signature: String?): Boolean {
        return when (signature) {
            "rar", "bz2", "zip", "tar", "gzip", "xz", "lz4", "7z", "xar", "oar", "java", "text",
            "wasm", "ttf", "woff1", "woff2", "shell", "xml", "svg", "exe",
            "vox", "fbx", "gltf", "obj", "blend", "mesh-draco", "md2", "md5mesh", "dae",
            "yaml" -> true
            else -> false
        }
    }

    fun readImage(file: FileReference, data: AsyncCacheData<Image?>, forGPU: Boolean) {
        if (file is ImageReadable) {
            data.value = if (forGPU) file.readGPUImage() else file.readCPUImage()
        } else if (file is BundledRef || (file !is SignatureFile && file.length() < 10_000_000L)) { // < 10MB -> read directly
            file.readBytes { bytes, exc ->
                exc?.printStackTrace()
                if (bytes != null) {
                    readImage(file, data, bytes)
                } else data.value = null
            }
        } else Signature.findName(file) { signature ->
            readImage(file, data, signature)
        }
    }

    fun readImage(file: FileReference, data: AsyncCacheData<Image?>, bytes: ByteArray) {
        val signature = Signature.findName(bytes)
        if (shouldIgnore(signature)) {
            data.value = null
        } else if (shouldUseFFMPEG(signature, file)) {
            tryFFMPEG(file, signature) { it, e ->
                data.value = it
                e?.printStackTrace()
            }
        } else {
            val reader = ImageCPUCache.byteReaders[signature] ?: ImageCPUCache.byteReaders[file.lcExtension]
            data.value = if (reader != null) reader(bytes) else tryGeneric(file, bytes)
        }
    }

    fun readImage(file: FileReference, data: AsyncCacheData<Image?>, signature: String?) {
        if (shouldIgnore(signature)) {
            data.value = null
        } else if (shouldUseFFMPEG(signature, file)) {
            tryFFMPEG(file, signature) { it, e ->
                data.value = it
                e?.printStackTrace()
            }
        } else {
            val reader = ImageCPUCache.fileReaders[signature] ?: ImageCPUCache.fileReaders[file.lcExtension]
            if (reader != null) reader(file) { it, e ->
                e?.printStackTrace()
                data.value = it
            } else tryGeneric(file) { it, e ->
                e?.printStackTrace()
                data.value = it
            }
        }
    }


    private fun tryFFMPEG(file: FileReference, signature: String?, callback: ImageCallback) {
        return if (file is FileFileRef) {
            val meta = FFMPEGMetadata.getMeta(file, false)!!
            val sequence = FFMPEGStream.getImageSequenceCPU(
                file, signature, meta.videoWidth, meta.videoHeight,
                Maths.min(20, (meta.videoFrameCount - 1) / 3),
                1, meta.videoFPS, meta.videoWidth, meta.videoFPS,
                meta.videoFrameCount
            )
            Sleep.waitUntil(true) { sequence.frames.size > 0 || sequence.isFinished }
            callback(sequence.frames.first(), null)
        } else {
            // todo when we have native ffmpeg, don't copy the file
            val tmp = FileFileRef.createTempFile("4ffmpeg", file.extension)
            file.readBytes { bytes, e ->
                if (bytes != null) {
                    tmp.writeBytes(bytes)
                    tryFFMPEG(FileReference.getReference(tmp), signature, callback)
                    tmp.delete()
                } else callback(null, e)
            }
        }
    }

    private fun tryGeneric(file: FileReference, callback: ImageCallback) {
        file.inputStream { it, exc ->
            if (it != null) {
                try {
                    val img = ImageIO.read(it) ?: throw IOException(file.toString())
                    it.close()
                    callback(img.toImage(), null)
                } catch (e: Exception) {
                    it.close()
                    file.inputStream { it2, exc2 ->
                        if (it2 != null) {
                            try {
                                callback(Imaging.getBufferedImage(it2).toImage(), null)
                            } catch (e: Exception) {
                                callback(null, e)
                            } finally {
                                it2.close()
                            }
                        } else callback(null, exc2)
                    }
                }
            } else callback(null, exc)
        }
    }

    private fun tryGeneric(file: FileReference, bytes: ByteArray): Image? {
        var image = try {
            ImageIO.read(bytes.inputStream())
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
        if (image == null) {
            LOGGER.debug("ImageIO failed for {}", file)
            try {
                image = Imaging.getBufferedImage(bytes)
            } catch (e: Exception) {
                // e.printStackTrace()
            }
        }
        if (image == null) {
            LOGGER.debug("Imaging failed for {}", file)
        }
        return image?.toImage()
    }
}