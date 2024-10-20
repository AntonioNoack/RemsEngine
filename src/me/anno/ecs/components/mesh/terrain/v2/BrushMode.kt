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
abstract class BrushMode(override val nameDesc: NameDesc, override val id: Int, val rotate: Boolean) : ExtendableEnum {

    abstract fun createBrush(): TerrainBrush

    override val values: List<ExtendableEnum>
        get() = Companion.values

    override fun toString(): String {
        return nameDesc.name
    }

    companion object {

        val values = ArrayList<BrushMode>()

        val ADDITIVE = object : BrushMode(NameDesc("Additive"), 0, true) {
            override fun createBrush(): TerrainBrush {
                val strength = 0.3f * Time.deltaTime.toFloat() * (if (Input.isShiftDown) -1f else +1f)
                return TerrainBrush { it.y += strength * pow(falloff(it), 4f) } // higher falloff to make is sharper
            }
        }

        val SMOOTHEN = object : BrushMode(NameDesc("Smoothen"), 1, true) {
            override fun createBrush(): TerrainBrush {
                val strength = dtTo10(2f * Time.deltaTime).toFloat()
                return TerrainBrush { it.y = mix(it.y, it.y * strength, falloff(it)) }
            }
        }

        val FLATTEN = object : BrushMode(NameDesc("Flatten"), 2, false) {
            override fun createBrush(): TerrainBrush {
                val strength = dtTo10(2f * Time.deltaTime).toFloat()
                return TerrainBrush { it.y = mix(it.y, it.y * strength, falloff(it)) }
            }
        }

        val PYRAMID = object : BrushMode(NameDesc("Pyramid"), 3, false) {
            override fun createBrush(): TerrainBrush {
                val strength = 0.3f * Time.deltaTime.toFloat() * (if (Input.isShiftDown) -1f else +1f)
                return TerrainBrush {
                    val pyramidShape = max(1f - max(abs(it.x), abs(it.z)), 0f)
                    it.y += strength * pyramidShape
                }
            }
        }

        val SPHERE = object : BrushMode(NameDesc("Sphere"), 4, false) {
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
        val SWIRL = object : BrushMode(NameDesc("Swirl"), 5, false) {
            override fun createBrush(): TerrainBrush {
                val factor = 0.5f * Time.deltaTime.toFloat() * (if (Input.isShiftDown) -1f else +1f)
                return TerrainBrush { it.rotateY(factor * falloff(it)) }
            }
        }

        val RANDOM = object : BrushMode(NameDesc("Random"), 6, true) {
            override fun createBrush(): TerrainBrush {
                val strength = 0.07f * Time.deltaTime.toFloat() * (if (Input.isShiftDown) -1f else +1f)
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
                    ADDITIVE, SMOOTHEN, FLATTEN,
                    PYRAMID, SPHERE, SWIRL, RANDOM
                )
            )
        }
    }
}