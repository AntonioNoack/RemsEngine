package me.anno.image

import me.anno.io.files.FileReference
import me.anno.utils.async.Callback

fun interface AsyncImageReader<S> {
    fun read(srcFile: FileReference, source: S, callback: Callback<Image>)
}