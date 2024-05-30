package me.anno.mesh.gltf.writer

class BufferView(
    /**
     * from the start of the buffer, typically binary.size()
     * */
    val offset: Int,
    /**
     * in bytes
     * */
    val length: Int,
    /**
     * GL_ARRAY_BUFFER or GL_ELEMENT_ARRAY_BUFFER
     * */
    val target: Int,
    /**
     * typically zero = automatic
     * */
    val byteStride: Int,
)
