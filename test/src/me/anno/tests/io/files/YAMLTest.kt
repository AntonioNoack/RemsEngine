package me.anno.tests.io.files

import me.anno.Engine
import me.anno.ecs.components.mesh.Mesh
import me.anno.io.files.InvalidRef
import me.anno.io.json.generic.JsonFormatter
import me.anno.io.json.generic.JsonReader
import me.anno.io.json.saveable.JsonStringReader
import me.anno.io.json.saveable.JsonStringWriter
import me.anno.io.saveable.Saveable
import me.anno.io.saveable.Saveable.Companion.registerCustomClass
import me.anno.io.yaml.saveable.YAML2JSON
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
        val fromYAML = reader.sortedContent
        val copyViaBothForTesting = JsonStringWriter.toText(fromYAML, InvalidRef)
        assertEquals(asJSON, copyViaBothForTesting)
        Engine.requestShutdown()
    }

    @Test
    fun testYAML2JSON() {
        registerCustomClass(Mesh())
        val json = JsonStringWriter.toText(flatCube.front, InvalidRef)
        println(json)
        val yaml = YAML2JSON.toYAML("YAML", JsonReader(json).readArray(), 0)
        println(yaml)
        val json2 = JsonFormatter.format(YAML2JSON.fromYAML(yaml))
        println(json2)
        val clone = JsonStringReader.readFirst(json2, InvalidRef, Mesh::class)
        val json3 = JsonStringWriter.toText(clone, InvalidRef)
        assertEquals(json, json3)
    }
}