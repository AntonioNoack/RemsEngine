package me.anno.bugs.done

import me.anno.config.DefaultConfig.style
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.material.Material
import me.anno.engine.DefaultAssets.flatCube
import me.anno.ui.debug.TestEngine.Companion.testUI3
import me.anno.ui.utils.ThumbnailPanel

/**
 * fixed bug, where thumbnail of MeshComponent wouldn't render, if a custom material was specified
 * */
fun main() {
    val mat = Material.metallic(0xffdd33, 0f)
    val ref = MeshComponent(flatCube, mat).ref
    testUI3("Thumbnail Of MeshComponent", ThumbnailPanel(ref, style))
}