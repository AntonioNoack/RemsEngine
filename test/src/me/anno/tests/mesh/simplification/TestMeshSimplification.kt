package me.anno.tests.mesh.simplification

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.MeshIterators.forEachTriangleIndex
import me.anno.ecs.components.mesh.terrain.DefaultNormalMap
import me.anno.ecs.components.mesh.terrain.HeightMap
import me.anno.ecs.components.mesh.terrain.RectangleTerrainModel
import me.anno.ecs.components.mesh.utils.IndexGenerator.generateIndices
import me.anno.engine.debug.DebugPoint
import me.anno.engine.debug.DebugShapes.showDebugPoint
import me.anno.engine.ui.render.RenderMode
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.maths.noise.PerlinNoise
import me.anno.ui.UIColors
import me.anno.utils.algorithms.ForLoop.forLoopSafely
import me.sp4cerat.fqms.FastQuadraticMeshSimplification
import me.sp4cerat.fqms.Triangle
import me.sp4cerat.fqms.Vertex
import org.joml.Vector4f

fun simplifyMesh(mesh: Mesh, ratio: Float, level: Int): Mesh {

    if (mesh.indices == null) {
        // algorithm doesn't work without indices
        mesh.generateIndices()
    }

    // add all vertices and triangles
    val positions = mesh.positions!!
    val helper = FastQuadraticMeshSimplification()
    forLoopSafely(positions.size, 3) { idx ->
        val v = Vertex()
        v.position.set(positions, idx)
        helper.vertices.add(v)
    }
    mesh.forEachTriangleIndex { ai, bi, ci ->
        val t = Triangle()
        t.vertexIds[0] = ai
        t.vertexIds[1] = bi
        t.vertexIds[2] = ci
        helper.triangles.add(t)
        false
    }

    helper.preserveBorder = true
    helper.simplifyMesh((ratio * mesh.numPrimitives).toInt(), level)

    val showBorders = mesh.numPrimitives < 1000
    val newPositions = FloatArray(helper.vertices.size * 3)
    for (i in helper.vertices.indices) {
        val v = helper.vertices[i]
        v.position.get(newPositions, i * 3)
        if (v.border && showBorders) {
            showDebugPoint(DebugPoint(v.position, UIColors.fireBrick, 1e3f))
        }
    }

    val newIndices = IntArray(helper.triangles.size * 3)
    for (i in helper.triangles.indices) {
        val t = helper.triangles[i]
        newIndices[i * 3] = t.vertexIds[0]
        newIndices[i * 3 + 1] = t.vertexIds[1]
        newIndices[i * 3 + 2] = t.vertexIds[2]
    }

    val clone = mesh.clone() as Mesh
    clone.unlinkPrefab()
    clone.invalidateGeometry()
    clone.positions = newPositions
    clone.indices = newIndices
    clone.normals = null
    clone.tangents = null
    clone.uvs = null

    return clone
}

fun createTestMesh(w: Int, h: Int): Mesh {
    val s = -15f
    val noise = PerlinNoise(2145, 8, 0.5f, -s, s, Vector4f(0.03f))
    val heightMap = HeightMap { x, y -> noise.getSmooth(x.toFloat(), y.toFloat()) }
    val normalMap = DefaultNormalMap(heightMap, 1f, false)
    return RectangleTerrainModel.generateRegularQuadHeightMesh(
        w, h, false, 1f,
        Mesh(), heightMap, normalMap
    )
}

fun main() {

    val mesh0 = createTestMesh(20, 20)
    val mesh1 = simplifyMesh(mesh0, 0.2f, 5)
    val mesh2 = simplifyMesh(mesh1, 0.05f, 5)

    val scene = Entity()
    Entity(scene)
        .add(MeshComponent(mesh1))

    Entity(scene)
        .add(MeshComponent(mesh2))
        .setPosition(-mesh0.getBounds().deltaX * 1.5, 0.0, 0.0)

    Entity(scene)
        .add(MeshComponent(mesh0))
        .setPosition(mesh0.getBounds().deltaX * 1.5, 0.0, 0.0)

    testSceneWithUI("Mesh Simplification", scene, RenderMode.LINES_MSAA)
}