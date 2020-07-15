package me.anno.utils

import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.Vector4f
import kotlin.math.roundToInt
import kotlin.math.sqrt

val GoldenRatio = (1f + sqrt(5f))*0.5f

fun sq(x: Float) = x*x

fun clamp(x: Int, min: Int, max: Int) = if(x < min) min else if(x < max) x else max
fun clamp(x: Float, min: Float, max: Float) = if(x < min) min else if(x < max) x else max
fun clamp(x: Double, min: Double, max: Double) = if(x < min) min else if(x < max) x else max
fun pow(base: Float, power: Float) = StrictMath.pow(base.toDouble(), power.toDouble()).toFloat()

fun length(dx: Float, dy: Float) = sqrt(dx*dx+dy*dy)
fun length(dx: Float, dy: Float, dz: Float) = sqrt(dx*dx+dy*dy+dz*dz)
fun distance(x0: Float, y0: Float, x1: Float, y1: Float) = length(x1-x0, y1-y0)

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

    val asX = x - a.x
    val asY = y - a.y

    val sAb = (b.x - a.x) * asY - (b.y - a.y) * asX > 0

    if ((c.x - a.x) * asY - (c.y - a.y) * asX > 0 == sAb) return false

    return (c.x - b.x) * (y - b.y) - (c.y - b.y) * (x - b.x) > 0 == sAb

    /*var sum = 0

    if(getSideSign(a,b) > 0f) sum++
    if(getSideSign(b,c) > 0f) sum++
    if(getSideSign(c,a) > 0f) sum++

    println(sum)
    println(getSideSign(a,b))
    println(getSideSign(b,c))
    println(getSideSign(c,a))

    // left or right of all lines
    return sum == 0 || sum == 3
*/
}

fun Vector2f.isInsideTriangle2(a: Vector2f, b: Vector2f, c: Vector2f): Boolean {

    var sum = 0

    if(getSideSign(a,b) > 0f) sum++
    if(getSideSign(b,c) > 0f) sum++
    if(getSideSign(c,a) > 0f) sum++

    // left or right of all lines
    return sum == 0

}

fun mix(a: Short, b: Short, f: Double): Double {
    return a * (1f-f) + b*f
}

fun mix(a: Short, b: Short, f: Float): Float {
    return a * (1f-f) + b*f
}

fun mix(a: Float, b: Float, f: Float): Float {
    return a * (1f-f) + b*f
}

fun mix(a: Int, b: Int, f: Float): Int {
    return (a * (1f-f) + b*f).roundToInt()
}

fun mix(a: Int, b: Int, shift: Int, f: Float): Int {
    return mix((a shr shift) and 0xff, (b shr shift) and 0xff, f) shl shift
}

fun mixARGB(a: Int, b: Int, f: Float): Int {
    return mix(a, b, 24, f) or mix(a, b, 16, f) or mix(a, b, 8, f) or mix(a, b, 0, f)
}

fun Vector3f.print() = "($x $y $z)"
fun Vector4f.print() = "($x $y $z $w)"

fun Vector4f.toVec3f() = Vector3f(x/w, y/w, z/w)
operator fun Vector3f.minus(s: Vector3f) = Vector3f(x-s.x, y-s.y, z-s.z)
operator fun Vector3f.plus(s: Vector3f) = Vector3f(x+s.x, y+s.y, z+s.z)
operator fun Vector3f.times(s: Float) = Vector3f(x*s, y*s, z*s)

operator fun Vector4f.minus(s: Vector4f) = Vector4f(x-s.x, y-s.y, z-s.z, w-s.w)
operator fun Vector4f.plus(s: Vector4f) = Vector4f(x+s.x, y+s.y, z+s.z, w+s.w)