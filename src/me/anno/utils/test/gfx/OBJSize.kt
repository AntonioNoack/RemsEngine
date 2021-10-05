package me.anno.utils.test.gfx

import me.anno.io.files.FileReference.Companion.getReference
import me.anno.mesh.obj.OBJReader2
import me.anno.utils.OS.downloads

fun main() {

    // seems like a good guess: :)
    // val defaultSize = Maths.clamp(file.length() / 64, 64, 1 shl 20).toInt()
    val ref = getReference(downloads, "ogldev-source/Content/crytek_sponza/sponza.obj")
    // val ref = getReference(documents, "sphere.obj")
    val reader = ref.inputStream().use { OBJReader2(it, ref) }
    reader.printSizes()

}