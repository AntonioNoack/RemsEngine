package me.anno.io.zip.internal

import com.github.junrar.Archive
import com.github.junrar.Volume
import me.anno.io.files.FileReference

class RarVolume(val a: Archive, val file: FileReference) : Volume {
    private val bytes by lazy { file.readBytesSync() }
    override fun getLength(): Long = file.length()
    override fun getArchive(): Archive = a
    override fun getReadOnlyAccess() = RarReadOnlyAccess(bytes)
}