package me.anno.tests.io.files

import me.anno.Engine
import me.anno.ecs.components.mesh.Mesh
import me.anno.io.files.InvalidRef
import me.anno.io.json.saveable.JsonStringWriter
import me.anno.io.saveable.Saveable
import me.anno.io.saveable.Saveable.Companion.registerCustomClass
import me.anno.io.yaml.saveable.YAMLStringReader
import me.anno.io.yaml.saveable.YAMLStringWriter
import me.anno.mesh.Shapes.flatCube
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class YAMLTest {
    @Test
    fun testYAMLReaderAndWriter() {
        registerCustomClass(Mesh())
        testReaderAndWriter(flatCube.front)
    }

    fun testReaderAndWriter(instance: Saveable) {
        val asJSON = JsonStringWriter.toText(instance, InvalidRef)
        val asYAML = YAMLStringWriter.toText(instance, InvalidRef)
        val reader = YAMLStringReader(asYAML, InvalidRef)
        reader.readAllInList()
        val fromYAML = reader.allInstances
        val copyViaBothForTesting = JsonStringWriter.toText(fromYAML, InvalidRef)
        assertEquals(asJSON, copyViaBothForTesting)
        Engine.requestShutdown()
    }
}