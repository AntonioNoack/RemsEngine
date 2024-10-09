package me.anno.tests.io.files

import me.anno.Engine
import me.anno.ecs.components.mesh.Mesh
import me.anno.io.files.InvalidRef
import me.anno.io.json.saveable.JsonStringWriter
import me.anno.io.saveable.Saveable
import me.anno.io.saveable.Saveable.Companion.registerCustomClass
import me.anno.io.xml.saveable.XMLStringReader
import me.anno.io.xml.saveable.XMLStringWriter
import me.anno.mesh.Shapes.flatCube
import me.anno.utils.assertions.assertEquals
import org.junit.jupiter.api.Test

class XMLTest {
    @Test
    fun testXMLReaderAndWriter() {
        registerCustomClass(Mesh())
        testReaderAndWriter(flatCube.front)
    }

    fun testReaderAndWriter(instance: Saveable) {
        val asJSON = JsonStringWriter.toText(instance, InvalidRef)
        println("json: $asJSON")
        val asXML = XMLStringWriter.toText(instance, InvalidRef)
        println("xml: $asXML")
        val reader = XMLStringReader(asXML, InvalidRef)
        reader.readAllInList()
        val fromXML = reader.allInstances
        val copyViaBothForTesting = JsonStringWriter.toText(fromXML, InvalidRef)
        assertEquals(asJSON, copyViaBothForTesting)
        Engine.requestShutdown()
    }
}