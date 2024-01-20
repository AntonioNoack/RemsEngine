package me.anno.tests.assimp

import me.anno.ecs.components.mesh.MeshCache
import me.anno.engine.ECSRegistry
import me.anno.io.files.InvalidRef
import me.anno.io.files.inner.InnerByteFile
import me.anno.io.files.inner.InnerFolder
import me.anno.tests.LOGGER
import me.anno.utils.OS.downloads

fun main() {

    // now all works :)

    ECSRegistry.init()

    for (file in listOf(
        "3d/bunny.obj",
        "3d/azeria/scene.gltf",
        "3d/azeria/azeria.zip/scene.gltf"
    )) {
        val ref = downloads.getChild(file)
        val data = ref.readBytesSync()
        val folder = InnerFolder(InvalidRef)
        val intFile = InnerByteFile(folder, ref.name, data)
        MeshCache[intFile]!!
        LOGGER.info("done :)")
    }
}