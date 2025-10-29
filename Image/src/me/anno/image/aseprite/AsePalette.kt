package me.anno.image.aseprite

import speiger.primitivecollections.IntToObjectHashMap

data class AsePalette(val size: Int, val entries: IntToObjectHashMap<AsePaletteEntry>) : AseChunk()
