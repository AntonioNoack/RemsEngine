package me.anno.image.aseprite

import me.anno.utils.structures.Collections.filterIsInstance2

data class AseFrame(val duration: Int) {
    val chunks = ArrayList<AseChunk>()

    val cels get() = chunks.filterIsInstance2(AseCel::class)
}
