package me.anno.tests.tools

import me.anno.Engine
import me.anno.ecs.Entity
import me.anno.ecs.prefab.PrefabCache
import me.anno.engine.OfficialExtensions
import me.anno.input.Clipboard.getClipboardContent
import me.anno.io.files.Reference.getReference
import me.anno.mesh.gltf.GLTFWriter
import me.anno.utils.OS.desktop
import me.anno.utils.assertions.assertTrue
import me.anno.utils.async.Callback

fun main() {
    // todo something goes wrong when parsing my FBX Ascii files...
    OfficialExtensions.initForTests()
    val src = getReference(
        when (val file = getClipboardContent()) {
            is List<*> -> file.firstOrNull().toString()
            else -> file.toString()
        }
    )
    assertTrue(src.exists, "$src must be a valid file ")
    val dst = desktop.getChild(src.name).getSiblingWithExtension("glb")
    val scene = PrefabCache[src].waitFor()!!.sample as Entity
    GLTFWriter().write(scene, dst, Callback.finish { Engine.requestShutdown() })
}