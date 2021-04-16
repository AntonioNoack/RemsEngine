package me.anno.cache.keys

import me.anno.io.FileReference

data class VideoFrameKey(
    val file: FileReference,
    val scale: Int,
    val bufferIndex: Int,
    val frameLength: Int,
    val localIndex: Int,
    val fps: Double
)