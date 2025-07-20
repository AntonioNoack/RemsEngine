package me.anno.tests.mesh

import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshCache
import me.anno.ecs.components.mesh.shapes.IcosahedronModel
import me.anno.ecs.components.mesh.utils.IndexRemover.removeIndices
import me.anno.engine.EngineBase.Companion.workspace
import me.anno.engine.OfficialExtensions
import me.anno.utils.OS.documents
import me.anno.utils.algorithms.ForLoop.forLoopSafely
import me.anno.utils.assertions.assertEquals
import org.joml.Vector3f
import org.junit.jupiter.api.Test

object ValidNormalsTest {

    private fun getMesh(withIndices: Boolean): Mesh {
        val mesh = IcosahedronModel.createIcosphere(1)
        if (withIndices && mesh.indices == null) mesh.indices = IntArray(mesh.positions!!.size / 3) { it }
        if (!withIndices && mesh.indices != null) mesh.removeIndices()
        assertEquals(withIndices, mesh.indices != null)
        mesh.normals = null
        return mesh
    }

    @Test
    fun testFlatNormalsAreValidWithIndices() {
        val mesh = getMesh(true)
        mesh.calculateNormals(false)
        validateNormals(mesh)
    }

    @Test
    fun testSmoothNormalsAreValidWithIndex() {
        val mesh = getMesh(true)
        mesh.calculateNormals(true)
        validateNormals(mesh)
    }

    @Test
    fun testFlatNormalsAreValidWithoutIndices() {
        val mesh = getMesh(false)
        mesh.calculateNormals(false)
        validateNormals(mesh)
    }

    @Test
    fun testSmoothNormalsAreValidWithoutIndices() {
        val mesh = getMesh(false)
        mesh.calculateNormals(true)
        validateNormals(mesh)
    }

    private fun validateNormals(mesh: Mesh) {
        val positions = mesh.positions!!
        val normals = mesh.normals!!
        assertEquals(positions.size, normals.size)
        val tmp = Vector3f()
        forLoopSafely(normals.size, 3) { idx ->
            tmp.set(normals, idx)
            assertEquals(1f, tmp.length(), 1e-6f)
        }
    }

    /*@Test
    fun testDuck() {
        OfficialExtensions.initForTests()
        workspace = documents.getChild("RemsEngine/AssetIndex")
        val duck = documents.getChild("RemsEngine/AssetIndex/Rubber Duck.json")
        val mesh = MeshCache.getEntry(duck).waitFor() as Mesh
        mesh.ensureNorTanUVs()
        validateNormals(mesh)
    }*/
}