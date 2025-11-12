package me.anno.bench

import com.bulletphysics.BulletGlobals
import cz.advel.stack.Stack
import me.anno.Engine
import me.anno.maths.Maths.sq
import me.anno.utils.Clock
import me.anno.utils.assertions.assertEquals
import org.apache.logging.log4j.LogManager
import org.joml.JomlMath
import org.joml.Quaterniond
import org.joml.Vector3d
import kotlin.math.sqrt
import kotlin.random.Random

private val LOGGER = LogManager.getLogger("RotationTo-Bench")

/**
 * Check whether our new methods with fewer sqrts is accurate and faster -> yes, it is :)
 * */
fun main() {

    val rnd = Random(1234)
    val a = Vector3d()
    val b = Vector3d()

    val q1 = Quaterniond()
    val q2 = Quaterniond()
    val q3 = Quaterniond()

    val t = Vector3d()
    var errOld = 0.0
    var errNew = 0.0
    var errBullet = 0.0

    var errOld2 = 0.0
    var errNew2 = 0.0

    val validations = 10000
    repeat(validations) {
        a.set(rnd.nextDouble(), rnd.nextDouble(), rnd.nextDouble()).sub(0.5)
        b.set(rnd.nextDouble(), rnd.nextDouble(), rnd.nextDouble()).sub(0.5)
        q1.rotationToOld(a.x, a.y, a.z, b.x, b.y, b.z)
        q2.rotationTo(a.x, a.y, a.z, b.x, b.y, b.z)

        // needs normalized vectors as inputs
        rotationToJBullet(a.normalize(), b.normalize(), q3)

        assertEquals(q1, q2, 1e-14)
        assertEquals(q1, q3, 1e-12)

        // check accuracy
        errOld += q1.transform(a, t).distanceSquared(b)
        errNew += q2.transform(a, t).distanceSquared(b)
        errBullet += q3.transform(a, t).distanceSquared(b)

        // validate new rotateTo method
        q1.rotateToOld(b.x, b.y, b.z, a.x, a.y, b.z)
        q2.rotateTo(b, a)

        errOld2 += sq(q1.angle())
        errNew2 += sq(q2.angle())

    }

    errOld = sqrt(errOld / validations)
    errNew = sqrt(errNew / validations)
    errBullet = sqrt(errBullet / validations)

    errOld2 = sqrt(errOld2 / validations)
    errNew2 = sqrt(errNew2 / validations)

    // our new methods seems to have slightly better accuracy :) (10%)
    LOGGER.info("RotationTo-Error: [Old: $errOld, Bullet: $errBullet, New: $errNew]")
    LOGGER.info("RotateTo-Error: [Old: $errOld2, New: $errNew2]")

    a.set(1.0, 0.0, 0.0)
    b.set(0.0, 1.0, 0.0)

    val clock = Clock(LOGGER)
    clock.benchmark(25_000, 50_000_000, "JOML") {
        // 6.9 ns/e -> I didn't expect it to be so fast XD
        // -> but that is because it assumes a and b to be normalized, and doesn't normalize the result
        rotationToJBullet(a, b, q1)
    }
    clock.benchmark(25_000, 50_000_000, "Old") { // 15.8 ns/e
        q1.rotationToOld(a.x, a.y, a.z, b.x, b.y, b.z)
    }
    clock.benchmark(25_000, 50_000_000, "New") { // 7.9 ns/e, so 2.2x faster
        q1.rotationTo(a.x, a.y, a.z, b.x, b.y, b.z)
    }

    clock.benchmark(25_000, 50_000_000, "Old/2") { // 26.2 ns/e
        q1.rotateToOld(a.x, a.y, a.z, b.x, b.y, b.z)
    }
    clock.benchmark(25_000, 50_000_000, "New/2") { // 10.5 ns/e, so 2.5x faster :)
        q1.rotateTo(a.x, a.y, a.z, b.x, b.y, b.z)
    }

    Engine.requestShutdown()
}

/**
 * Original JOML implementation;
 * Uses four sqrts and four divisions in the standard path.
 * My new version only uses two sqrts and one division.
 * */
