package me.anno.games.creeperworld

import me.anno.image.Image
import me.anno.image.raw.IntImage
import me.anno.language.translation.NameDesc

class RockType(
    val id: Int, val name: NameDesc,
    val properties: Map<RockProperty, Float>,
    val texture: Image
) {
    constructor(id: Int, name: NameDesc, properties: Map<RockProperty, Float>, color: Int) :
            this(id, name, properties, IntImage(1, 1, intArrayOf(color), false))
}