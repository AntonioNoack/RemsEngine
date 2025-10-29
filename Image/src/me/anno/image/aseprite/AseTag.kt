package me.anno.image.aseprite

class AseTag(
    val fromFrame: Int,
    val toFrame: Int,
    val direction: Int,
    val repeat: Int,
    val colorRgb: IntArray?, // optional legacy color (3 bytes)
    val name: String
) : AseChunk()