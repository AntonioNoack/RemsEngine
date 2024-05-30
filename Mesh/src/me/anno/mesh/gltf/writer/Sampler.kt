package me.anno.mesh.gltf.writer

import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.Filtering

data class Sampler(
    val filtering: Filtering,
    val clamping: Clamping
)