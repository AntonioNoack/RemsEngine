package me.anno.image.aseprite

import me.anno.image.raw.IntImage

class AseCel(
    val layerIndex: Int,
    val x: Short,
    val y: Short,
    val opacity: Int,
    val celType: Int,
    val zIndex: Short,
    // depending on type:
    var imageData: IntImage?,
    val linkedFramePosition: Int,
    val tilemapData: ByteArray? // raw tile data for tilemaps (after decompression)
) : AseChunk()