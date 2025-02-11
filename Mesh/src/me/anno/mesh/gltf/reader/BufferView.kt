package me.anno.mesh.gltf.reader

import java.io.ByteArrayInputStream
import java.io.InputStream

class BufferView(val buffer: ByteArray, val offset: Int, val length: Int) {
    fun stream(): InputStream {
        return ByteArrayInputStream(buffer, offset, length)
    }
}