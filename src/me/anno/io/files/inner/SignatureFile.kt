package me.anno.io.files.inner

import me.anno.io.Streams.readNBytes2
import me.anno.io.files.Signature
import me.anno.utils.assertions.assertFalse
import java.io.InputStream

/**
 * file, which has a pre-computed signature
 * */
interface SignatureFile {

    var signature: Signature?

    companion object {
        @JvmStatic
        fun setDataAndSignature(file: InnerFileWithData, size: Long, getInputStream: () -> InputStream) {
            assertFalse(file.isDirectory)

            file.size = size
            getInputStream().use { input ->
                if (size <= InnerFolderCache.sizeLimit) {
                    file.data = input.readBytes()
                    file.signature = Signature.find(file.data!!)
                } else {
                    val bytes = input.readNBytes2(Signature.sampleSize, false)
                    file.signature = Signature.find(bytes)
                }
            }
        }
    }
}