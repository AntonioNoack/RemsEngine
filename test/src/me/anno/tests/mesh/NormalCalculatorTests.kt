package me.anno.tests.mesh

import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshIterators.forEachTriangleIndex
import me.anno.ecs.components.mesh.shapes.IcosahedronModel
import me.anno.ecs.components.mesh.shapes.UVSphereModel
import me.anno.ecs.components.mesh.utils.IndexRemover.removeIndices
import me.anno.engine.DefaultAssets.flatCube
import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertNotNull
import me.anno.utils.assertions.assertNull
import org.joml.Vector3f
import org.junit.jupiter.api.Test
import kotlin.math.sqrt

class NormalCalculatorTests {
    @Test
    fun testFlatSphere1() {
        testFlatShape(IcosahedronModel.createIcosphere(2))
    }

    @Test
    fun testFlatSphere2() {
        testFlatShape(UVSphereModel.createUVSphere(10, 10))
    }

    @Test
    fun testFlatCube() {
        testFlatShape(flatCube.deepClone())
    }

    fun testFlatShape(shape: Mesh) {
        val numTris = shape.numPrimitives
        shape.normals = null
        shape.removeIndices()
        shape.calculateNormals(false)
        val pa = Vector3f()
        val pb = Vector3f()
        val pc = Vector3f()
        val pos = shape.positions!!
        val nor = shape.normals!!
        val tmp = Vector3f()
        var ctr = 0
        shape.forEachTriangleIndex { ai, bi, ci ->
            pa.set(pos, ai * 3)
            pb.set(pos, bi * 3)
            pc.set(pos, ci * 3)
            val normal = pb.sub(pa).cross(pc.sub(pa)).normalize()
            assertEquals(normal, tmp.set(nor, ai * 3))
            assertEquals(normal, tmp.set(nor, bi * 3))
            assertEquals(normal, tmp.set(nor, ci * 3))
            ctr++
            false
        }
        assertEquals(numTris, ctr.toLong())
    }

    /**
     * test cube, make it smooth
     * */
    @Test
    fun testSmoothCube() {
        val shape = flatCube.deepClone()
        shape.normals = null
        shape.removeIndices()
        assertNull(shape.normals)
        assertNull(shape.indices)
        shape.calculateNormals(true)
        val pos = assertNotNull(shape.positions)
        val nor = assertNotNull(shape.normals)
        assertEquals(pos.size, nor.size)
        val scale = sqrt(3f)
        for (i in pos.indices) {
            assertEquals(pos[i], nor[i] * scale, 0.5f)
        }
    }

    /**
     * test sphere without indices, generate new indices
     * */
    @Test
    fun testSphereNewIndices() {
        val shape = UVSphereModel.createUVSphere(10, 10)
        val numIndices = shape.indices!!.size
        shape.normals = null
        shape.removeIndices()
        assertNull(shape.normals)
        assertNull(shape.indices)
        shape.calculateNormals(true)
        assertNotNull(shape.normals)
        assertNotNull(shape.indices)
        assertEquals(numIndices, shape.indices!!.size)
    }
}