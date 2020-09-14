package me.anno.ui.editor.files.thumbs

import java.io.EOFException
import java.io.InputStream

class WindowsThumbDBStream(val src: InputStream) : InputStream() {

    var ctr = 0
    override fun read(): Int {
        ctr++
        val read = src.read()
        if (read < 0) throw EOFException()
        return read
    }

    fun setFilePtr(target: Int): Int {
        // println("jump $ctr -> $target")
        if (target < ctr) {
            if(target < marked) throw RuntimeException()
            reset()
        }
        while (ctr < target) {
            ctr++
            val read = src.read()
            if (read < 0) return -1
        }
        return target
    }

    var marked = 0
    override fun mark(p0: Int) {
        src.mark(p0)
        marked = ctr
    }

    override fun reset(){
        src.reset()
        ctr = marked
    }

    fun readInt() = read() + read().shl(8) or read().shl(16) or read().shl(24)
    fun readMagic() = readInt()
    fun readLong() = readInt().toLong() + readInt().toLong().shl(32)
    fun readWideChar4() = readLong()

}