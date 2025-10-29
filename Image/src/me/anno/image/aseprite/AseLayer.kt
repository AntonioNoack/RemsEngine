package me.anno.image.aseprite

import me.anno.utils.types.Booleans.hasFlag

class AseLayer(
    val flags: Int,
    val type: Int,
    val childLevel: Int,
    val blendMode: Int,
    val opacity: Int,
    val name: String,
    val tilesetIndex: Int? = null,
    val uuid: ByteArray? = null
) {
    val isVisible: Boolean
        get() = flags.hasFlag(1)
}