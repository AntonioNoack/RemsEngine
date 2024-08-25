package me.anno.io

import java.io.OutputStream

object VoidOutputStream: OutputStream() {
    override fun write(b: Int) {}
    override fun write(b: ByteArray) {}
    override fun write(b: ByteArray, off: Int, len: Int) {}
}