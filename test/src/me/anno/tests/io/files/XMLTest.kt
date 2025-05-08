package me.anno.tests.io.files

import me.anno.Engine
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshAttribute
import me.anno.gpu.buffer.Attribute
import me.anno.io.files.InvalidRef
import me.anno.io.json.saveable.JsonStringWriter
import me.anno.io.saveable.Saveable
import me.anno.io.saveable.Saveable.Companion.registerCustomClass
import me.anno.io.xml.saveable.XMLStringReader
import me.anno.io.xml.saveable.XMLStringWriter
import me.anno.mesh.Shapes.flatCube
import me.anno.utils.assertions.assertEquals
import org.apache.logging.log4j.LogManager
import org.junit.jupiter.api.Test

class XMLTest {
    companion object {
        private val LOGGER = LogManager.getLogger(XMLTest::class)
    }

    @Test
    fun testXMLReaderAndWriter() {
        registerCustomClass(Mesh())
        registerCustomClass(Attribute())
        registerCustomClass(MeshAttribute())
        testReaderAndWriter(flatCube.front)
    }

    fun testReaderAndWriter(instance: Saveable) {
        val asJSON = JsonStringWriter.toText(instance, InvalidRef)
        LOGGER.debug("json: {}", asJSON)
        val asXML = XMLStringWriter.toText(instance, InvalidRef)
        LOGGER.debug("xml: {}", asXML)
        val reader = XMLStringReader(asXML, InvalidRef)
        reader.readAllInList()
        val fromXML = reader.allInstances
        val copyViaBothForTesting = JsonStringWriter.toText(fromXML, InvalidRef)
        assertEquals(asJSON, copyViaBothForTesting)
        Engine.requestShutdown()
    }
}