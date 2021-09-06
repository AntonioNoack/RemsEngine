package me.anno.io.text

import me.anno.io.json.ObjectMapper.write
import java.io.OutputStream

class TextStreamWriter(val data: OutputStream) : TextWriterBase() {

    override fun append(v: Char) {
        data.write(v)
    }

    override fun append(v: Int) {
        data.write(v.toString())
    }

    override fun append(v: Long) {
        data.write(v.toString())
    }

    override fun append(v: String) {
        data.write(v)
    }

}