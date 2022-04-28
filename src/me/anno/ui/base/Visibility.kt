package me.anno.ui.base

enum class Visibility(val id: Int) {
    GONE(0), VISIBLE(1);

    companion object {
        operator fun get(v: Boolean) = if (v) VISIBLE else GONE
    }
}