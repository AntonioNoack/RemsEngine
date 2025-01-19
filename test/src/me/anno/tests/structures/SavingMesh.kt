package me.anno.tests.structures

import me.anno.ecs.components.mesh.Mesh
import me.anno.io.files.InvalidRef
import me.anno.io.json.saveable.JsonStringReader
import me.anno.io.json.saveable.JsonStringWriter
import me.anno.io.saveable.Saveable.Companion.registerCustomClass
import me.anno.utils.assertions.assertContentEquals
import org.junit.jupiter.api.Test

class SavingMesh {
    @Test
    fun testMesh() {
        registerCustomClass(Mesh())
        val mesh = Mesh()
        mesh.positions = FloatArray(18) { it.toFloat() % 5f }
        mesh.indices = IntArray(10) { it }
        val clone = JsonStringReader.readFirst(JsonStringWriter.toText(mesh, InvalidRef), InvalidRef, Mesh::class)
        assertContentEquals(clone.positions, mesh.positions)
        assertContentEquals(clone.indices, mesh.indices)
    }
}