package me.anno.tests.mesh.gltf

import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshCache
import me.anno.ecs.components.mesh.shapes.IcosahedronModel
import me.anno.io.files.inner.InnerFolder
import me.anno.io.files.inner.temporary.InnerTmpByteFile
import me.anno.mesh.gltf.GLTFReader
import me.anno.mesh.gltf.GLTFWriter
import me.anno.utils.Sleep
import me.anno.utils.assertions.assertContentEquals
import me.anno.utils.assertions.assertEquals
import me.anno.utils.types.Strings.toFloat
import org.junit.jupiter.api.Test

class GLTFReadWriteTests {

    @Test
    fun testFloatReading() {
        assertEquals(0.58778524f, ("0.58778524" as CharSequence).toFloat())
    }

    /**
     * test saving, loading, check for identity
     * */
    @Test
    fun testMeshIO() {
        val mesh = IcosahedronModel.createIcosphere(1)
        var bytes: ByteArray? = null
        var done = false
        GLTFWriter().apply {
            // todo bug: Entity.addComponent(MeshComponent(mesh)) doesn't work
            write(mesh) { it, e ->
                e?.printStackTrace()
                bytes = it
                done = true
            }
        }
        Sleep.waitUntil(true) { done }
        val tmpFile = InnerTmpByteFile(bytes!!)
        var hasFolder = false
        var folder: InnerFolder? = null
        GLTFReader.readAsFolder(tmpFile) { folder1, e ->
            folder = folder1
            hasFolder = true
        }
        Sleep.waitUntil(true) { hasFolder }
        val meshFile = folder!!.getChild("meshes").listChildren().first()
        val loadedMesh = MeshCache.getEntry(meshFile).waitFor() as Mesh
        assertContentEquals(mesh.positions, loadedMesh.positions, 1e-6f)
        assertContentEquals(mesh.normals, loadedMesh.normals, 1e-6f)
    }
}