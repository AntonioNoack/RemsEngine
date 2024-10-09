package me.anno.io.json.saveable

import me.anno.io.files.FileReference
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.io.Writer

class JsonStreamWriter(val writer: Writer, workspace: FileReference) : JsonWriterBase(workspace) {

    private val c = CharArray(1)

    override fun flush() {
        super.flush()
        writer.flush()
    }

    override fun append(v: Char) {
        // a character may be part of a smiley, so
        // we have to use a Writer instead of an OutputStream
        c[0] = v
        writer.write(c)
    }

    override fun append(v: Int) {
        writer.write(v.toString())
    }

    override fun append(v: Long) {
        writer.write(v.toString())
    }

    override fun append(v: String) {
        writer.write(v)
    }

}