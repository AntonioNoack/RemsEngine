package me.anno.tests.recast

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.engine.DefaultAssets
import me.anno.maths.Maths.PIf
import me.anno.maths.Maths.sq
import me.anno.maths.geometry.polygon.PolygonArea.getPolygonAreaVector3f
import me.anno.recast.NavMeshBuilder
import me.anno.recast.NavMeshData
import me.anno.recast.NavMeshDebugComponent
import me.anno.utils.types.Floats.formatPercent
import org.joml.Vector3f
import org.junit.jupiter.api.Test
import org.recast4j.detour.MeshData

class FlatRecastTest {

    companion object {
        fun calculateTotalArea(mesh: MeshData): Float {
            val tmp = Vector3f()
            val allVertices = List(mesh.vertCount) {
                Vector3f(mesh.vertices, it * 3)
            }
            return (0 until mesh.polyCount).sumOf { pi ->
                val polygon = mesh.polygons[pi]
                val vertices = List(polygon.numVertices) { vi ->
                    allVertices[polygon.vertices[vi]]
                }
                vertices.getPolygonAreaVector3f(tmp)
                    .length().toDouble()
            }.toFloat()
        }

        val scale = 10f

        fun createRecastScene(mesh: Mesh): Pair<Entity, NavMeshData> {
            val scene = Entity()
            Entity("Floor", scene)
                .add(MeshComponent(mesh))
                .setScale(scale)

            // why is the nav mesh only two triangles???
            //   because our logic didn't support TRIANGLE_STRIP yet, and plane uses that
            val builder = NavMeshBuilder()
            builder.cellSize = 0.2f
            builder.cellHeight = 0.3f
            builder.agentType.height = 1f
            builder.agentType.radius = 0.2f
            builder.agentType.maxStepHeight = 0.5f
            builder.collisionMask = 1
            builder.agentType.maxSpeed = 3f
            builder.agentType.maxAcceleration = 10f

            val navMeshData = builder.buildData(scene)!!
            scene.add(NavMeshDebugComponent(navMeshData))
            return scene to navMeshData
        }
    }

    @Test
    fun testPlaneProducesFullArea() {
        val (_, navMeshData) = createRecastScene(DefaultAssets.plane)

        val expectedArea = sq(scale * 2f)
        val actualArea = calculateTotalArea(navMeshData.meshData)

        println("Found area: ${(actualArea / expectedArea).formatPercent()}")
        check(actualArea >= expectedArea * 0.9f)
    }

    @Test
    fun testCircleProducesFullArea() {
        val (_, navMeshData) = createRecastScene(DefaultAssets.circle)

        val expectedArea = sq(scale) * PIf
        val actualArea = calculateTotalArea(navMeshData.meshData)

        println("Found area: ${(actualArea / expectedArea).formatPercent()}")
        check(actualArea >= expectedArea * 0.9f)
    }
}