package me.anno.tests.gfx.buffer

import me.anno.ecs.components.mesh.shapes.IcosahedronModel
import me.anno.ecs.components.mesh.utils.IndexGenerator.generateIndices
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.gpu.buffer.DrawMode
import me.anno.gpu.buffer.TriangleIndices
import me.anno.utils.assertions.assertEquals

fun main() {
    val mesh = IcosahedronModel.createIcosphere(3)
    if (mesh.indices == null) mesh.generateIndices() // ensure it has indices

    assertEquals(DrawMode.TRIANGLES, mesh.drawMode)

    val oldArray = mesh.indices!!
    val newArray = TriangleIndices.trianglesToTriangleStrip(oldArray).toIntArray()
    mesh.indices = newArray
    mesh.drawMode = DrawMode.TRIANGLE_STRIP
    mesh.invalidateGeometry()

    println("Compressed ${oldArray.size} indices to ${newArray.size}")

    testSceneWithUI("toTriangleStrip", mesh)
}