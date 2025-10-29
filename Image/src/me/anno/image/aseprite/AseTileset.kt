package me.anno.image.aseprite

class AseTileset(
    val tilesetId: Long,
    val flags: Long,
    val numTiles: Int,
    val tileWidth: Int,
    val tileHeight: Int,
    val baseIndex: Int,
    val name: String,
    val embeddedTilesData: ByteArray?
) : AseChunk()