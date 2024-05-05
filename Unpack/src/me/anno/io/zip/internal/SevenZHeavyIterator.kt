package me.anno.io.zip.internal

import me.anno.io.files.FileReference
import me.anno.io.files.inner.IHeavyIterable
import me.anno.io.zip.Inner7zFile
import me.anno.utils.structures.Callback
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry
import java.io.ByteArrayInputStream
import java.io.InputStream

class SevenZHeavyIterator(val self: Inner7zFile, val callback: Callback<InputStream>) :
    IHeavyIterable<SevenZArchiveEntry, SevenZIterator, ByteArray> {
    override fun openStream(source: FileReference) = SevenZIterator(self.getZipStream())
    override fun hasInterest(stream: SevenZIterator, item: SevenZArchiveEntry) = item.name == self.relativePath
    override fun process(
        stream: SevenZIterator,
        item: SevenZArchiveEntry,
        previous: ByteArray?,
        index: Int,
        total: Int
    ): ByteArray {
        val bytes = previous ?: stream.file.getInputStream(item).use {
            it.readBytes()
        }
        callback.ok(ByteArrayInputStream(bytes))
        return bytes
    }

    override fun closeStream(source: FileReference, stream: SevenZIterator) {
        stream.file.close()
    }
}