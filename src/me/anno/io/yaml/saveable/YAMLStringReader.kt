package me.anno.io.yaml.saveable

import me.anno.engine.projects.FileEncoding
import me.anno.io.Streams.readText
import me.anno.io.files.FileReference
import me.anno.io.saveable.ReaderImpl
import me.anno.io.saveable.Saveable
import me.anno.io.saveable.StreamReader
import me.anno.io.saveable.StringReader
import java.io.InputStream

class YAMLStringReader(val yamlStr: CharSequence, val workspace: FileReference) : ReaderImpl {

    override fun readAllInList() {
        val bytes = yamlStr.toString().encodeToByteArray()
        allInstances = FileEncoding.YAML.decode(bytes, workspace, false)
    }

    override fun finish() {}

    override var allInstances: List<Saveable> = emptyList()

    companion object : StringReader, StreamReader {
        override fun createReader(data: CharSequence, workspace: FileReference, sourceName: String): ReaderImpl {
            return YAMLStringReader(data, workspace)
        }

        override fun toText(element: Saveable, workspace: FileReference): String {
            return YAMLStringWriter.toText(element, workspace)
        }

        override fun createReader(data: InputStream, workspace: FileReference, sourceName: String): ReaderImpl {
            return createReader(data.readText(), workspace, sourceName)
        }
    }
}