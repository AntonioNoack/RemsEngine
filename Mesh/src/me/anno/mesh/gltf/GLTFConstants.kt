package me.anno.mesh.gltf

internal object GLTFConstants {
    const val GL_FLOAT = 0x1406
    const val GL_UNSIGNED_BYTE = 0x1401
    const val GL_UNSIGNED_SHORT = 0x1403
    const val GL_UNSIGNED_INT = 0x1405
    const val GL_ARRAY_BUFFER = 34962
    const val GL_ELEMENT_ARRAY_BUFFER = 34963

    val JSON_CHUNK_MAGIC = leMagic('J', 'S', 'O', 'N')
    val BINARY_CHUNK_MAGIC = leMagic('B', 'I', 'N', 0.toChar())
    fun leMagic(b: Char, g: Char, r: Char, a: Char): Int {
        return (a.code shl 24) or (r.code shl 16) or (g.code shl 8) or b.code
    }
}