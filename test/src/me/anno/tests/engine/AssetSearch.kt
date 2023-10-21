package me.anno.tests.engine

import me.anno.io.files.FileReference
import me.anno.io.files.FileReference.Companion.getReference
import me.anno.io.zip.InnerFolderCache
import me.anno.utils.hpc.ProcessingGroup
import me.anno.utils.structures.lists.Lists.any2

// only go into folders, and unity / mesh files
val allowedExtensions = listOf(
    "unitypackage", "zip", "tar", "rar"
)

val searchTerms = listOf(
    "duck", "boat", "ship"
)

val processingGroup = ProcessingGroup("AssetSearch", 16)

fun main() {
    // list all duck meshes in my asset database
    InnerFolderCache.timeoutMillis = 250
    search(getReference("E:/Assets/Unity"), 10, 0)
    processingGroup.waitUntilDone(false)
}

fun search(file: FileReference, maxDepth: Int, zipDepth: Int) {
    if (searchTerms.any2 { file.name.contains(it, true) }) {
        println("Found file: $file")
    } else if (maxDepth > 0) {
        if (file.isDirectory) {
            processingGroup += {
                println("Looking into $file")
                for (child in file.listChildren()!!) {
                    search(child, maxDepth - 1, zipDepth)
                }
            }
        } else if (file.lcExtension in allowedExtensions) {
            processingGroup += {
                println("Looking into $file")
                try {
                    val children = try {
                        file.listChildren() ?: emptyList()
                    } catch (e: Exception) {
                        emptyList()
                    }
                    for (i in children.indices) {
                        search(children[i], maxDepth - 1, zipDepth + 1)
                    }
                    if (zipDepth == 0) {
                        InnerFolderCache.clear()
                    }
                } catch (e: Exception) {
                    println("Error happened in $file")
                    throw e
                }
            }
        }
    }
}