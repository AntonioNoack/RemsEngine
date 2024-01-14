package me.anno.tests.files

import me.anno.Engine
import me.anno.ecs.components.mesh.MeshCache
import me.anno.engine.OfficialExtensions
import me.anno.extensions.ExtensionLoader
import me.anno.io.files.FileReference.Companion.getReference
import me.anno.io.files.InvalidRef
import me.anno.io.json.generic.JsonFormatter
import me.anno.io.json.saveable.JsonStringWriter
import me.anno.io.xml.generic.XMLFormatter
import me.anno.io.xml.saveable.XMLStringWriter

fun main() {
    OfficialExtensions.register()
    ExtensionLoader.load()
    val ref = getReference("res://icon.obj")
    val mesh = MeshCache[ref]!!
    println(JsonFormatter.format(JsonStringWriter.toText(mesh, InvalidRef), "\t", 100))
    println(XMLFormatter.format(XMLStringWriter.toText(mesh, InvalidRef), "\t", 100))
    Engine.requestShutdown()
}