package me.anno.video

import me.anno.io.files.FileReference

data class VideoFrameKey(
    var file: FileReference,
    var scale: Int,
    var bufferIndex: Int,
    var bufferLength: Int,
    var fps: Double,
    val localIndex: Int
)