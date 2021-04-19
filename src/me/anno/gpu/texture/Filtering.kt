package me.anno.gpu.texture

import me.anno.language.translation.NameDesc

enum class Filtering(val baseIsNearest: Boolean, val id: Int, val naming: NameDesc){
    NEAREST(true, 0, NameDesc("Nearest")),
    LINEAR(false, 1, NameDesc("Linear")),
    CUBIC(false, 2, NameDesc("Cubic"));

    fun find(value: Int): Filtering {
        return values().firstOrNull { it.id == value } ?: this
    }
}