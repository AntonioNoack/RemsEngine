package me.anno.image.aseprite

class AseSprite(val header: AseHeader) {
    val layers = ArrayList<AseLayer>()
    val frames = ArrayList<AseFrame>()
    val palettes = ArrayList<AsePalette>()
    val tilesets = ArrayList<AseTileset>()
    val slices = ArrayList<AseSlice>()
    val tags = ArrayList<AseTag>()
}