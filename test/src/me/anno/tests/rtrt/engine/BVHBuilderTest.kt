package me.anno.tests.rtrt.engine

import me.anno.ecs.components.mesh.shapes.IcosahedronModel
import me.anno.maths.bvh.BLASBranch
import me.anno.maths.bvh.BLASLeaf
import me.anno.maths.bvh.BLASNode
import me.anno.maths.bvh.BVHBuilder
import me.anno.maths.bvh.SplitMethod
import me.anno.utils.assertions.assertTrue
import org.joml.AABBf
import org.joml.Vector3f
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class BVHBuilderTest {

    private val mesh = IcosahedronModel.createIcosphere(2)

    private data class Triangle(val v0: Vector3f, val v1: Vector3f, val v2: Vector3f)

    @Test
    fun testAllTrianglesAppearExactlyOnce() {
        val triangles = HashSet<Triangle>()
        mesh.forEachTriangle { a: Vector3f, b: Vector3f, c: Vector3f ->
            triangles.add(Triangle(Vector3f(a), Vector3f(b), Vector3f(c)))
        }
        assertEquals(mesh.numPrimitives.toInt(), triangles.size)
        val blas = BVHBuilder.buildBLAS(mesh, SplitMethod.MEDIAN_APPROX, 8)
        testAllTrianglesAppearExactlyOnce(blas!!, triangles)
        assertEquals(0, triangles.size)
    }

    private fun testAllTrianglesAppearExactlyOnce(blas: BLASNode, triangles: HashSet<Triangle>) {
        when (blas) {
            is BLASBranch -> {
                testAllTrianglesAppearExactlyOnce(blas.n0, triangles)
                testAllTrianglesAppearExactlyOnce(blas.n1, triangles)
            }
            is BLASLeaf -> {
                val geometry = blas.geometry
                val indices = geometry.indices
                val positions = geometry.positions
                for (i in blas.start * 3 until (blas.start + blas.length) * 3 step 3) {
                    val a = Vector3f(positions, indices[i] * 3)
                    val b = Vector3f(positions, indices[i + 1] * 3)
                    val c = Vector3f(positions, indices[i + 2] * 3)
                    assertTrue(triangles.remove(Triangle(a, b, c)))
                }
            }
            else -> throw NotImplementedError()
        }
    }

    @Test
    fun testAllAABBsCorrect() {
        val blas = BVHBuilder.buildBLAS(mesh, SplitMethod.MEDIAN_APPROX, 8)
        testAllAABBsCorrect(blas!!)
    }

    fun testAllAABBsCorrect(blas: BLASNode) {
        when (blas) {
            is BLASBranch -> {
                assertEquals(
                    AABBf(blas.n0.bounds).union(blas.n1.bounds),
                    blas.bounds
                )
                testAllAABBsCorrect(blas.n0)
                testAllAABBsCorrect(blas.n1)
            }
            is BLASLeaf -> {
                val geometry = blas.geometry
                val indices = geometry.indices
                val positions = geometry.positions
                for (i in blas.start * 3 until (blas.start + blas.length) * 3) {
                    val j = indices[i] * 3
                    assertTrue(blas.bounds.testPoint(positions[j], positions[j + 1], positions[j + 2]))
                }
            }
            else -> throw NotImplementedError()
        }
    }

    @Test
    fun testMaxNodeSize() {
        val limit = 8
        val blas = BVHBuilder.buildBLAS(mesh, SplitMethod.MEDIAN_APPROX, limit)
        testMaxNodeSize(blas!!, limit)
    }

    fun testMaxNodeSize(blasNode: BLASNode, limit: Int): Int {
        return when (blasNode) {
            is BLASBranch -> {
                val sum = testMaxNodeSize(blasNode.n0, limit) + testMaxNodeSize(blasNode.n1, limit)
                assertTrue(sum > limit)
                sum
            }
            is BLASLeaf -> {
                assertTrue(blasNode.length in 1..limit)
                blasNode.length
            }
            else -> throw NotImplementedError()
        }
    }
}