package me.anno.image

import me.anno.io.files.FileReference

fun interface AsyncImageReader<S> {
    suspend fun read(srcFile: FileReference, source: S): Result<Image>
}