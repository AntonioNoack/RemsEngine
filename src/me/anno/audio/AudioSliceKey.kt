package me.anno.audio

import me.anno.io.files.FileReference

data class AudioSliceKey(val file: FileReference, val slice: Long)