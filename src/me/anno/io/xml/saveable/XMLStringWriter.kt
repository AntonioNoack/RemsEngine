package me.anno.io.xml.saveable

import me.anno.engine.projects.FileEncoding
import me.anno.io.files.FileReference
import me.anno.io.saveable.Saveable

object XMLStringWriter {

    fun toText(entries: Collection<Saveable>, workspace: FileReference): String {
        return FileEncoding.PRETTY_XML.encode(entries.toList(), workspace).decodeToString()
    }

    fun toText(entry: Saveable, workspace: FileReference): String {
        return toText(listOf(entry), workspace)
    }
}