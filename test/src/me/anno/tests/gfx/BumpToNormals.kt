package me.anno.tests.gfx

import me.anno.image.ImageCache
import me.anno.image.raw.IntImage
import me.anno.io.files.FileReference
import me.anno.utils.Color.rgba
import me.anno.utils.OS.downloads
import javax.vecmath.Vector3f

fun main() {
    val folder = downloads.getChild("ogldev-source/crytek_sponza/textures")
    for (file in folder.listChildren()) {
        if (file.nameWithoutExtension.endsWith("bump", true)) {
            var newName = file.nameWithoutExtension
            newName = newName.substring(0, newName.length - 4) + "normal." + file.extension
            val dstFile = folder.getChild(newName)
            if (!dstFile.exists) {
                convert(file, dstFile)
            }
        }
    }
}

fun convert(src: FileReference, dst: FileReference) {

    val image = ImageCache[src, false]!!
    fun get(x: Int, y: Int): Int {
        val xi = (x + image.width) % image.width
        val yi = (y + image.height) % image.height
        return image.getRGB(xi, yi) and 0xff // is grayscale -> all channels have the same value
    }

    val dstImage = IntImage(image.width, image.height, false)
    val maxExpectedGradient = 16f
    val v = Vector3f()
    for (y in 0 until image.height) {
        for (x in 0 until image.width) {
            val dx = (get(x + 1, y) - get(x - 1, y)) * 0.5f
            val dy = (get(x, y + 1) - get(x, y - 1)) * 0.5f
            v.set(dx, dy, maxExpectedGradient)
            v.normalize()
            v.scale(0.5f)
            val color = rgba(v.x + 0.5f, v.y + 0.5f, v.z + 0.5f, 1f)
            dstImage.setRGB(x, y, color)
        }
    }

    dstImage.write(dst)

}
