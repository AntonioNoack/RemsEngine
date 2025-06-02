package me.anno.io.links

import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.files.Reference.getReference
import me.anno.io.files.inner.InnerFolder
import me.anno.io.files.inner.InnerLinkFile
import me.anno.utils.async.Callback
import me.anno.utils.async.Callback.Companion.USE_COROUTINES_INSTEAD
import me.anno.utils.async.waitForCallback

object LNKReader {

    suspend fun readLNKAsFolder(src: FileReference): Result<InnerFolder> {
        return waitForCallback { readLNKAsFolder(src, it) }
    }

    @Deprecated(USE_COROUTINES_INSTEAD)
    fun readLNKAsFolder(file: FileReference, callback: Callback<InnerFolder>) {
        WindowsShortcut.get(file) { value, err ->
            val link = getReference(value?.absolutePath)
            if (link != InvalidRef) {
                val folder = InnerFolder(file)
                val iconPath = getReference(value?.iconPath)
                if (iconPath != InvalidRef && iconPath.name != link.name) {
                    InnerLinkFile(folder, iconPath.name, iconPath)
                }
                InnerLinkFile(folder, link.name, link)
                callback.ok(folder)
            } else callback.err(err)
        }
    }
}