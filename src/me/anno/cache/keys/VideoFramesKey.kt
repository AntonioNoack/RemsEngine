package me.anno.cache.keys

import java.io.File

data class VideoFramesKey(
    val file: File,
    val scale: Int,
    val bufferIndex: Int,
    val frameLength: Int,
    val fps: Double
)