package me.anno.io.zip

import me.anno.io.files.FileReference

interface IHeavyAccess<Stream> {
    fun openStream(source: FileReference, callback: (Stream?, Exception?) -> Unit)
    fun process(stream: Stream)
    fun closeStream(source: FileReference, stream: Stream)
}