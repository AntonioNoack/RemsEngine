package me.anno.gpu.texture

enum class Filtering(val baseIsNearest: Boolean, val id: Int, val displayName: String){
    NEAREST(true, 0, "Nearest"),
    LINEAR(false, 1, "Linear"),
    CUBIC(true, 2, "Cubic");

    fun find(value: Int): Filtering {
        return values().firstOrNull { it.id == value } ?: this
    }
}