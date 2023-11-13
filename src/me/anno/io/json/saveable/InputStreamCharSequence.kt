package me.anno.io.json.saveable

import me.anno.utils.structures.arrays.IntArrayList
import java.io.InputStream
import java.io.InputStreamReader

class InputStreamCharSequence(val input: InputStream, length: Int) : CharSequence {

    // probably we could only memorize the last n bytes, since more won't be needed
    // or the TextReader could give the signal, that we're allowed to forget

    // to support non-ascii charsets
    private val reader = InputStreamReader(input)

    private val memory = IntArrayList(512)
    override var length: Int = if (length <= 0) Int.MAX_VALUE else length

    private fun ensureIndex(index: Int) {
        while (memory.size <= kotlin.math.min(index, length)) {
            val read = reader.read()
            if (read < 0) {
                this.length = kotlin.math.min(
                    this.length,
                    memory.size
                )
            }
            memory.add(read)
        }
        while (memory.size <= index) {
            memory.add(0)
        }
    }

    override fun get(index: Int): Char {
        ensureIndex(index)
        return memory[index].toChar()
    }

    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence {
        ensureIndex(endIndex - 1)
        return String(CharArray(endIndex - startIndex) {
            memory[startIndex + it].toChar()
        })
    }

    override fun toString(): String {
        val builder = StringBuilder(kotlin.math.min(1024, length))
        var index = -1
        while (++index < length) builder.append(get(index))
        return builder.toString()
    }

}