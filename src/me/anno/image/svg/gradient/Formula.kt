package me.anno.image.svg.gradient

class Formula(var v: Float, var x: Float, var y: Float, var xx: Float, var xy: Float, var yy: Float) {

    constructor() : this(0f, 0f, 0f, 0f, 0f, 0f)

    fun clear() {
        v = 0f
        x = 0f
        y = 0f
        xx = 0f
        xy = 0f
        yy = 0f
    }

    fun transform(cx: Float, cy: Float, scale: Float) {
        translate(cx, cy)
        scale(scale)
    }

    fun scale(scale: Float) {
        if (scale == 1f) return
        val invScale = 1f / scale
        val invScale2 = invScale * invScale
        x *= invScale
        y *= invScale
        xx *= invScale2
        xy *= invScale2
        yy *= invScale2
    }

    fun translate(dx: Float, dy: Float) {
        v += x * dx + y * dy + dx * dx + dy * dy
        x += 2 * xx * dx// + xy * dy
        y += 2 * yy * dy// - xy * dx // why is the switched sign required?
    }

    override fun toString(): String = "($v $x $y $xx $xy $yy)"

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val t = Formula(0f, 1f, 17f, 1f, 1f, 1f)
            val dx = +2.5f
            val dy = +7.5f
            println(t)
            t.translate(+dx, +dy)
            println(t)
            t.translate(+dx, -dy)
            println(t)
            t.translate(-dx, -dy)
            println(t)
            t.translate(-dx, +dy)
            println(t)
        }
    }

}