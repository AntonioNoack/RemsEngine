package me.anno.audio

import me.anno.animation.LoopingState
import me.anno.io.files.FileKey

data class PipelineKey(
    val file: FileKey,
    val time0: Double,
    val time1: Double,
    val bufferSize: Int,
    val repeat: LoopingState,
)