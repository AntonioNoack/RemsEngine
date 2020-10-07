package me.anno.ui.base

enum class Visibility {
    VISIBLE, GONE;
    companion object {
        operator fun get(v: Boolean) = if(v) VISIBLE else GONE
    }
}