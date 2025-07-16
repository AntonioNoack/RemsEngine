package me.anno.bench

import me.anno.Engine
import me.anno.utils.Clock
import me.anno.utils.pooling.JomlPools
import org.apache.logging.log4j.LogManager
import org.joml.Quaterniond
import org.joml.Vector3d
import kotlin.math.sqrt
import kotlin.random.Random

private val LOGGER = LogManager.getLogger("NormalToQuatY-Bench")

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

    var errOld = 0.0
    var errNew = 0.0
    var errNew2 = 0.0

    val validations = 10000
    repeat(validations) {

        a.set(rnd.nextDouble(), rnd.nextDouble(), rnd.nextDouble()).sub(0.5).normalize()

        normalToQuaternionYOld(a.x, a.y, a.z, q1)
        q2.rotationTo(0.0, 1.0, 0.0, a.x, a.y, a.z)
        q3.rotationYTo2(a.x, a.y, a.z)

        // check accuracy
        errOld += q1.transformInverse(a, b).distanceSquared(0.0, 1.0, 0.0)
        errNew += q2.transformInverse(a, b).distanceSquared(0.0, 1.0, 0.0)
        errNew2 += q3.transformInverse(a, b).distanceSquared(0.0, 1.0, 0.0)
    }

    errOld = sqrt(errOld / validations)
    errNew = sqrt(errNew / validations)
    errNew2 = sqrt(errNew2 / validations)

    // somehow, our old method was pretty inaccurate 🤔
    LOGGER.info("Error: [Old: $errOld, New: $errNew, New2: $errNew2]")

    // generate random vectors for benchmark
    val ai = Array(512) {
        Vector3d(rnd.nextDouble(), rnd.nextDouble(), rnd.nextDouble()).sub(0.5).normalize()
    }

    val clock = Clock(LOGGER)
    clock.benchmark(25_000, 50_000_000, "Old") { idx -> // 15.24 ns/e
        val a = ai[idx.and(511)]
        normalToQuaternionYOld(a.x, a.y, a.z, q1)
    }
    clock.benchmark(25_000, 50_000_000, "New") { idx -> // 6.3 ns/e, so 2.4x faster
        val a = ai[idx.and(511)]
        q2.rotationTo(0.0, 1.0, 0.0, a.x, a.y, a.z)
    }
    clock.benchmark(25_000, 50_000_000, "New2") { idx -> // 6.3 ns/e, so not any faster
        val a = ai[idx.and(511)]
        q2.rotationYTo(a.x, a.y, a.z)
    }
    clock.benchmark(25_000, 50_000_000, "New3") { idx -> // 5.0 ns/e, but forgets normalization
        val a = ai[idx.and(511)]
        q2.rotationYTo2(a.x, a.y, a.z)
    }

    Engine.requestShutdown()
}

fun normalToQuaternionYOld(x: Double, y: Double, z: Double, dst: Quaterniond = Quaterniond()): Quaterniond {
    // to do this works perfectly, but the y-angle shouldn't change :/
    // uses ~ 28 ns/e on R5 2600 in fp32
    if (x * x + z * z > 0.001) {
        val v3 = JomlPools.vec3d
        val v0 = v3.create()
        val v2 = v3.create()
        v0.set(z, 0.0, -x).normalize()
        v0.cross(x, y, z, v2)
        val v00 = v0.x
        val v22 = v2.z
        val diag = v00 + y + v22
        if (diag >= 0.0) {
            dst.set(z - v2.y, v2.x - v0.z, v0.y - x, diag + 1.0)
        } else if (v00 >= y && v00 >= v22) {
            dst.set(v00 - (y + v22) + 1.0, x + v0.y, v0.z + v2.x, z - v2.y)
        } else if (y > v22) {
            dst.set(x + v0.y, y - (v22 + v00) + 1.0, v2.y + z, v2.x - v0.z)
        } else {
            dst.set(v0.z + v2.x, v2.y + z, v22 - (v00 + y) + 1.0, v0.y - x)
        }
        v3.sub(2)
        return dst.normalize()
    } else if (y > 0.0) { // up
        return dst.identity()
    } else { // down
        return dst.set(1.0, 0.0, 0.0, 0.0)
    }
}

fun Quaterniond.rotationYTo(
    toDirX: Double, toDirY: Double, toDirZ: Double
): Quaterniond {
    val toLenSq = Vector3d.lengthSquared(toDirX, toDirY, toDirZ)
    val invLenFactor = sqrt(toLenSq) // ^2 -> ^1
    if (toDirY < -0.999999 * invLenFactor) {
        set(1.0, 0.0, 0.0, 0.0)
    } else {
        val cx = toDirZ
        val cy = 0.0
        val cz = -toDirX
        set(cx, cy, cz, invLenFactor + toDirY)
            .normalize()
    }
    return this
}

// if toDir is already normalized, this could be used.
// isn't much faster though, only 6.5 ns -> 5.1 ns
fun Quaterniond.rotationYTo2(
    toDirX: Double, toDirY: Double, toDirZ: Double
): Quaterniond {
    if (toDirY < -0.999999) {
        set(1.0, 0.0, 0.0, 0.0)
    } else {
        set(toDirZ, 0.0, -toDirX, 1.0 + toDirY)
            .normalize()
    }
    return this
}