package me.anno.utils.types

import org.joml.*

object Vectors {

    operator fun Vector2fc.plus(s: Vector2fc) = Vector2f(x() + s.x(), y() + s.y())
    operator fun Vector2fc.minus(s: Vector2fc) = Vector2f(x() - s.x(), y() - s.y())
    operator fun Vector2fc.times(f: Float) = Vector2f(x() * f, y() * f)
    operator fun Vector2fc.times(s: Vector2fc) = Vector2f(x() * s.x(), y() * s.y())

    operator fun Vector2dc.plus(s: Vector2dc) = Vector2d(x() + s.x(), y() + s.y())
    operator fun Vector2dc.minus(s: Vector2dc) = Vector2d(x() - s.x(), y() - s.y())
    operator fun Vector2dc.times(f: Double) = Vector2d(x() * f, y() * f)

    operator fun Vector3fc.plus(s: Vector3fc) = Vector3f(x() + s.x(), y() + s.y(), z() + s.z())
    operator fun Vector3fc.minus(s: Vector3fc) = Vector3f(x() - s.x(), y() - s.y(), z() - s.z())
    operator fun Vector3fc.times(s: Float) = Vector3f(x() * s, y() * s, z() * s)
    operator fun Vector3fc.times(s: Vector3fc) = Vector3f(x() * s.x(), y() * s.y(), z() * s.z())

    operator fun Vector3ic.plus(s: Vector3ic) = Vector3i(x() + s.x(), y() + s.y(), z() + s.z())
    operator fun Vector3ic.minus(s: Vector3ic) = Vector3i(x() - s.x(), y() - s.y(), z() - s.z())
    operator fun Vector3ic.times(s: Float) = Vector3f(x() * s, y() * s, z() * s)

    operator fun Vector3dc.plus(s: Vector3dc) = Vector3d(x() + s.x(), y() + s.y(), z() + s.z())
    operator fun Vector3dc.minus(s: Vector3dc) = Vector3d(x() - s.x(), y() - s.y(), z() - s.z())
    operator fun Vector3dc.times(s: Double) = Vector3d(x() * s, y() * s, z() * s)

    operator fun Vector3fc.plus(s: Vector3ic) = Vector3f(x() + s.x(), y() + s.y(), z() + s.z())
    operator fun Vector3ic.plus(s: Vector3fc) = Vector3f(x() + s.x(), y() + s.y(), z() + s.z())
    operator fun Vector3fc.minus(s: Vector3ic) = Vector3f(x() - s.x(), y() - s.y(), z() - s.z())
    operator fun Vector3ic.minus(s: Vector3fc) = Vector3f(x() - s.x(), y() - s.y(), z() - s.z())

    operator fun Vector4fc.minus(s: Vector4fc) = Vector4f(x() - s.x(), y() - s.y(), z() - s.z(), w() - s.w())
    operator fun Vector4fc.plus(s: Vector4fc) = Vector4f(x() + s.x(), y() + s.y(), z() + s.z(), w() + s.w())
    operator fun Vector4fc.plus(s: Float) = if (s == 0f) this else Vector4f(x() + s, y() + s, z() + s, w() + s)
    operator fun Vector4fc.times(s: Float) = Vector4f(x() * s, y() * s, z() * s, w() * s)
    operator fun Vector4fc.times(s: Vector4fc) = Vector4f(x() * s.x(), y() * s.y(), z() * s.z(), w() * s.w())

    fun Vector4fc.mulAlpha(m: Float, dst: Vector4f = Vector4f()) = dst.set(x(), y(), z(), w() * m)

    fun avg(a: Vector2fc, b: Vector2f) = Vector2f(a).add(b).mul(0.5f)
    fun avg(a: Vector2dc, b: Vector2d) = Vector2d(a).add(b).mul(0.5)
    fun avg(a: Vector3fc, b: Vector3f) = Vector3f(a).add(b).mul(0.5f)

    fun avg(a: Vector2fc, b: Vector2fc, c: Vector2fc) = Vector2f((a.x() + b.x() + c.x()) / 3f, (a.y() + b.y() + c.y()) / 3f)
    fun avg(a: Vector2dc, b: Vector2dc, c: Vector2dc) = Vector2d((a.x() + b.x() + c.x()) / 3f, (a.y() + b.y() + c.y()) / 3f)
    fun avg(a: Vector3fc, b: Vector3fc, c: Vector3fc) =
        Vector3f((a.x() + b.x() + c.x()) / 3f, (a.y() + b.y() + c.y()) / 3f, (a.z() + b.z() + c.z()) / 3f)

    fun Vector2fc.print(pts: List<Vector2fc>) = "${pts.indexOf(this)}"
    fun Vector2dc.print(pts: List<Vector2dc>) = "${pts.indexOf(this)}"

    fun Vector2fc.getSideSign(b: Vector2fc, c: Vector2fc): Float {
        val bx = b.x() - x()
        val by = b.y() - y()
        val cx = c.x() - x()
        val cy = c.y() - y()
        return cx * by - cy * bx
    }

