package me.anno.video

import me.anno.io.files.FileReference

data class VideoFramesKey(
    val file: FileReference,
    val scale: Int,
    val bufferIndex: Int,
    val bufferLength: Int,
    val fps: Double
)