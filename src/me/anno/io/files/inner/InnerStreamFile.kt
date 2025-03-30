package me.anno.io.files.inner

import me.anno.io.Streams.readText
import me.anno.io.files.FileReference
import me.anno.utils.async.Callback
import java.io.InputStream

open class InnerStreamFile(
    absolutePath: String, relativePath: String, parent: FileReference,
    val getStream: () -> InputStream
) : InnerFile(absolutePath, relativePath, false, parent) {

    @Suppress("unused")
    constructor(folder: InnerFolder, name: String, getStream: () -> InputStream) : this(
        "${folder.absolutePath}/$name",
        "${folder.relativePath}/$name",
        folder,
        getStream
    )

    override fun inputStream(lengthLimit: Long, closeStream: Boolean, callback: Callback<InputStream>) {
        callback.ok(getStream())
    }

    override fun readBytes(callback: Callback<ByteArray>) {
        callback.ok(getStream().readBytes())
    }

    override fun readText(callback: Callback<String>) {
        callback.ok(getStream().readText())
    }

    override fun length(): Long {
        return 100_000L // unknown
    }
}