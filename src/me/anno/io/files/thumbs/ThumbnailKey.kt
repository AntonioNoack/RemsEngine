package me.anno.io.files.thumbs

import me.anno.io.files.FileReference

data class ThumbnailKey(val file: FileReference, val lastModified: Long, val isDirectory: Boolean, val size: Int)