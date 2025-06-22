package me.anno.bench

import me.anno.Engine
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshCache
import me.anno.ecs.components.mesh.MeshIterators.forEachPointIndex
import me.anno.ecs.components.mesh.MeshIterators.forEachTriangleIndex
import me.anno.ecs.components.mesh.utils.IndexGenerator.generateIndices
import me.anno.engine.OfficialExtensions
import me.anno.utils.Clock
import me.anno.utils.OS.downloads
import me.anno.utils.OS.res
import me.anno.utils.types.Strings.upperSnakeCaseToTitle
import me.sp4cerat.fqms.FastQuadraticMeshSimplification
import me.sp4cerat.fqms.Triangle
import me.sp4cerat.fqms.Vertex
import org.apache.logging.log4j.LogManager

private val LOGGER = LogManager.getLogger("MeshSimplification")

fun main() {
    OfficialExtensions.initForTests() // Mesh loaders
    val clock = Clock(LOGGER)
    var easy = true
    val mesh = MeshCache.getEntry(
        if (easy) res.getChild("meshes/CuteGhost.fbx")
        else downloads.getChild("3d/dragon.obj")
    ).waitFor() as Mesh
    if (mesh.indices == null) {
        // todo bug/issue: generateIndicesV2 is very slow
        mesh.generateIndices()
    }
    LOGGER.info("Base Mesh: ${mesh.numPrimitives} ${mesh.drawMode.name.upperSnakeCaseToTitle()}s")
    var firstRun = true
    clock.benchmark(if (easy) 5 else 0, if (easy) 250 else 3, "Simplification") {
        val helper = FastQuadraticMeshSimplification()
        val positions = mesh.positions!!
        mesh.forEachPointIndex(false) { i ->
            val vertex = Vertex()
            vertex.position.set(positions, i * 3)
            helper.vertices.add(vertex)
            false
        }
        mesh.forEachTriangleIndex { ai, bi, ci ->
            val triangle = Triangle()
            triangle.vertexIds[0] = ai
            triangle.vertexIds[1] = bi
            triangle.vertexIds[2] = ci
            helper.triangles.add(triangle)
            false
        }
        helper.simplifyMesh((helper.vertices.size * 0.1f).toInt())
        if (firstRun) {
            LOGGER.info("Simplified to ${helper.triangles.size} triangles, ${helper.vertices.size} vertices")
            firstRun = false
        }
    }
    Engine.requestShutdown()
}