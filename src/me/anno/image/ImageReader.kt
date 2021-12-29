package me.anno.image

import me.anno.image.raw.GrayscaleImage
import me.anno.image.raw.CachedImage
import me.anno.image.raw.OpaqueImage
import me.anno.io.files.FileReference
import me.anno.io.zip.InnerFolder
import java.io.IOException

object ImageReader {

    // todo the color of the fox switches...
    // todo display monochrome textures as monochrome
    // todo images must not be kept in memory forever

    // easy interface to read any image as rgba and individual channels

    fun readAsFolder(file: FileReference): InnerFolder {
        val folder = InnerFolder(file)
        // add the most common swizzles: r,g,b,a
        // todo maybe bgra or similar in the future
        val image = ImageCPUCache.getImage(file, false) ?: throw IOException("Could not read $file as image")
        val hasG = image.numChannels > 1
        val hasB = image.numChannels > 2
        val hasA = image.hasAlphaChannel
        val ori = CachedImage(file, image)

        // normal components
        createComponent(ori, folder, "r.png", "r", false)
        if (hasG) createComponent(ori, folder, "g.png", "g", false)
        if (hasB) createComponent(ori, folder, "b.png", "b", false)
        if (hasA) createComponent(ori, folder, "a.png", "a", false)

        // inverted components
        createComponent(ori, folder, "1-r.png", "r", true)
        if (hasG) createComponent(ori, folder, "1-g.png", "g", true)
        if (hasB) createComponent(ori, folder, "1-b.png", "b", true)
        if (hasA) createComponent(ori, folder, "1-a.png", "a", true)

        // grayscale, if not only a single channel
        if (hasG) createComponent(folder, "grayscale.png", GrayscaleImage(ori))

        // rgb without alpha, if alpha exists
        if (hasA) createComponent(folder, "rgb1.png", OpaqueImage(ori))

        return folder
    }

    private fun createComponent(folder: InnerFolder, name: String, image: Image) {
        folder.createImageChild(name, image)
    }

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