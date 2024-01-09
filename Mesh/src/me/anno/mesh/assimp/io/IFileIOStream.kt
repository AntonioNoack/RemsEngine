package me.anno.mesh.assimp.io

interface IFileIOStream {

    companion object {
        const val SEEK_SET = 0
        const val SEEK_CUR = 1
        const val SEEK_END = 2
    }

    var position: Long

    val length: Long

    fun close()

    // whence = from where / where to add the offset
    fun seek(offset: Long, whence: Int): Int

    // returns the number of items, not number of bytes
    fun read(buffer: Long, size: Long, count: Long): Long

}