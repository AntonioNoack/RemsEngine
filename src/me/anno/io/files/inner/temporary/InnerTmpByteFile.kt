package me.anno.io.files.inner.temporary

@Suppress("unused")
class InnerTmpByteFile(bytes: ByteArray, ext: String = "bin") : InnerTmpFile(ext) {
    init {
        data = bytes
        size = bytes.size.toLong()
        compressedSize = size
    }
}