package me.anno.tests.files

import me.anno.io.files.thumbs.Thumbs
import me.anno.mesh.blender.BlenderReader
import me.anno.utils.OS

fun main() {
    // time for debugger to attach
    // for (i in 0 until 100) Thread.sleep(100)
    // val ref = getReference(documents, "Blender/Bedroom.blend")
    val ref = OS.documents.getChild("Blender/MaterialTest-2.blend")
    @Suppress("SpellCheckingInspection")
    // val ref = getReference("E:/Documents/Blender/Aerial Aircraft Carrier (CVNA-82)II.blend")
    Thumbs.testGeneration(ref, BlenderReader::readAsFolder)
}