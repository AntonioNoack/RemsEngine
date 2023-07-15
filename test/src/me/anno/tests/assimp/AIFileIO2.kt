package me.anno.tests.assimp

import me.anno.engine.ECSRegistry
import me.anno.io.files.FileReference.Companion.getReference
import me.anno.io.files.InvalidRef
import me.anno.io.zip.InnerByteFile
import me.anno.io.zip.InnerFolder
import me.anno.mesh.assimp.StaticMeshesLoader
import me.anno.utils.LOGGER
import me.anno.utils.OS.downloads

fun main() {

    // now all works :)

    ECSRegistry.init()

    for (file in listOf(
        "3d/bunny.obj",
        "3d/azeria/scene.gltf",
        "3d/azeria/azeria.zip/scene.gltf"
    )) {
        val ref = getReference(downloads, file)
        val data = ref.readBytesSync()
        val folder = InnerFolder(InvalidRef)
        val intFile = InnerByteFile(folder, ref.name, data)
        StaticMeshesLoader.read(intFile, intFile.getParent())
        LOGGER.info("done :)")
    }

}