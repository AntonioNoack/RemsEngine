package me.anno.tests.gfx.textures

import me.anno.io.files.FileReference
import me.anno.io.files.inner.InnerFolder
import me.anno.io.files.inner.InnerFolderCache
import me.anno.utils.OS.downloads

fun main() {
    val prefab = InnerFolderCache.readAsFolder(downloads.getChild("Blender 2.glb"), false)!!
    printHierarchy(prefab, 0)
}

fun printHierarchy(file: FileReference, depth: Int) {
    println("  ".repeat(depth) + file.name + ": " + file::class.simpleName)
    when (file) {
        is InnerFolder -> {
            for (child in file.listChildren()) {
                printHierarchy(child, depth + 1)
            }
        }
    }
}