package me.anno.mesh.gltf.reader

import java.io.ByteArrayInputStream
import java.io.InputStream

class BufferView(val buffer: ByteArray, val offset: Int, val length: Int) {
    fun stream(): InputStream = ByteArrayInputStream(buffer, offset, length)
    fun bytes(): ByteArray =
        if (offset == 0 && length == buffer.size) buffer
        else buffer.copyOfRange(offset, offset + length)
}