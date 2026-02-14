package me.anno.tests.mesh

import me.anno.Engine
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.terrain.DefaultNormalMap
import me.anno.ecs.components.mesh.terrain.HeightMap
import me.anno.ecs.components.mesh.terrain.RectangleTerrainModel
import me.anno.engine.raycast.RaycastMesh
import me.anno.image.ImageWriter
import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.sq
import me.anno.maths.noise.PerlinNoise
import me.anno.tests.mesh.simplification.simplifyMesh
import me.anno.utils.assertions.assertLessThan
import me.anno.utils.assertions.assertTrue
import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.Vector4f
import org.junit.jupiter.api.Test
import kotlin.math.abs
import kotlin.math.sqrt

class MeshSimplificationTest {

    val numPointsX = 20
    val numPointsZ = 20

    val s = 15f
    val noise = PerlinNoise(2145, 8, 0.5f, -s, s, Vector4f(0.03f))
    val heightMap = HeightMap { x, y -> noise.getSmooth(x.toFloat(), y.toFloat()) }

    fun createTerrainMesh(): Mesh {
        val normalMap = DefaultNormalMap(heightMap, 1f, false)
        return RectangleTerrainModel.generateRegularQuadHeightMesh(
            numPointsX, numPointsZ, false, 1f,
            Mesh(), heightMap, normalMap
        )
    }

    // todo validate that the terrain height roughly matches the original
    // todo validate that the number of generated vertices is lower than the original & roughly as expected
    fun compareTerrainHeight(mesh: Mesh): Vector2f {
        var avg = 0f
        var sq = 0f

        val start = Vector3f(0f, -s, 0f)
        val dir = Vector3f(0f, 1f, 0f)

        val expected1 = FloatArray(numPointsX * numPointsZ)
        val actual1 = FloatArray(numPointsX * numPointsZ)
        val error1 = FloatArray(numPointsX * numPointsZ)

        var invalidHits = 0
        for (zi in 0 until numPointsZ) {
            for (xi in 0 until numPointsX) {
                val expected = heightMap[xi, zi]

                start.x = (xi - (numPointsX - 1.0001f) * 0.5f)
                start.z = (zi - (numPointsZ - 1.0001f) * 0.5f)

                // use raycast for lookup
                val actual = RaycastMesh.raycastLocalMesh(
                    mesh, start, dir, 1e3f, -1, null, true
                ) + start.y

                if (actual.isInfinite()) {
                    invalidHits++
                    continue
                }

                val index = xi + zi * numPointsX
                expected1[index] = expected
                actual1[index] = actual

                val error = clamp(expected - actual, -s, s)
                error1[index] = error

                avg += error
                sq += sq(error)
            }
        }

        if (false) {
            ImageWriter.writeImageFloat(numPointsX, numPointsZ, "actual.png", true, actual1.copyOf())
            ImageWriter.writeImageFloat(numPointsX, numPointsZ, "expected.png", true, expected1.copyOf())
            ImageWriter.writeImageFloat(numPointsX, numPointsZ, "error.png", true, error1.copyOf())
            println(error1.toList().sorted())
        }

        val n = numPointsX * numPointsZ
        assertTrue(invalidHits * 3 < n) { "Too many invalid hits, $invalidHits vs $n" }
        return Vector2f(avg / n, sqrt(abs(avg * avg - sq)) / n)
    }

    @Test
    fun testSimplification() {
        Engine.cancelShutdown() // to ensure cache works for raycasting

        val mesh0 = createTerrainMesh()
        val mesh1 = simplifyMesh(mesh0, 0.2f, 5)
        val mesh2 = simplifyMesh(mesh1, 0.05f, 5)

        assertTrue(mesh0.numPrimitives > mesh1.numPrimitives)
        assertTrue(mesh1.numPrimitives > mesh2.numPrimitives)

        val err0 = compareTerrainHeight(mesh0)
        val err1 = compareTerrainHeight(mesh1)
        val err2 = compareTerrainHeight(mesh2)
        assertLessThan(abs(err0.x), 1e-5f)
        assertLessThan(abs(err1.x), 0.01f)
        assertLessThan(abs(err2.x), 0.01f)
    }
}