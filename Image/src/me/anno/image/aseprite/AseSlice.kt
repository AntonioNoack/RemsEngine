package me.anno.image.aseprite

data class AseSlice(val name: String, val flags: Int, val keys: List<AseSliceKey>) : AseChunk()
