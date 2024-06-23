package me.anno.io.xml.saveable

import me.anno.io.Streams.readText
import me.anno.io.files.FileReference
import me.anno.io.json.generic.JsonFormatter
import me.anno.io.json.saveable.JsonStringReader
import me.anno.io.saveable.ReaderImpl
import me.anno.io.saveable.Saveable
import me.anno.io.saveable.StreamReader
import me.anno.io.saveable.StringReader
import me.anno.io.xml.generic.XMLNode
import me.anno.io.xml.generic.XMLReader
import java.io.ByteArrayInputStream
import java.io.InputStream

class XMLStringReader(val xmlStr: CharSequence, val workspace: FileReference) : ReaderImpl {

    override fun readAllInList() {
        val stream = ByteArrayInputStream(xmlStr.toString().encodeToByteArray())
        sortedContent = JsonStringReader.read(
            JsonFormatter.format(XML2JSON.fromXML(XMLReader().read(stream) as XMLNode)),
            workspace, false
        )
    }

    override fun finish() {}

    override var sortedContent: List<Saveable> = emptyList()

    companion object : StringReader, StreamReader {
        override fun createReader(data: CharSequence, workspace: FileReference, sourceName: String): ReaderImpl {
            return XMLStringReader(data, workspace)
        }

        override fun toText(element: Saveable, workspace: FileReference): String {
            return XMLStringWriter.toText(element, workspace)
        }

        override fun createReader(data: InputStream, workspace: FileReference, sourceName: String): ReaderImpl {
            return createReader(data.readText(), workspace, sourceName)
        }
    }
}