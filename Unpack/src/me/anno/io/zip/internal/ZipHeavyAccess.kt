package me.anno.io.zip.internal

import me.anno.io.files.FileReference
import me.anno.io.files.inner.IHeavyAccess
import me.anno.io.zip.InnerZipFile
import me.anno.utils.async.Callback
import org.apache.commons.compress.archivers.zip.ZipFile
import java.io.InputStream

class ZipHeavyAccess(val self: InnerZipFile, val callback: Callback<InputStream>) : IHeavyAccess<ZipFile> {
    override fun openStream(source: FileReference, callback: Callback<ZipFile>) = self.getZipStream(callback)
    override fun closeStream(source: FileReference, stream: ZipFile) = stream.close()
    override fun process(stream: ZipFile) {
        val entry = stream.getEntry(self.relativePath)
        callback.ok(stream.getInputStream(entry))
    }
}