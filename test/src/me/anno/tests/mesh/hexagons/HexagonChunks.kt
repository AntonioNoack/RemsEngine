package me.anno.tests.mesh.hexagons

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.components.mesh.utils.SimpleMeshJoiner
import me.anno.engine.debug.DebugShapes.debugTexts
import me.anno.engine.debug.DebugText
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.gpu.CullMode
import me.anno.gpu.buffer.DrawMode
import me.anno.maths.Maths
import me.anno.maths.chunks.spherical.Hexagon
import me.anno.maths.chunks.spherical.HexagonSphere
import me.anno.utils.Color.black
import me.anno.utils.Color.toVecRGBA
import me.anno.utils.types.Arrays.resize
import org.joml.Vector3d

fun createMesh(color: Int): Mesh {
    val mesh = Mesh()
    val material = Material()
    color.toVecRGBA(material.diffuseBase)
    material.cullMode = CullMode.BOTH
    mesh.material = material.ref
    return mesh
}

fun randomColor(): Int = (Maths.random() * 1e9).toInt() or black

fun chunkToFaceMesh(chunk: List<Hexagon>, color: Int = randomColor()): Mesh {
    val mesh = createMesh(color)
    createFaceMesh(mesh, chunk)
    return mesh
}

fun chunkToLineMesh(chunk: List<Hexagon>, len: Float, color: Int = randomColor()): Mesh {
    val mesh = createMesh(color)
    createConnectionMesh(mesh, chunk, len)
    return mesh
}

fun main() {
    val n = 10
    val s = 2
    val sphere = HexagonSphere(n, s)
    val scene = Entity()
    val faceMeshes = ArrayList<Mesh>()
    val lineMeshes = ArrayList<Mesh>()
    val duration = 1e9f
    for (tri in 0 until 20) {
        for (si in 0 until s) {
            for (sj in 0 until s - si) {
                val chunk = sphere.queryChunk(tri, si, sj)
                debugTexts.add(
                    DebugText(
                        Vector3d(sphere.getChunkCenter(tri, si, sj)),
                        "$tri/$si/$sj", -1, duration
                    )
                )
                val color = randomColor()
                faceMeshes.add(chunkToFaceMesh(chunk, color))
                lineMeshes.add(chunkToLineMesh(chunk, sphere.len, color xor 0x404040))
            }
        }
    }
    val joiner = SimpleMeshJoiner(true, true, false, false)
    scene.add(MeshComponent(joiner.join(faceMeshes)))
    scene.add(MeshComponent(joiner.join(lineMeshes)))
    testSceneWithUI("Hexagon Chunks", scene)
}

fun createHexSphere(n: Int): Pair<ArrayList<Hexagon>, Float> {
    val sphere = HexagonSphere(n, 1)
    val all = ArrayList<Hexagon>(sphere.numHexagons.toInt())
    for (tri in 0 until 20) all.addAll(sphere.queryChunk(tri, 0, 0))
    return all to sphere.len
}

/**
 * creates a triangulated surface for a hexagon mesh;
 * each line is currently duplicated...
 * */
fun createLineMesh(mesh: Mesh, hexagons: List<Hexagon>) {
    var pi = 0
    var li = 0
    val positions = mesh.positions.resize(6 * 3 * hexagons.size)
    val indices = mesh.indices.resize(6 * 2 * hexagons.size)
    mesh.drawMode = DrawMode.LINES
    mesh.positions = positions
    mesh.indices = indices
    mesh.normals = positions
    for (hex in hexagons) {
        var p0 = pi / 3
        var p1 = p0 + hex.corners.size - 1
        for (c in hex.corners) {
            positions[pi++] = c.x
            positions[pi++] = c.y
            positions[pi++] = c.z
            indices[li++] = p0
            indices[li++] = p1
            p1 = p0
            p0++
        }
    }
    mesh.invalidateGeometry()
}

/**
 * creates a triangulated surface for a hexagon mesh
 * */
fun createFaceMesh(
    mesh: Mesh,
    hexagons: List<Hexagon>,
    i0: Int = 0,
    i1: Int = hexagons.size
): Mesh {
    var pi = 0
    var li = 0
    val size = i1 - i0
    val pentagonCount = (i0 until i1).count { hexagons[it].corners.size == 5 }
    val positions = mesh.positions.resize(3 * (6 * size - pentagonCount))
    val indices = mesh.indices.resize(3 * (4 * size - pentagonCount))
    mesh.positions = positions
    mesh.indices = indices
    mesh.normals = positions
    for (i in i0 until i1) {
        val hex = hexagons[i]
        val p0 = pi / 3
        var p1 = p0 + 1
        for (c in hex.corners) {
            positions[pi++] = c.x
            positions[pi++] = c.y
            positions[pi++] = c.z
        }
        for (j in 2 until hex.corners.size) {
            indices[li++] = p0
            indices[li++] = p1++
            indices[li++] = p1
        }
    }
    mesh.invalidateGeometry()
    return mesh
}
