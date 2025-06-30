package me.anno.tools

import me.anno.graph.hdb.index.Folder
import me.anno.graph.hdb.index.IndexReader
import me.anno.graph.hdb.index.StorageFile
import me.anno.utils.OS.home

fun main() {
    // read thumbnail cache
    // todo export all entries into a .zip file
    val folder = home.getChild(".cache/RemsEngine/thumbs")
    val indexFile = folder.getChild("index.json")
    val storageFiles = HashMap<Int, StorageFile>()
    val root = Folder("")
    IndexReader(indexFile.inputStreamSync()) { sfIndex ->
        storageFiles.getOrPut(sfIndex) {
            StorageFile(sfIndex)
        }
    }.readFolder(root)
    printHierarchy(root, 0, "")
}

fun printHierarchy(folder: Folder, depth: Int, fullName: String) {
    if (folder.files.isEmpty() && folder.children.size == 1) {
        val child = folder.children.values.first()
        printHierarchy(child, depth, "$fullName/${child.name}")
        return
    }
    println("  ".repeat(depth) + "$fullName (${folder.storageFile?.index})")
    for (child in folder.children.values.sortedBy { it.name }) {
        printHierarchy(child, depth + 1, child.name)
    }
    folder.files.forEach { key, file ->
        val key1 = key.toUInt().toString(16).padStart(4, '0')
        println("  ".repeat(depth + 1) + "$key1: ${file.range}")
    }
}