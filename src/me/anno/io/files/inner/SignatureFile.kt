package me.anno.io.files.inner

import me.anno.io.Streams.readNBytes2
import me.anno.io.files.Signature
import java.io.InputStream

/**
 * file, which has a pre-computed signature
 * */
interface SignatureFile {

    var signature: Signature?

    companion object {
        @JvmStatic
        fun setDataAndSignature(file: InnerFile, getInputStream: () -> InputStream) {
            if (!file.isDirectory) {
                file as SignatureFile
                getInputStream().use { input ->
                    if (file.size in 1..InnerFolderCache.sizeLimit) {
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
}