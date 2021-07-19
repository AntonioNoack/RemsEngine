package me.anno.io

import java.io.InputStream

object EmptyInputStream : InputStream() {
    override fun read(): Int = -1
}