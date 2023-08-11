package me.anno.gpu.blending

@Suppress("unused")
enum class BlendFunc(val hasParams: Boolean) {
    ADD(true),
    SUB(true),
    REV_SUB(true),
    MIN(false),
    MAX(false)
}