    fun Vector2dc.getSideSign(b: Vector2dc, c: Vector2dc): Double {
        val bx = b.x() - x()
        val by = b.y() - y()
        val cx = c.x() - x()
        val cy = c.y() - y()
        return cx * by - cy * bx
    }

    fun Vector2fc.isInsideTriangle(a: Vector2fc, b: Vector2fc, c: Vector2fc): Boolean {

        val asX = x() - a.x()
        val asY = y() - a.y()

        val sAb = (b.x() - a.x()) * asY - (b.y() - a.y()) * asX > 0

        if ((c.x() - a.x()) * asY - (c.y() - a.y()) * asX > 0 == sAb) return false

        return (c.x() - b.x()) * (y() - b.y()) - (c.y() - b.y()) * (x() - b.x()) > 0 == sAb

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

    fun Vector2fc.isInsideTriangle2(a: Vector2fc, b: Vector2fc, c: Vector2fc): Boolean {

        var sum = 0

        if (getSideSign(a, b) > 0f) sum++
        if (getSideSign(b, c) > 0f) sum++
        if (getSideSign(c, a) > 0f) sum++

        // left or right of all lines
        return sum == 0

    }


    fun Vector2d.isInsideTriangle(a: Vector2dc, b: Vector2dc, c: Vector2dc): Boolean {

        val asX = x() - a.x()
        val asY = y() - a.y()

        val sAb = (b.x() - a.x()) * asY - (b.y() - a.y()) * asX > 0

        if ((c.x() - a.x()) * asY - (c.y() - a.y()) * asX > 0 == sAb) return false

        return (c.x() - b.x()) * (y() - b.y()) - (c.y() - b.y()) * (x() - b.x()) > 0 == sAb

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

    fun Vector2dc.isInsideTriangle2(a: Vector2dc, b: Vector2dc, c: Vector2dc): Boolean {

        var sum = 0

        if (getSideSign(a, b) > 0f) sum++
        if (getSideSign(b, c) > 0f) sum++
        if (getSideSign(c, a) > 0f) sum++

        // left or right of all lines
        return sum == 0

    }

    // https://courses.cs.washington.edu/courses/csep557/10au/lectures/triangle_intersection.pdf
    fun rayTriangleIntersection(
        origin: Vector3fc, direction: Vector3fc,
        a: Vector3fc, b: Vector3fc, c: Vector3fc,
        maxDistance: Float
    ): Pair<Vector3fc, Float>? {
        val ba = b - a
        val ca = c - a
        val n = ba.cross(ca, Vector3f())
        val d = n.dot(a)
        val t = (d - n.dot(origin)) / n.dot(direction)
        if (t < 0f || t >= maxDistance) return null
        val q = origin + direction * t
        if ((b - a).cross(q - a).dot(n) < 0) return null
        if ((c - b).cross(q - b).dot(n) < 0) return null
        if ((a - c).cross(q - c).dot(n) < 0) return null
        return q to t
    }

    fun Int.byteToHex() = (256 or this).toString(16).substring(1)
    fun Vector3f.toHex() =
        "#${(x * 255).toInt().byteToHex()}${(y * 255).toInt().byteToHex()}${(z * 255).toInt().byteToHex()}"

    fun Vector2fc.print() = "(${x()} ${y()})"
    fun Vector2dc.print() = "(${x()} ${y()})"
    fun Vector2ic.print() = "(${x()} ${y()})"
    fun Vector3fc.print() = "(${x()} ${y()} ${z()})"
    fun Vector3dc.print() = "(${x()} ${y()} ${z()})"
    fun Vector3ic.print() = "(${x()} ${y()} ${z()})"
    fun Vector4fc.print() = "(${x()} ${y()} ${z()} ${w()})"
    fun Vector4dc.print() = "(${x()} ${y()} ${z()} ${w()})"
    fun Vector4ic.print() = "(${x()} ${y()} ${z()} ${w()})"

    fun Vector4fc.toVec3f(): Vector3f {
        val w = w()
        return Vector3f(x() / w, y() / w, z() / w)
    }

    fun Vector3f.is000() = x == 0f && y == 0f && z == 0f
    fun Vector3f.is111() = x == 1f && y == 1f && z == 1f
    fun Vector4f.is1111() = x == 1f && y == 1f && z == 1f && w == 1f

    fun Vector3fc.is000() = x() == 0f && y() == 0f && z() == 0f
    fun Vector3fc.is111() = x() == 1f && y() == 1f && z() == 1f
    fun Vector4fc.is1111() = x() == 1f && y() == 1f && z() == 1f && w() == 1f

    fun Vector3fi(x: Int, y: Int, z: Int) = Vector3f(x.toFloat(), y.toFloat(), z.toFloat())

    val Vector3f.yzx get() = Vector3f(y, z, x)
    val Vector3f.zxy get() = Vector3f(z, x, y)

    fun DoubleArray.toVec3() = Vector3f(this[0].toFloat(), this[1].toFloat(), this[2].toFloat())

}