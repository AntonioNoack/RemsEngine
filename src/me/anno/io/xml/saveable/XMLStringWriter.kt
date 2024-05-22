package me.anno.io.xml.saveable

import me.anno.io.files.FileReference
import me.anno.io.json.generic.JsonReader
import me.anno.io.json.saveable.JsonStringWriter
import me.anno.io.saveable.Saveable

object XMLStringWriter {

    private fun jsonToXML(json: String): String {
        val json1 = JsonReader(json).readArray()
        val xml = XML2JSON.toXML("RemsEngine", json1)
        return xml.toString()
    }

    fun toText(entries: Collection<Saveable>, workspace: FileReference): String {
        return jsonToXML(JsonStringWriter.toText(entries, workspace))
    }

    fun toText(entry: Saveable, workspace: FileReference): String {
        return jsonToXML(JsonStringWriter.toText(entry, workspace))
    }
}