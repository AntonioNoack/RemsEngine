package me.anno.utils

import org.joml.Vector2f
import kotlin.math.sqrt


fun clamp(x: Int, min: Int, max: Int) = if(x < min) min else if(x < max) x else max
fun clamp(x: Float, min: Float, max: Float) = if(x < min) min else if(x < max) x else max
fun pow(base: Float, power: Float) = StrictMath.pow(base.toDouble(), power.toDouble()).toFloat()

fun length(dx: Float, dy: Float) = sqrt(dx*dx+dy*dy)
fun length(x0: Float, y0: Float, x1: Float, y1: Float) = length(x1-x0, y1-y0)

operator fun Vector2f.minus(s: Vector2f) = Vector2f(x-s.x, y-s.y)
operator fun Vector2f.plus(s: Vector2f) = Vector2f(x+s.x, y+s.y)
operator fun Vector2f.times(f: Float) = Vector2f(x*f, y*f)

fun avg(a: Vector2f, b: Vector2f, c: Vector2f) = Vector2f((a.x+b.x+c.x)/3f, (a.y+b.y+c.y)/3f)

fun Vector2f.print() = "($x $y)"
fun Vector2f.print(pts: List<Vector2f>) = "${pts.indexOf(this)}"

fun Vector2f.getSideSign(b: Vector2f, c: Vector2f): Float {
    val bx = b.x - x
    val by = b.y - y
    val cx = c.x - x
    val cy = c.y - y
    return cx * by - cy * bx
}

fun Vector2f.isInsideTriangle(a: Vector2f, b: Vector2f, c: Vector2f): Boolean {

    val as_x = x - a.x
    val as_y = y - a.y

    val s_ab = (b.x - a.x) * as_y - (b.y - a.y) * as_x > 0

    if ((c.x - a.x) * as_y - (c.y - a.y) * as_x > 0 == s_ab) return false

    return if ((c.x - b.x) * (y - b.y) - (c.y - b.y) * (x - b.x) > 0 != s_ab) false else true

    var sum = 0

    if(getSideSign(a,b) > 0f) sum++
    if(getSideSign(b,c) > 0f) sum++
    if(getSideSign(c,a) > 0f) sum++

    println(sum)
    println(getSideSign(a,b))
    println(getSideSign(b,c))
    println(getSideSign(c,a))

    // left or right of all lines
    return sum == 0 || sum == 3

}

fun Vector2f.isInsideTriangle2(a: Vector2f, b: Vector2f, c: Vector2f): Boolean {

    var sum = 0

    if(getSideSign(a,b) > 0f) sum++
    if(getSideSign(b,c) > 0f) sum++
    if(getSideSign(c,a) > 0f) sum++

    // left or right of all lines
    return sum == 0

}
