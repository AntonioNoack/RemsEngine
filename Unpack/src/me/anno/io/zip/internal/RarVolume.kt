package me.anno.io.zip.internal

import com.github.junrar.Archive
import com.github.junrar.Volume
import com.github.junrar.io.IReadOnlyAccess
import me.anno.io.files.FileReference

class RarVolume(val a: Archive, val file: FileReference) : Volume {
        val bytes by lazy { file.readBytesSync() }
        override fun getReadOnlyAccess(): IReadOnlyAccess {
            return RarReadOnlyAccess(bytes)
        }

        override fun getLength(): Long = file.length()
        override fun getArchive(): Archive = a
    }