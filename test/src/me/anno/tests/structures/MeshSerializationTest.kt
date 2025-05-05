package me.anno.tests.structures

import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshAttribute
import me.anno.gpu.buffer.Attribute
import me.anno.io.files.InvalidRef
import me.anno.io.json.saveable.JsonStringReader
import me.anno.io.json.saveable.JsonStringWriter
import me.anno.io.saveable.Saveable.Companion.registerCustomClass
import me.anno.utils.assertions.assertContentEquals
import org.junit.jupiter.api.Test

class MeshSerializationTest {
    @Test
    fun testMesh() {
        registerCustomClass(Mesh())
        registerCustomClass(Attribute())
        registerCustomClass(MeshAttribute())
        val mesh = Mesh()
        mesh.positions = FloatArray(18) { it.toFloat() % 5f }
        mesh.indices = IntArray(10) { it }
        val text = JsonStringWriter.toText(mesh, InvalidRef)
        val clone = JsonStringReader.readFirst(text, InvalidRef, Mesh::class)
        assertContentEquals(clone.positions, mesh.positions)
        assertContentEquals(clone.indices, mesh.indices)
    }
}