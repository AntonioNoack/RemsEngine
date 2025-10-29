package me.anno.image.aseprite

data class AseHeader(
    val fileSize: Long,
    val frames: Int,
    val width: Int,
    val height: Int,
    val colorDepth: Int,
    val flags: Int,
    val speed: Int,
    val transparentIndex: Int,
    val numColors: Int,
    val pixelWidth: Int,
    val pixelHeight: Int,
    val gridX: Short,
    val gridY: Short,
    val gridWidth: Int,
    val gridHeight: Int
)