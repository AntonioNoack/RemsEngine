package me.anno.tests.geometry

import me.anno.ecs.components.mesh.Mesh
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.maths.geometry.MarchingCubes
import org.joml.AABBf

fun main() {

    val sxy = sx * sy
    val values = FloatArray(sx * sy * sz) { i1 ->
        val i2 = i1 % sxy
        val xi = i2 % sx
        val yi = i2 / sx
        val zi = i1 / sxy
        sample(xi.toFloat(), yi.toFloat(), zi.toFloat())
    }

    val mesh = Mesh()
    val points = MarchingCubes.march(
        sx, sy, sz, values, 0f,
        AABBf(0f, 0f, 0f, sx - 1f, sy - 1f, sz - 1f),
        false
    )
    mesh.positions = points.toFloatArray()
    // - normals can be calculated using the field to get better results,
    //   however we're using a random field, so we don't really have a field
    // - both true and false can be tried here
    mesh.calculateNormals(smooth = true)
    testSceneWithUI("MarchingCubes", mesh)
}