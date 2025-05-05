package me.anno.tests.terrain.v1

import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshAttributes.color0
import me.anno.maths.Maths.mix
import me.anno.maths.Maths.sq
import me.anno.tests.terrain.v1.TerrainChunkSystem.Companion.sx
import me.anno.tests.terrain.v1.TerrainChunkSystem.Companion.sz
import me.anno.utils.Color.mixARGB
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min

class TerrainEditor(
    val offsetX: Float,
    val offsetZ: Float,
    val falloffFactor: Float,
    val effect0: Float,
    val mesh: Mesh
) {

    val pos = mesh.positions!!
    val col = mesh.color0!!

    val falloff0 = exp(-9f)

    fun falloff(x: Float, z: Float): Float {
        return max(exp(falloffFactor * sq(x + offsetX, z + offsetZ)) - falloff0, 0f)
    }

    fun paint(color: Int) {
        val strength = 20f * effect0
        for (i in col.indices) {
            val j = i * 3
            val wi = min(strength * falloff(pos[j], pos[j + 2]), 1f)
            col[i] = mixARGB(col[i], color, wi)
        }
    }

    fun smooth() {
        val effect = 5f * effect0
        val dxi = 3
        val dyi = sz * 3
        for (yi in 0 until sx) {
            for (xi in 0 until sz) {
                val i = (xi + yi * sz) * 3 + 1
                var sum = 4f * pos[i]
                var weight = 4f
                if (xi > 0) {
                    sum += pos[i - dxi]
                    weight++
                }
                if (xi < sz - 1) {
                    sum += pos[i + dxi]
                    weight++
                }
                if (yi > 0) {
                    sum += pos[i - dyi]
                    weight++
                }
                if (yi < sx - 1) {
                    sum += pos[i + dyi]
                    weight++
                }
                val wi = min(effect * falloff(pos[i - 1], pos[i + 1]), 1f)
                pos[i] = mix(pos[i], sum / weight, wi)
            }
        }
    }

    fun flatten() {
        var sum = 0f
        var weight = 1e-9f
        for (i in pos.indices step 3) {
            val wi = effect0 * falloff(pos[i], pos[i + 2])
            sum += pos[i + 1] * wi
            weight += wi
        }
        val avg = sum / weight
        for (i in pos.indices step 3) {
            val wi = min(effect0 * falloff(pos[i], pos[i + 2]), 1f)
            pos[i + 1] = mix(pos[i + 1], avg, wi)
        }
    }

    fun adding(strength: Float) {
        for (i in pos.indices step 3) {
            pos[i + 1] += strength * falloff(pos[i], pos[i + 2])
        }
    }
}
