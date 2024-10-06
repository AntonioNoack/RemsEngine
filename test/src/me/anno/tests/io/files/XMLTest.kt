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
import me.anno.io.xml.saveable.XML2JSON
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
        val reader = XMLStringReader(asXML, InvalidRef)
        reader.readAllInList()
        val fromXML = reader.allInstances
        val copyViaBothForTesting = JsonStringWriter.toText(fromXML, InvalidRef)
        assertEquals(asJSON, copyViaBothForTesting)
        Engine.requestShutdown()
    }

    @Test
    fun testXML2JSON() {
        registerCustomClass(Mesh())
        testEquals(flatCube.front)
    }

    fun testEquals(instance: Saveable) {
        val json = JsonStringWriter.toText(instance, InvalidRef)
        println(json)
        val xml = XML2JSON.toXML("xml", JsonReader(json).readArray())
        println(xml)
        val json2 = JsonFormatter.format(XML2JSON.fromXML(xml))
        println(json2)
        val clone = JsonStringReader.readFirst(json2, InvalidRef, instance::class)
        val json3 = JsonStringWriter.toText(clone, InvalidRef)
        assertEquals(json, json3)
    }

    @Test
    fun testEscaping() {
        registerCustomClass(Mesh())
        val mesh = Mesh()
        mesh.name = "<Hi\"!/"
        testEquals(mesh)
    }
}