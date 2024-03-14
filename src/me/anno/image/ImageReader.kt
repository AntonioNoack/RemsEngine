package me.anno.image

import me.anno.cache.AsyncCacheData
import me.anno.gpu.texture.TextureCache
import me.anno.gpu.texture.TextureLib.blackTexture
import me.anno.gpu.texture.TextureLib.missingColors
import me.anno.gpu.texture.TextureLib.missingTexture
import me.anno.gpu.texture.TextureLib.whiteTexture
import me.anno.image.raw.AlphaMaskImage
import me.anno.image.raw.BGRAImage
import me.anno.image.raw.ComponentImage
import me.anno.image.raw.GPUImage
import me.anno.image.raw.GrayscaleImage
import me.anno.image.raw.IntImage
import me.anno.image.raw.OpaqueImage
import me.anno.io.MediaMetadata
import me.anno.io.files.BundledRef
import me.anno.io.files.FileReference
import me.anno.io.files.Signature
import me.anno.io.files.inner.InnerFolder
import me.anno.io.files.inner.SignatureFile
import me.anno.maths.Maths
import me.anno.utils.OS
import me.anno.utils.structures.Callback
import org.apache.logging.log4j.LogManager
import java.io.InputStream

/**
 * an easy interface to read any image as rgba and individual channels
 * */
object ImageReader {

    private val LOGGER = LogManager.getLogger(ImageReader::class)
    private val missingImage = IntImage(2, 2, missingColors, false)

    var tryFFMPEG: ((file: FileReference, signature: String?, forGPU: Boolean, callback: Callback<Image>) -> Unit)? =
        null

    var readIcoLayers: ((InputStream) -> List<Image>)? = null

    @JvmStatic
    fun readAsFolder(file: FileReference, callback: Callback<InnerFolder>) {

        val folder = InnerFolder(file)

        // add the most common swizzles: r,g,b,a
        createComponent(file, folder, "r.png", 'r', false)
        createComponent(file, folder, "g.png", 'g', false)
        createComponent(file, folder, "b.png", 'b', false)
        createComponent(file, folder, "a.png", 'a', false)

        // bgra
        createComponent(file, folder, "bgra.png") {
            if (it is BGRAImage) it.base // bgra.bgra = rgba
            else BGRAImage(it)
        }

        // inverted components
        createComponent(file, folder, "1-r.png", 'r', true)
        createComponent(file, folder, "1-g.png", 'g', true)
        createComponent(file, folder, "1-b.png", 'b', true)
        createComponent(file, folder, "1-a.png", 'a', true)

        // white with transparency, black with transparency (overriding color)
        createAlphaMask(file, folder, "111a.png", false)
        createAlphaMask(file, folder, "000a.png", true)

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

        val ric = readIcoLayers
        if (file.lcExtension == "ico" && ric != null) {
            Signature.findName(file) { sign ->
                if (sign == null || sign == "ico") {
                    file.inputStream { it, exc ->
                        if (it != null) {
                            val layers = ric(it)
                            for (index in layers.indices) {
                                val layer = layers[index] as? Image ?: break
                                folder.createImageChild("layer$index", layer)
                            }
                            it.close()
                            callback.ok(folder)
                        } else {
                            exc?.printStackTrace()
                            callback.ok(folder)
                        }
                    }
                } else callback.ok(folder)
            }
            return // we're done, don't call callback twice
        }

        callback.ok(folder)
    }

    @JvmStatic
    private fun createComponent(file: FileReference, folder: InnerFolder, name: String, createImage: (Image) -> Image) {
        folder.createLazyImageChild(name, lazy {
            val src = ImageCache[file, false] ?: run {
                LOGGER.warn("Missing texture for $file")
                missingImage
            }
            createImage(src)
        }, {
            val src = TextureCache[file, false] ?: run {
                LOGGER.warn("Missing texture for $file")
                missingTexture
            }
            createImage(GPUImage(src))
        })
    }

