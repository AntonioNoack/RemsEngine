package me.anno.tests.files

import me.anno.io.files.FileReference
import me.anno.io.files.FileReference.Companion.getReference
import me.anno.tests.LOGGER

fun main() {
    rename(getReference("C:\\Users\\Antonio\\AndroidStudioProjects\\Calculator\\app\\src\\main\\cpp"))
}

fun rename(folder: FileReference) {
    for (child in folder.listChildren()!!) {
        if (child.isDirectory) {
            rename(child)
        } else {
            val ext = when (child.lcExtension) {
                "c", "cc", "h", "hpp" -> "cpp"
                else -> continue
            }
            val newFile = child.getSibling(child.nameWithoutExtension + "." + ext)
            if (!newFile.exists) {
                child.renameTo(newFile)
            } else LOGGER.warn("$newFile already exists for $child")
        }
    }
}