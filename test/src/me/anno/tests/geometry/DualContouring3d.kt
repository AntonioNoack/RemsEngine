package me.anno.tests.geometry

import me.anno.ecs.components.mesh.Mesh
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.maths.geometry.DualContouring3d
import me.anno.utils.pooling.JomlPools
import me.anno.utils.structures.arrays.IntArrayList
import org.joml.Vector3f

fun main() {

    val sx = 64
    val sy = 64
    val sz = 64

    val offset = Vector3f(0.5f)

    val s = 0.02f
    val comp = dualContouringTest()
    val seeds = IntArrayList(8)
    val func = DualContouring3d.Func3d { xi, yi, zi ->
        val pos = JomlPools.vec4f.create()
        val x = (xi - sx / 2) * s - offset.x
        val y = (yi - sy / 2) * s - offset.y
        val z = (zi - sz / 2) * s - offset.z
        val value = comp.computeSDF(pos.set(x, y, z, 0f), seeds)
        JomlPools.vec4f.sub(1)
        value
    }

    val grad = DualContouring3d.gradient(func)
    val edges = DualContouring3d.contour3d(sx, sy, sz, func, grad)
    val positions = FloatArray(edges.size * 3)
    val normals = FloatArray(edges.size * 3)
    var i = 0
    val n = Vector3f()
    for (point in edges) {
        grad.calc(point.x, point.y, point.z, n)
        n.normalize()
        normals[i] = n.x
        positions[i++] = point.x
        normals[i] = n.y
        positions[i++] = point.y
        normals[i] = n.z
        positions[i++] = point.z
    }
    val indices = IntArray(edges.size / 4 * 6)
    i = 0
    for (j in edges.indices step 4) {
        indices[i++] = j
        indices[i++] = j + 1
        indices[i++] = j + 3
        indices[i++] = j
        indices[i++] = j + 3
        indices[i++] = j + 2
    }
    val mesh = Mesh()
    mesh.positions = positions
    mesh.normals = normals
    mesh.indices = indices
    // mesh.calculateNormals(false)
    testSceneWithUI("DualContouring3d", mesh)
}