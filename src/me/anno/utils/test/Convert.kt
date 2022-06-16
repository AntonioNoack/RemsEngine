package me.anno.utils.test

import me.anno.image.ImageCPUCache
import me.anno.io.files.FileReference.Companion.getReference

fun main() {
    val src = getReference("E:/Assets/Sponza/textures")
    val dst = src.getChild("jpg")
    dst.tryMkdirs()
    for (child in src.listChildren()!!) {
        if (child.lcExtension == "png") {
            val jpgPath = dst.getChild(child.nameWithoutExtension + ".jpg")
            ImageCPUCache.getImage(child, 0L, false)!!
                .write(jpgPath)
            jpgPath.renameTo(dst.getChild(child.name)) // rename to PNG, so I can still use the mesh files
            ImageCPUCache.removeFileEntry(child) // free memory
        }
    }
}