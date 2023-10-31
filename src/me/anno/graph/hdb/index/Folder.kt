package me.anno.graph.hdb.index

import java.util.concurrent.ConcurrentHashMap

class Folder(var name: String) {

    val files = ConcurrentHashMap<Long, File>()
    val children = ConcurrentHashMap<String, Folder>()
    var storageFile: StorageFile? = null

    override fun toString(): String {
        return "Folder@${System.identityHashCode(this)}($name) { files: $files, children: $children, sf: $storageFile }"
    }

}