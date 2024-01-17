package me.anno.tests.files

import me.anno.Engine
import me.anno.engine.ECSRegistry
import me.anno.engine.OfficialExtensions
import me.anno.extensions.ExtensionLoader
import me.anno.io.files.InvalidRef
import me.anno.io.json.generic.JsonFormatter
import me.anno.io.json.saveable.JsonStringWriter
import me.anno.io.xml.generic.XMLFormatter
import me.anno.io.xml.saveable.XMLStringReader
import me.anno.io.xml.saveable.XMLStringWriter
import me.anno.mesh.Shapes.flatCube

fun main() {
    OfficialExtensions.register()
    ExtensionLoader.load()
    ECSRegistry.init()
    val mesh = flatCube.front
    val asJSON = JsonFormatter.format(JsonStringWriter.toText(mesh, InvalidRef), "\t", 100)
    val asXML = XMLFormatter.format(XMLStringWriter.toText(mesh, InvalidRef), "\t", 100)
    println(asJSON)
    println(asXML)
    println(XMLStringReader(asXML).children)
    Engine.requestShutdown()
}