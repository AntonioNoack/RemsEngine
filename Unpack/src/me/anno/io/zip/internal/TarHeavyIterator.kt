package me.anno.io.zip.internal

import me.anno.io.files.FileReference
import me.anno.io.files.inner.IHeavyIterable
import me.anno.io.zip.InnerTarFile
import me.anno.utils.structures.Callback
import org.apache.commons.compress.archivers.ArchiveEntry
import java.io.ByteArrayInputStream
import java.io.InputStream

class TarHeavyIterator(val self: InnerTarFile, val callback: Callback<InputStream>) :
    IHeavyIterable<ArchiveEntry, TarArchiveIterator, ByteArray> {
    override fun openStream(source: FileReference, callback: Callback<TarArchiveIterator>) {
        self.getZipStream { stream, err ->
            if (stream != null) callback.ok(TarArchiveIterator(stream))
            else err?.printStackTrace()
        }
    }

    override fun hasInterest(stream: TarArchiveIterator, item: ArchiveEntry) = item.name == self.readingPath
    override fun process(
        stream: TarArchiveIterator,
        item: ArchiveEntry, previous: ByteArray?,
        index: Int, total: Int
    ): ByteArray {
        val bytes = previous ?: stream.file.readBytes()
        callback.ok(ByteArrayInputStream(bytes))
        return bytes
    }
}