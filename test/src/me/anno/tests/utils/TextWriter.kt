package me.anno.tests.utils

import me.anno.ecs.prefab.change.CSet
import me.anno.ecs.prefab.change.Path
import me.anno.engine.ECSRegistry
import me.anno.io.files.FileFileRef
import me.anno.io.files.FileReference.Companion.getReference
import me.anno.io.files.InvalidRef
import me.anno.io.json.saveable.JsonStringReader
import me.anno.io.json.saveable.JsonStringWriter

/**
 * a test, because smileys were not written correctly
 * */
fun main() {
    // smileys were not saved correctly, why?
    // because the input stream reader was reading bytes, not chars
    ECSRegistry.init()
    val smiley = "🤔"
    val text = CSet(Path.ROOT_PATH, smiley, 0)
    text.name = smiley
    val asString = JsonStringWriter.toText(text, InvalidRef)
    println(asString)
    val asText = JsonStringReader.readFirst(asString, InvalidRef, false) as CSet
    println("Decoded text: ${asText.name}, ${asText.name!!.length}")
    // this works so far...
    val tmp = FileFileRef.createTempFile("smiley", ".tmp")
    val ref = getReference(tmp.absolutePath)
    ref.writeText(asString)
    println(ref.readTextSync())
    println(tmp)
    // tmp.delete()
}