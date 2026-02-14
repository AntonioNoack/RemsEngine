package me.anno.tests.mesh.gltf

import me.anno.Engine
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.AssetImport
import me.anno.utils.OS.desktop

fun main() {
    OfficialExtensions.initForTests()
    val src = desktop.getChild("Excavator.glb")
    val dst = desktop.getChild("Excavator")
    dst.delete()
    dst.tryMkdirs()
    AssetImport.deepCopyImport(dst, listOf(src), null)
    Engine.requestShutdown()
}