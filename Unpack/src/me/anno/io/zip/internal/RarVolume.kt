package me.anno.io.zip.internal

import com.github.junrar.Archive
import com.github.junrar.Volume
import me.anno.io.files.FileReference

class RarVolume(private val a: Archive, private val file: FileReference, private val bytes: ByteArray) : Volume {
    override fun getLength(): Long = file.length()
    override fun getArchive(): Archive = a
    override fun getReadOnlyAccess() = RarReadOnlyAccess(bytes)
}