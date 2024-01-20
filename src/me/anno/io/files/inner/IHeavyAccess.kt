package me.anno.io.files.inner

import me.anno.utils.structures.Callback
import me.anno.io.files.FileReference

interface IHeavyAccess<Stream> {
    fun openStream(source: FileReference, callback: Callback<Stream>)
    fun process(stream: Stream)
    fun closeStream(source: FileReference, stream: Stream)
}