package me.anno.ecs.components.mesh.terrain.v2

import me.anno.Time
import me.anno.ecs.annotations.ExtendableEnum
import me.anno.ecs.components.mesh.terrain.TerrainBrush
import me.anno.ecs.components.mesh.terrain.v2.TriTerrainChunk.Companion.falloff
import me.anno.input.Input
import me.anno.language.translation.NameDesc
import me.anno.maths.Maths.dtTo01
import me.anno.maths.Maths.dtTo10
import me.anno.maths.Maths.max
import me.anno.maths.Maths.mix
import me.anno.maths.Maths.pow
import me.anno.maths.noise.FullNoise
import kotlin.math.abs
import kotlin.math.sqrt

@Suppress("unused")
abstract class BrushMode(override val nameDesc: NameDesc, val rotate: Boolean) : ExtendableEnum {

    abstract fun createBrush(): TerrainBrush

    override val values: List<ExtendableEnum>
        get() = Companion.values

    companion object {

        val values = ArrayList<BrushMode>()

        val SMOOTHEN = object : BrushMode(NameDesc("Smoothen"), true) {
            override fun createBrush(): TerrainBrush {
                val strength = dtTo10(2f * Time.deltaTime).toFloat()
                return TerrainBrush { it.y = mix(it.y, it.y * strength, falloff(it)) }
            }
        }

        val FLATTEN = object : BrushMode(NameDesc("Flatten"), false) {
            override fun createBrush(): TerrainBrush {
                val strength = dtTo10(2f * Time.deltaTime).toFloat()
                return TerrainBrush { it.y = mix(it.y, it.y * strength, falloff(it)) }
            }
        }

        val ADDITIVE = object : BrushMode(NameDesc("Additive"), true) {
            override fun createBrush(): TerrainBrush {
                val strength = 0.3f * Time.deltaTime.toFloat() * (if (Input.isShiftDown) -1f else +1f)
                return TerrainBrush { it.y += strength * pow(falloff(it), 4f) } // higher falloff to make is sharper
            }
        }

        val PYRAMID = object : BrushMode(NameDesc("Pyramid"), false) {
            override fun createBrush(): TerrainBrush {
                val strength = 0.3f * Time.deltaTime.toFloat() * (if (Input.isShiftDown) -1f else +1f)
                return TerrainBrush {
                    val pyramidShape = max(1f - max(abs(it.x), abs(it.z)), 0f)
                    it.y += strength * pyramidShape
                }
            }
        }

        val SPHERE = object : BrushMode(NameDesc("Sphere"), false) {
            override fun createBrush(): TerrainBrush { // edge has sharp falloff -> looks weird
                val strength = dtTo01(Time.deltaTime.toFloat()) * (if (Input.isShiftDown) -1f else +1f)
                return TerrainBrush {
                    val sphereShape = sqrt(max(0.05f - (it.x * it.x + it.z * it.z), 0f))
                    it.y = mix(it.y, sphereShape, strength * pow(falloff(it), 4f))
                }
            }
        }

        // sharp edge/step? -> won't work properly

        // swirl for testing
        val SWIRL = object : BrushMode(NameDesc("Swirl"), false) {
            override fun createBrush(): TerrainBrush {
                val factor = 0.5f * Time.deltaTime.toFloat() * (if (Input.isShiftDown) -1f else +1f)
                return TerrainBrush { it.rotateY(factor * falloff(it)) }
            }
        }

        val RANDOM = object : BrushMode(NameDesc("Random"), true) {
            override fun createBrush(): TerrainBrush {
                val strength = 0.02f * Time.deltaTime.toFloat() * (if (Input.isShiftDown) -1f else +1f)
                val noise = FullNoise(1024)
                return TerrainBrush {
                    val noise11 = noise.getSmooth(it.x * 100f, it.z * 100f) - 0.5f
                    it.y += strength * noise11 * falloff(it)
                }
            }
        }

        init {
            values.addAll(
                listOf(
                    SMOOTHEN, FLATTEN, ADDITIVE,
                    PYRAMID, SPHERE, SWIRL, RANDOM
                )
            )
        }
    }
}