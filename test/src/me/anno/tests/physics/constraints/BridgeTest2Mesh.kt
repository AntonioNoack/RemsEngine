package me.anno.tests.physics.constraints

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.material.Material
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.maths.Maths.posMod
import me.anno.mesh.Triangulation
import me.anno.utils.assertions.assertEquals
import me.anno.utils.structures.arrays.FloatArrayList
import me.anno.utils.structures.arrays.FloatArrayListUtils.add
import me.anno.utils.structures.lists.Lists.wrap
import me.anno.utils.types.Booleans.hasFlag
import org.joml.Vector2f
import org.joml.Vector3d
import kotlin.math.cosh
import kotlin.math.sinh

/**
 * create meshes for a bow bridge
 * */
fun main() {
    val scene = Entity()
    val meshes = createBridgeMeshes(15, 0.2f, 0.1f, 0.01f)
    for ((mesh, pos) in meshes) {
        Entity(scene)
            .setPosition(pos)
            .add(MeshComponent(mesh))
    }
    testSceneWithUI("Bridge", scene)
}

val cosh1 = cosh(1f)

fun getBridgePoint(i: Float, dy: Float, n: Int): Vector2f {
    val x = i * 2f / n - 1f
    val dx = dy * sinh(x)
    return Vector2f(x + dx, dy - cosh(x) + cosh1)
}

fun getBridgePoint(i: Int, dy: Float, n: Int): Vector2f {
    return getBridgePoint(i.toFloat(), dy, n)
}

fun createBridgeMeshes(n: Int, dx: Float, dy: Float, dxHalfOffset: Float): Array<Pair<Mesh, Vector3d>> {
    val m0 = Material.diffuse(0x808080)
    val m1 = Material.diffuse(0x707070)
    return Array(n) { i ->
        val dxI = if (i.hasFlag(1)) -dxHalfOffset else dxHalfOffset
        val center = getBridgePoint(i + 0.5f, dy * 0.5f, n)
        val mesh = extrudePolygonToMesh(
            listOf(
                getBridgePoint(i, 0f, n),
                getBridgePoint(i + 1, 0f, n),
                getBridgePoint(i + 1, dy, n),
                getBridgePoint(i, dy, n),
            ).onEach { it.sub(center) }, -dx + dxI, dx + dxI
        )
        mesh.materials = (if (i.hasFlag(1)) m0 else m1).ref.wrap()
        val pos = Vector3d(0f, center.y, center.x)
        mesh to pos
    }
}

fun extrudePolygonToMesh(polygon: List<Vector2f>, x0: Float, x1: Float): Mesh {

    val mesh = Mesh()
    val outerSide = Triangulation.ringToTrianglesVec2f(polygon)

    val posSize = (outerSide.size * 2 + polygon.size * 6) * 3
    val pos = FloatArrayList(posSize)

    fun addSide(sign: Int) {
        val x = if (sign > 0) x1 else x0
        for (i0 in outerSide.indices) {
            val i = if (sign > 0) outerSide.lastIndex - i0 else i0
            val v = outerSide[i]
            pos.add(x, v.y, v.x)
        }
    }

    addSide(+1)
    addSide(-1)

    for (i in polygon.indices) {
        val j = posMod(i + 1, polygon.size)
        val v0 = polygon[i]
        val v1 = polygon[j]
        pos.add(x0, v0.y, v0.x)
        pos.add(x1, v0.y, v0.x)
        pos.add(x1, v1.y, v1.x)

        pos.add(x0, v0.y, v0.x)
        pos.add(x1, v1.y, v1.x)
        pos.add(x0, v1.y, v1.x)
    }

    assertEquals(posSize, pos.size)

    mesh.positions = pos.toFloatArray(true)
    mesh.calculateNormals(false)

    return mesh
}