package me.anno.io.files.inner.temporary

@Suppress("unused")
class InnerTmpByteFile(bytes: ByteArray, ext: String = "bin") : InnerTmpFile(ext) {
    init {
        writeBytes(bytes)
    }

    override fun writeBytes(bytes: ByteArray, offset: Int, length: Int) {
        val bytes2 = if (offset != 0 || length != bytes.size) {
            bytes.copyOfRange(offset, offset + length)
        } else bytes
        data = bytes2
        size = length.toLong()
        compressedSize = size
    }
}