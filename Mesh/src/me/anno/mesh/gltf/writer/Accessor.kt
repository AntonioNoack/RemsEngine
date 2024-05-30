package me.anno.mesh.gltf.writer

class Accessor(
    val view: Int,
    val type: String,
    val componentType: Int,
    val count: Int,
    val normalized: Boolean,
    val min: String?,
    val max: String?
)