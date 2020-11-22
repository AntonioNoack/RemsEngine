package me.anno.utils

import org.joml.*

object Vectors {

    operator fun Vector2f.plus(s: Vector2f) = Vector2f(x + s.x, y + s.y)
    operator fun Vector2f.minus(s: Vector2f) = Vector2f(x - s.x, y - s.y)
    operator fun Vector2f.times(f: Float) = Vector2f(x * f, y * f)

    operator fun Vector2d.plus(s: Vector2d) = Vector2d(x + s.x, y + s.y)
    operator fun Vector2d.minus(s: Vector2d) = Vector2d(x - s.x, y - s.y)
    operator fun Vector2d.times(f: Double) = Vector2d(x * f, y * f)

    operator fun Vector3f.plus(s: Vector3f) = Vector3f(x + s.x, y + s.y, z + s.z)
    operator fun Vector3f.minus(s: Vector3f) = Vector3f(x - s.x, y - s.y, z - s.z)
    operator fun Vector3f.times(s: Float) = Vector3f(x * s, y * s, z * s)

    operator fun Vector3i.plus(s: Vector3i) = Vector3i(x + s.x, y + s.y, z + s.z)
    operator fun Vector3i.minus(s: Vector3i) = Vector3i(x - s.x, y - s.y, z - s.z)
    operator fun Vector3i.times(s: Float) = Vector3f(x * s, y * s, z * s)

    operator fun Vector3d.plus(s: Vector3d) = Vector3d(x + s.x, y + s.y, z + s.z)
    operator fun Vector3d.minus(s: Vector3d) = Vector3d(x - s.x, y - s.y, z - s.z)
    operator fun Vector3d.times(s: Double) = Vector3d(x * s, y * s, z * s)

    operator fun Vector3f.plus(s: Vector3i) = Vector3f(x + s.x, y + s.y, z + s.z)
    operator fun Vector3i.plus(s: Vector3f) = Vector3f(x + s.x, y + s.y, z + s.z)
    operator fun Vector3f.minus(s: Vector3i) = Vector3f(x - s.x, y - s.y, z - s.z)
    operator fun Vector3i.minus(s: Vector3f) = Vector3f(x - s.x, y - s.y, z - s.z)

    operator fun Vector4f.minus(s: Vector4f) = Vector4f(x - s.x, y - s.y, z - s.z, w - s.w)
    operator fun Vector4f.plus(s: Vector4f) = Vector4f(x + s.x, y + s.y, z + s.z, w + s.w)
    operator fun Vector4f.times(s: Float) = Vector4f(x * s, y * s, z * s, w * s)

    fun avg(a: Vector2f, b: Vector2f, c: Vector2f) = Vector2f((a.x + b.x + c.x) / 3f, (a.y + b.y + c.y) / 3f)
    fun avg(a: Vector2d, b: Vector2d, c: Vector2d) = Vector2d((a.x + b.x + c.x) / 3f, (a.y + b.y + c.y) / 3f)
    fun avg(a: Vector3f, b: Vector3f, c: Vector3f) =
        Vector3f((a.x + b.x + c.x) / 3f, (a.y + b.y + c.y) / 3f, (a.z + b.z + c.z) / 3f)

    fun Vector2f.print(pts: List<Vector2f>) = "${pts.indexOf(this)}"
    fun Vector2d.print(pts: List<Vector2d>) = "${pts.indexOf(this)}"

    fun Vector2f.getSideSign(b: Vector2f, c: Vector2f): Float {
        val bx = b.x - x
        val by = b.y - y
        val cx = c.x - x
        val cy = c.y - y
        return cx * by - cy * bx
    }

    fun Vector2d.getSideSign(b: Vector2d, c: Vector2d): Double {
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

        if (getSideSign(a, b) > 0f) sum++
        if (getSideSign(b, c) > 0f) sum++
        if (getSideSign(c, a) > 0f) sum++

        // left or right of all lines
        return sum == 0

    }


    fun Vector2d.isInsideTriangle(a: Vector2d, b: Vector2d, c: Vector2d): Boolean {

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

    fun Vector2d.isInsideTriangle2(a: Vector2d, b: Vector2d, c: Vector2d): Boolean {

        var sum = 0

        if (getSideSign(a, b) > 0f) sum++
        if (getSideSign(b, c) > 0f) sum++
        if (getSideSign(c, a) > 0f) sum++

        // left or right of all lines
        return sum == 0

    }

    // https://courses.cs.washington.edu/courses/csep557/10au/lectures/triangle_intersection.pdf
    fun rayTriangleIntersection(
        origin: Vector3f, direction: Vector3f,
        a: Vector3f, b: Vector3f, c: Vector3f,
        maxDistance: Float
    ): Pair<Vector3f, Float>? {
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

    fun Vector2f.print() = "($x $y)"
    fun Vector2d.print() = "($x $y)"
    fun Vector2i.print() = "($x $y)"
    fun Vector3f.print() = "($x $y $z)"
    fun Vector3d.print() = "($x $y $z)"
    fun Vector3i.print() = "($x $y $z)"
    fun Vector4f.print() = "($x $y $z $w)"
    fun Vector4d.print() = "($x $y $z $w)"
    fun Vector4i.print() = "($x $y $z $w)"

    fun Vector4f.toVec3f() = Vector3f(x / w, y / w, z / w)

    fun Vector3f.is000() = x == 0f && y == 0f && z == 0f
    fun Vector3f.is111() = x == 1f && y == 1f && z == 1f
    fun Vector4f.is1111() = x == 1f && y == 1f && z == 1f && w == 1f

    fun Vector3fi(x: Int, y: Int, z: Int) = Vector3f(x.toFloat(), y.toFloat(), z.toFloat())

}