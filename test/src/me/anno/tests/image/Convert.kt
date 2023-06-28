package me.anno.tests.image

import me.anno.image.ImageCPUCache
import me.anno.io.files.FileReference.Companion.getReference
import me.anno.utils.OS.desktop

fun main() {
    // convertAllPNGToJPG()
    scaleDownImages()
}

/**
 * Convert all png files to jpg in a folder.
 * */
fun convertAllPNGToJPG() {
    val src = getReference("E:/Assets/Sponza/textures")
    val dst = src.getChild("jpg")
    dst.tryMkdirs()
    for (child in src.listChildren()!!) {
        if (child.lcExtension == "png") {
            val jpgPath = dst.getChild(child.nameWithoutExtension + ".jpg")
            ImageCPUCache[child, 0L, false]!!
                .write(jpgPath)
            jpgPath.renameTo(dst.getChild(child.name)) // rename to PNG, so I can still use the mesh files
            ImageCPUCache.removeFileEntry(child) // free memory
        }
    }
}

/**
 * Scale down all images in a folder.
 * */
fun scaleDownImages() {
    val src = desktop.getChild("Berlin")
    val dst = src.getChild("small")
    dst.tryMkdirs()
    for (child in src.listChildren()!!) {
        if (child.isDirectory) continue
        val image = ImageCPUCache[child, false] ?: continue
        image
            .resized(image.width / 3, image.height / 3, false)
            .write(dst.getChild(child.name))
    }
}