package me.anno.image.thumbs

import me.anno.io.files.FileReference

data class ThumbnailKey(val file: FileReference, val lastModified: Long, val size: Int)