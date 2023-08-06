package me.anno.tests.rtrt.other

import org.joml.AABBf
import org.joml.Vector3f
import java.util.Random

fun main() {
    // test raytracing data in uvbaker
    val blas = ("-1 -1 -1 0 1 1 1 10 -1 -1 -1 1 1 1 1 7 -1 -1 -1 2 1 1 1 4 -1 -1 1 3 1 1 1 3 -1 -1 -1 0 1 1 1 6 1 -1 -1 6 1 1 1 3 -1 1 -1 9 1 1 1 3 -1 -1 -1 1 1 1 1 9 -1 -1 -1 12 1 1 -1 3 -1 -1 -1 15 1 -1 1 3 -1 -1 -1 0 1 1 1 14 -1 -1 -1 0 1 1 1 13 1 -1 -1 18 1 1 1 3 -1 1 -1 21 1 1 1 3 -1 -1 -1 2 1 1 1 18 -1 -1 -1 1 1 1 1 17 -1 -1 -1 24 -1 1 1 3 -1 -1 -1 27 1 -1 1 3 -1 -1 -1 2 1 1 1 20 -1 -1 1 30 1 1 1 3 -1 -1 -1 0 1 1 1 22 -1 -1 -1 33 1 1 -1 3 -1 -1 -1 36 -1 1 1 3 0 0 0 0 0 0 0 0 0 0 0 0")
        .split(' ').map { it.toFloat() }.toFloatArray()
    val pos = ("1 -1 1 1 1 1 -1 1 1 1 -1 -1 1 1 -1 1 1 1 1 1 -1 -1 1 1 1 1 1 -1 -1 -1 1 1 -1 1 -1 -1 -1 -1 -1 1 -1 -1 1 -1 1 1 -1 -1 1 1 1 1 -1 1 1 1 -1 -1 1 -1 -1 1 1 -1 -1 1 -1 1 1 -1 1 -1 -1 -1 -1 1 -1 1 -1 -1 1 1 -1 1 -1 1 1 -1 -1 1 -1 -1 -1 -1 1 -1 1 1 -1 -1 -1 1 -1 1 -1 -1 -1 -1")
        .split(' ').map { it.toFloat() }.toFloatArray()

    println("lengths: ${blas.size}, ${pos.size}")

    val rayPos = Vector3f(0f, 0f, 0f)
    val rayDir = Vector3f(0f, 0f, 1f)
    val bounds = AABBf()
    val nextNodeStack = IntArray(64)
    var stackIndex = 0
    var nodeIndex = 0
    while (true) {
        val i8 = nodeIndex * 8
        bounds.setMin(blas[i8], blas[i8 + 1], blas[i8 + 2])
        bounds.setMax(blas[i8 + 4], blas[i8 + 5], blas[i8 + 6])
        val passes = bounds.testRay(rayPos.x, rayPos.y, rayPos.z, rayDir.x, rayDir.y, rayDir.z)
        println("[$nodeIndex] $bounds? $passes")
        if (passes || true) {
            val vx = blas[i8 + 3].toInt()
            val vy = blas[i8 + 7].toInt()
            if (vx < 3) {
                println("[$nodeIndex] split on ${"xyz"[vx]} -> $vy")
                nextNodeStack[stackIndex++] = vy
                nodeIndex++
            } else {
                println("[$nodeIndex] ${vx - 3} += $vy")
                for (i in vx - 3 until vx - 3 + vy) {
                    val i3 = i * 3
                    if (!bounds.testPoint(pos[i3], pos[i3 + 1], pos[i3 + 2])) {
                        println("                        illegal: Point $i/${pos.size/3}@$nodeIndex out of bounds ${pos[i3]},${pos[i3 + 1]},${pos[i3 + 2]}")
                    } else println("ok ${pos[i3]},${pos[i3 + 1]},${pos[i3 + 2]}")
                }
                if (stackIndex == 0) break
                val ni = nextNodeStack[--stackIndex]
                nodeIndex = ni
            }
        } else {
            if (stackIndex == 0) break
            val ni = nextNodeStack[--stackIndex]
            nodeIndex = ni
            Random().nextInt()
        }
    }
}