    @JvmStatic
    private fun createComponent(
        file: FileReference, folder: InnerFolder, name: String,
        swizzle: Char, inverse: Boolean
    ) {
        createComponent(file, folder, name) { srcImage ->
            when {
                (swizzle == 'a' && !srcImage.hasAlphaChannel) -> GPUImage(if (inverse) blackTexture else whiteTexture)
                (swizzle == 'b' && srcImage.numChannels < 3) || (swizzle == 'g' && srcImage.numChannels < 2) ->
                    GPUImage(if (inverse) whiteTexture else blackTexture)
                else -> ComponentImage(srcImage, inverse, swizzle)
            }
        }
    }

    @JvmStatic
    private fun createAlphaMask(file: FileReference, folder: InnerFolder, name: String, black: Boolean) {
        createComponent(file, folder, name) { srcImage ->
            if (srcImage.hasAlphaChannel) {
                val color1 = if (black) 0 else 0xffffff
                AlphaMaskImage(srcImage, false, 'a', color1)
            } else {
                GPUImage(if (black) blackTexture else whiteTexture)
            }
        }
    }

    private fun shouldUseFFMPEG(signature: String?, file: FileReference): Boolean {
        if (OS.isWeb) return false // uncomment, when we support FFMPEG in the browser XD
        return signature == "dds" || signature == "media" || file.lcExtension == "webp"
    }

    private fun shouldIgnore(signature: String?): Boolean {
        return when (signature) {
            "rar", "bz2", "zip", "tar", "gzip", "xz", "lz4", "7z", "xar", "oar", "java", "text",
            "wasm", "ttf", "woff1", "woff2", "shell", "xml", "svg", "exe",
            "vox", "fbx", "gltf", "obj", "blend", "mesh-draco", "md2", "md5mesh", "dae",
            "yaml" -> true
            else -> false
        }
    }

    fun readImage(file: FileReference, forGPU: Boolean): AsyncCacheData<Image> {
        return readImage(file, AsyncCacheData(), forGPU)
    }

    fun readImage(file: FileReference, data: AsyncCacheData<Image>, forGPU: Boolean): AsyncCacheData<Image> {
        if (file is ImageReadable) {
            data.value = if (forGPU) file.readGPUImage() else file.readCPUImage()
        } else if (file is BundledRef || (file !is SignatureFile && file.length() < 10_000_000L)) { // < 10MB -> read directly
            file.readBytes { bytes, exc ->
                exc?.printStackTrace()
                if (bytes != null) {
                    readImage(file, data, bytes, forGPU)
                } else {
                    data.value = null
                    data.hasValue = true
                }
            }
        } else Signature.findName(file) { signature ->
            readImage(file, data, signature, forGPU)
        }
        return data
    }

    private fun readImage(file: FileReference, data: AsyncCacheData<Image>, bytes: ByteArray, forGPU: Boolean) {
        val signature = Signature.findName(bytes)
        val tryFFMPEG = tryFFMPEG
        if (shouldIgnore(signature)) {
            data.value = null
        } else if (tryFFMPEG != null && shouldUseFFMPEG(signature, file)) {
            tryFFMPEG(file, signature, forGPU) { it, e ->
                data.value = it
                e?.printStackTrace()
            }
        } else {
            val reader = ImageCache.byteReaders[signature] ?: ImageCache.byteReaders[file.lcExtension]
            if (reader != null) reader(bytes) { it, e ->
                data.value = it
                e?.printStackTrace()
            }
        }
    }

    private fun readImage(file: FileReference, data: AsyncCacheData<Image>, signature: String?, forGPU: Boolean) {
        val tryFFMPEG = tryFFMPEG
        if (shouldIgnore(signature)) {
            data.value = null
        } else if (tryFFMPEG != null && shouldUseFFMPEG(signature, file)) {
            tryFFMPEG(file, signature, forGPU) { it, e ->
                data.value = it
                e?.printStackTrace()
            }
        } else {
            val reader = ImageCache.fileReaders[signature] ?: ImageCache.fileReaders[file.lcExtension]
            if (reader != null) reader(file) { it, e ->
                data.value = it
                e?.printStackTrace()
            }
        }
    }

    fun frameIndex(meta: MediaMetadata): Int {
        return Maths.min(20, (meta.videoFrameCount - 1) / 3)
    }
}