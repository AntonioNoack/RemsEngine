package me.anno.tests.files

import me.anno.Engine
import me.anno.ecs.components.mesh.Mesh
import me.anno.io.Saveable
import me.anno.io.Saveable.Companion.registerCustomClass
import me.anno.io.files.InvalidRef
import me.anno.io.json.saveable.JsonStringWriter
import me.anno.io.xml.saveable.XMLStringReader
import me.anno.io.xml.saveable.XMLStringWriter
import me.anno.mesh.Shapes.flatCube
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class XMLTest {
    @Test
    fun testXMLReaderAndWriter() {
        registerCustomClass(Mesh())
        testReaderAndWriter(flatCube.front)
    }

    fun testReaderAndWriter(instance: Saveable) {
        val asJSON = JsonStringWriter.toText(instance, InvalidRef)
        val asXML = XMLStringWriter.toText(instance, InvalidRef)
        val fromXML = XMLStringReader(asXML).children
        val copyViaBothForTesting = JsonStringWriter.toText(fromXML, InvalidRef)
        assertEquals(asJSON, copyViaBothForTesting)
        Engine.requestShutdown()
    }
}