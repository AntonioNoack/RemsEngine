package me.anno.io.files.inner.temporary

@Suppress("unused")
class InnerTmpByteFile(bytes: ByteArray, ext: String = "bin") : InnerTmpFile(ext) {
    init {
        writeBytes(bytes)
    }

    override fun writeBytes(bytes: ByteArray) {
        data = bytes
        size = bytes.size.toLong()
        compressedSize = size
    }
}