package me.anno.io.files.inner

import me.anno.io.BufferedIO.useBuffered
import me.anno.io.files.Signature
import me.anno.io.Streams.readNBytes2
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
                val bufferedIn = getInputStream().useBuffered()
                if (file.size in 1..InnerFolderCache.sizeLimit) {
                    file.data = bufferedIn.readBytes()
                    file.signature = Signature.find(file.data!!)
                } else {
                    val bytes = bufferedIn.readNBytes2(Signature.sampleSize, false)
                    file.signature = Signature.find(bytes)
                }
                bufferedIn.close()
            }
        }
    }
}