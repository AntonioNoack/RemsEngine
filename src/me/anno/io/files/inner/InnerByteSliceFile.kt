package me.anno.io.files.inner

import me.anno.graph.hdb.ByteSlice
import me.anno.io.files.FileReference
import me.anno.io.files.Signature
import me.anno.utils.async.Callback
import java.io.InputStream

class InnerByteSliceFile(absolutePath: String, relativePath: String, parent: FileReference, val slice: ByteSlice) :
    InnerStreamFile(absolutePath, relativePath, parent, slice::stream), SignatureFile {
    override var signature: Signature? = Signature.find(slice)
    override fun length(): Long {
        return slice.size.toLong()
    }

    override fun inputStream(lengthLimit: Long, closeStream: Boolean, callback: Callback<InputStream>) {
        callback.ok(slice.stream())
    }
}