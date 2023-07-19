package me.anno.tests.gfx

import me.anno.io.files.FileReference.Companion.getReference
import me.anno.mesh.obj.OBJReader
import me.anno.utils.OS.downloads

fun main() {
    // seems like a good guess: :)
    // val defaultSize = Maths.clamp(file.length() / 64, 64, 1 shl 20).toInt()
    val ref = getReference(downloads, "ogldev-source/Content/crytek_sponza/sponza.obj")
    // val ref = getReference(documents, "sphere.obj")
    val input = ref.inputStreamSync()
    val reader = OBJReader(input, ref)
    input.close()
    println("materials: " + reader.materialsFolder.children.size)
    println("meshes: " + reader.meshesFolder.children.size)
}