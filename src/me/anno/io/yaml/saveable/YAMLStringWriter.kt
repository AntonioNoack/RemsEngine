package me.anno.io.yaml.saveable

import me.anno.io.files.FileReference
import me.anno.io.json.generic.JsonReader
import me.anno.io.json.saveable.JsonStringWriter
import me.anno.io.saveable.Saveable

object YAMLStringWriter {

    private fun jsonToYAML(json: String): String {
        val json1 = JsonReader(json).readArray()
        val yaml = YAML2JSON.toYAML("RemsEngine", json1, 0)
        return yaml.toString()
    }

    fun toText(entries: Collection<Saveable>, workspace: FileReference): String {
        return jsonToYAML(JsonStringWriter.toText(entries, workspace))
    }

    fun toText(entry: Saveable, workspace: FileReference): String {
        return jsonToYAML(JsonStringWriter.toText(entry, workspace))
    }
}