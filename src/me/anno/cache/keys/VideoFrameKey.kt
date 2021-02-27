package me.anno.cache.keys

import java.io.File

data class VideoFrameKey(
    val file: File,
    val scale: Int,
    val bufferIndex: Int,
    val frameLength: Int,
    val localIndex: Int,
    val fps: Double
)