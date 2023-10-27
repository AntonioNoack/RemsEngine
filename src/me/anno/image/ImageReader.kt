package me.anno.image

import me.anno.image.raw.BGRAImage
import me.anno.image.raw.ComponentImage
import me.anno.image.raw.GrayscaleImage
import me.anno.image.raw.OpaqueImage
import me.anno.io.files.FileReference
import me.anno.io.files.Signature
import me.anno.io.zip.InnerFolder
import net.sf.image4j.codec.ico.ICOReader

/**
 * an easy interface to read any image as rgba and individual channels
 * */
object ImageReader {

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
        createComponent(file, folder, "bgra.png") { BGRAImage(it) }

        // inverted components
        createComponent(file, folder, "1-r.png", "r", true)
        createComponent(file, folder, "1-g.png", "g", true)
        createComponent(file, folder, "1-b.png", "b", true)
        createComponent(file, folder, "1-a.png", "a", true)

        // grayscale, if not only a single channel
        createComponent(file, folder, "grayscale.png") { GrayscaleImage(it) }

        // rgb without alpha, if alpha exists
        createComponent(file, folder, "rgb.png") { OpaqueImage(it) }

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
            val src = ImageCPUCache[file, false]!!
            createImage(src)
        })
    }

    @JvmStatic
    private fun createComponent(
        file: FileReference, folder: InnerFolder, name: String,
        swizzle: String, inverse: Boolean = false
    ) {
        when (swizzle.length) {
            1 -> createComponent(file, folder, name) {
                ComponentImage(it, inverse, swizzle[0])
            }
            else -> throw NotImplementedError(swizzle)
        }
    }
}