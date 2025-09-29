package me.anno.mesh.gltf

import me.anno.io.binary.ByteArrayIO.leMagic

internal object GLTFConstants {
    const val GL_BYTE = 0x1400
    const val GL_UNSIGNED_BYTE = 0x1401
    const val GL_SHORT = 0x1402
    const val GL_UNSIGNED_SHORT = 0x1403
    const val GL_UNSIGNED_INT = 0x1405
    const val GL_FLOAT = 0x1406
    const val GL_ARRAY_BUFFER = 34962
    const val GL_ELEMENT_ARRAY_BUFFER = 34963

    val FILE_MAGIC = leMagic("glTF")
    val JSON_CHUNK_MAGIC = leMagic("JSON")
    val BINARY_CHUNK_MAGIC = leMagic("BIN\u0000")
}