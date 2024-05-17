package me.anno.tests.structures

import me.anno.ecs.components.mesh.Mesh
import me.anno.io.Saveable.Companion.registerCustomClass
import me.anno.io.files.InvalidRef
import me.anno.io.json.saveable.JsonStringReader
import me.anno.io.json.saveable.JsonStringWriter
import org.junit.jupiter.api.Test
import kotlin.test.assertContentEquals

class SavingMesh {
    @Test
    fun testMesh() {
        registerCustomClass(Mesh())
        val mesh = Mesh()
        mesh.positions = FloatArray(18) { it.toFloat() % 5f }
        mesh.indices = IntArray(10) { it }
        val clone = JsonStringReader.readFirst<Mesh>(JsonStringWriter.toText(mesh, InvalidRef), InvalidRef)
        assertContentEquals(clone.positions, mesh.positions)
        assertContentEquals(clone.indices, mesh.indices)
    }
}