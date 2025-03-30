package me.anno.io.files.inner.lazy

import me.anno.io.files.FileReference
import me.anno.io.files.Signature
import me.anno.io.files.inner.InnerFile
import me.anno.io.files.inner.InnerFolder
import me.anno.io.files.inner.SignatureFile
import me.anno.utils.async.Callback
import java.io.InputStream

class InnerLazyByteFile(
    absolutePath: String, relativePath: String, parent: FileReference,
    val content: Lazy<ByteArray>
) : InnerFile(absolutePath, relativePath, false, parent), SignatureFile {

    @Suppress("unused")
    constructor(folder: InnerFolder, name: String, content: Lazy<ByteArray>) : this(
        "${folder.absolutePath}/$name",
        "${folder.relativePath}/$name",
        folder, content
    )

    override var signature: Signature?
        get() = Signature.find(content.value)
        set(_) {}

    override fun readBytes(callback: Callback<ByteArray>) {
        callback.ok(content.value)
    }

    override fun inputStream(lengthLimit: Long, closeStream: Boolean, callback: Callback<InputStream>) {
        callback.ok(content.value.inputStream())
    }

    override fun readText(callback: Callback<String>) {
        callback.ok(content.value.decodeToString())
    }

    override fun length(): Long {
        return if(content.isInitialized()) content.value.size.toLong() else 100_000L // a guess
    }
}