package me.anno.image

import me.anno.image.raw.BGRAImage
import me.anno.image.raw.ComponentImage
import me.anno.image.raw.GrayscaleImage
import me.anno.image.raw.OpaqueImage
import me.anno.io.files.FileReference
import me.anno.io.files.Signature
import me.anno.io.zip.InnerFolder
import net.sf.image4j.codec.ico.ICOReader
import java.io.IOException

/**
 * an easy interface to read any image as rgba and individual channels
 * */
object ImageReader {

    @JvmStatic
    fun readAsFolder(file: FileReference, callback: (InnerFolder?, Exception?) -> Unit) {

        // todo white with transparency, black with transparency
        // (overriding color)

        val folder = InnerFolder(file)

        // add the most common swizzles: r,g,b,a
        // to do maybe bgra or similar in the future
        val image = ImageCPUCache[file, false] ?: throw IOException("Could not read $file as image")
        val hasG = image.numChannels > 1
        val hasB = image.numChannels > 2
        val hasA = image.hasAlphaChannel

        // normal components
        createComponent(image, folder, "r.png", "r", false)
        if (hasG) createComponent(image, folder, "g.png", "g", false)
        if (hasB) createComponent(image, folder, "b.png", "b", false)
        if (hasA) createComponent(image, folder, "a.png", "a", false)

        // bgra
        folder.createImageChild("bgra.png", BGRAImage(image))

        // inverted components
        createComponent(image, folder, "1-r.png", "r", true)
        if (hasG) createComponent(image, folder, "1-g.png", "g", true)
        if (hasB) createComponent(image, folder, "1-b.png", "b", true)
        if (hasA) createComponent(image, folder, "1-a.png", "a", true)

        // grayscale, if not only a single channel
        if (hasG) createComponent(folder, "grayscale.png", GrayscaleImage(image))

        // rgb without alpha, if alpha exists
        if (hasA) createComponent(folder, "rgb1.png", OpaqueImage(image))

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
    private fun createComponent(folder: InnerFolder, name: String, image: Image) {
        folder.createImageChild(name, image)
    }

    @JvmStatic
    private fun createComponent(
        image: Image, folder: InnerFolder, name: String,
        swizzle: String, inverse: Boolean = false
    ) {
        folder.createImageChild(
            name, when (swizzle.length) {
                1 -> ComponentImage(image, inverse, swizzle[0])
                else -> throw RuntimeException("Not yet implemented")
            }
        )
    }

}