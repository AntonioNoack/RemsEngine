package me.anno.ui.base.components

class Padding(l: Int, t: Int, r: Int, b: Int) : LTRB(l, t, r, b) {

    constructor(all: Int) : this(all, all, all, all)
    constructor(x: Int, y: Int) : this(x, y, x, y)
    constructor() : this(0)

    companion object {
        val Zero get() = Padding()
    }

    operator fun plusAssign(s: Padding) {
        left += s.left
        top += s.top
        right += s.right
        bottom += s.bottom
    }

    fun clone(): Padding = Padding(left, top, right, bottom)

    override val className: String get() = "Padding"
}