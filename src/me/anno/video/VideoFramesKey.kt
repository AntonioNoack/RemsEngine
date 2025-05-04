package me.anno.video

import me.anno.io.files.FileReference

data class VideoFramesKey(
    var file: FileReference,
    var scale: Int,
    var bufferIndex: Int,
    var bufferLength: Int,
    var fps: Double
)