fun Quaterniond.rotationToOld(
    fromDirX: Double, fromDirY: Double, fromDirZ: Double,
    toDirX: Double, toDirY: Double, toDirZ: Double
): Quaterniond {
    val fn = JomlMath.invsqrt(fromDirX * fromDirX + fromDirY * fromDirY + fromDirZ * fromDirZ)
    val tn = JomlMath.invsqrt(toDirX * toDirX + toDirY * toDirY + toDirZ * toDirZ)
    val fx = fromDirX * fn
    val fy = fromDirY * fn
    val fz = fromDirZ * fn
    val tx = toDirX * tn
    val ty = toDirY * tn
    val tz = toDirZ * tn
    val dot = fx * tx + fy * ty + fz * tz
    var x: Double
    var y: Double
    var z: Double
    val w: Double
    if (dot < -0.999999) {
        // from and to are opposite to each other ->
        //  there's multiple solutions, pick any
        x = fy
        y = -fx
        z = 0.0
        if (fy * fy + y * y == 0.0) {
            x = 0.0
            y = fz
            z = -fy
        }
        set(x, y, z, 0.0)
    } else {
        val sd2 = sqrt((1.0 + dot) * 2.0)
        val isd2 = 1.0 / sd2
        val cx = fy * tz - fz * ty
        val cy = fz * tx - fx * tz
        val cz = fx * ty - fy * tx
        x = cx * isd2
        y = cy * isd2
        z = cz * isd2
        w = sd2 * 0.5
        set(x, y, z, w).normalize()
    }
    return this
}

/**
 * Game Programming Gems 2.10. make sure v0,v1 are normalized
 * from jBullet
 * @author jezek2
 * */
fun rotationToJBullet(v0: Vector3d, v1: Vector3d, out: Quaterniond): Quaterniond {
    val d = v0.dot(v1)
    if (d < -1.0 + BulletGlobals.FLT_EPSILON) {
        // just pick any vector
        out.set(0.0, 1.0, 0.0, 0.0)
        return out
    }

    val s = sqrt((1.0 + d) * 2.0)
    val rs = 1.0 / s
    val c = Stack.borrowVec3d()
    v0.cross(v1, c)
    out.set(c.x * rs, c.y * rs, c.z * rs, s * 0.5)

    return out
}

fun Quaterniond.rotateToOld(
    fromDirX: Double, fromDirY: Double, fromDirZ: Double,
    toDirX: Double, toDirY: Double, toDirZ: Double,
    dst: Quaterniond = this
): Quaterniond {
    // there is an error somewhere in this, but I really don't know where :/
    val fn = JomlMath.invsqrt(fromDirX * fromDirX + fromDirY * fromDirY + fromDirZ * fromDirZ)
    val tn = JomlMath.invsqrt(toDirX * toDirX + toDirY * toDirY + toDirZ * toDirZ)
    val fx = fromDirX * fn
    val fy = fromDirY * fn
    val fz = fromDirZ * fn
    val tx = toDirX * tn
    val ty = toDirY * tn
    val tz = toDirZ * tn
    val dot = fx * tx + fy * ty + fz * tz
    var x: Double
    var y: Double
    var z: Double
    var w: Double
    if (dot < -0.999999) {
        x = fy
        y = -fx
        z = 0.0
        w = 0.0
        if (fy * fy + y * y == 0.0) {
            x = 0.0
            y = fz
            z = -fy
            w = 0.0
        }
    } else {
        val sd2 = sqrt((1.0 + dot) * 2.0)
        val isd2 = 1.0 / sd2
        val cx = fy * tz - fz * ty
        val cy = fz * tx - fx * tz
        val cz = fx * ty - fy * tx
        x = cx * isd2
        y = cy * isd2
        z = cz * isd2
        w = sd2 * 0.5
        val n2 = JomlMath.invsqrt(x * x + y * y + z * z + w * w)
        x *= n2
        y *= n2
        z *= n2
        w *= n2
    }
    return dst.set(
        this.w * x + this.x * w + this.y * z - this.z * y,
        this.w * y - this.x * z + this.y * w + this.z * x,
        this.w * z + this.x * y - this.y * x + this.z * w,
        this.w * w - this.x * x - this.y * y - this.z * z
    )
}
