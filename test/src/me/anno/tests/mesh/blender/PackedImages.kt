package me.anno.tests.mesh.blender

import me.anno.mesh.blender.BlenderReader
import me.anno.tests.files.printTree
import me.anno.utils.OS.documents

fun main() {
    BlenderReader.readAsFolder(documents.getChild("Blender/PackSample.blend")) { folder, exc ->
        exc?.printStackTrace()
        folder?.printTree(1, 10)
    }
}