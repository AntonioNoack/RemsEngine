package me.anno.tests.maths.noise

import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.terrain.DefaultNormalMap
import me.anno.ecs.components.mesh.terrain.HeightMap
import me.anno.ecs.components.mesh.terrain.RectangleTerrainModel
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.maths.Maths.clamp01
import me.anno.maths.Maths.pow
import me.anno.maths.Smoothstep.smoothstep
import me.anno.maths.noise.ErosionNoise
import me.anno.maths.noise.PerlinNoise
import me.anno.maths.noise.PhacelleHash
import me.anno.maths.noise.PhacelleNoise
import me.anno.utils.Color.toRGB
import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.Vector4f
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max

/**
 * create terrain based on erosion noise,
 * as a baseline use fullnoise/perlin-noise
 * */
fun main() {

    // todo find good-looking parameters
    // todo color the terrain like in https://www.shadertoy.com/view/sf23W1
    val base = PerlinNoise(1234, 2, 0.5f, 0f, 1f)
    val noise = object : ErosionNoise(PhacelleNoise(PhacelleHash())) {
        val tmp1 = Vector2f()
        val tmp2 = Vector2f()
        override fun sampleTerrain(px: Float, py: Float, dst: Vector3f): Vector3f {
            val h = base.getSmoothGradient(px, py, tmp1, tmp2)
            return dst.set(h, tmp2.x, tmp2.y)
        }
    }
    // noise.cellSize *= 0.1f
    // noise.scale *= 0.1f

    // todo bug: the normals are all (0,1,0) :/

    val cellSize = 0.01f
    val heightMap = HeightMap { x, y ->
        val height = noise[x * cellSize, y * cellSize]
        max(height, noise.waterHeight)
    }
    val tmpColor = Vector3f()
    val data = Vector4f()
    val mesh = RectangleTerrainModel.generateRegularQuadHeightMesh(
        512, 512,
        false, cellSize, Mesh(), heightMap,
        DefaultNormalMap(heightMap, cellSize, false)
    ) { x, y, height, normal ->
        noise[x * cellSize, y * cellSize, data]
        val color = if (height > noise.waterHeight) {
            calculateSurfaceColor(
                noise, height, normal,
                data.y, data.z, data.w,
                0f, tmpColor
            )
        } else {
            calculateWaterColor(
                height, normal,
                0f, tmpColor
            )
        }
        color.toRGB()
    }
    mesh.calculateNormals(false) // this should not be necessary
    testSceneWithUI("ErosionNoise", mesh)
}

fun calculateSurfaceColor(
    noise: ErosionNoise,
    posY: Float,
    normal: Vector3f,
    erosion: Float,
    ridgeMap: Float,
    trees: Float,
    breakup: Float, // detail noise (optional, can be 0f)
    diffuseColor: Vector3f
): Vector3f {

    val occlusion = clamp01(erosion + 0.5f)

    // -------------------------
    // Cliffs / Dirt
    // -------------------------
    diffuseColor.set(CLIFF_COLOR)
        .lerp(
            DIRT_COLOR,
            smoothstep(0.6f, 0.0f, occlusion + breakup * 1.5f)
        )

    diffuseColor.lerp(
        CLIFF_COLOR,
        smoothstep(0.4f, 0.52f, posY)
    )

    // -------------------------
    // Snow
    // -------------------------
    diffuseColor.lerp(
        Vector3f(1f),
        smoothstep(0.53f, 0.6f, posY + breakup * 0.1f)
    )

    // -------------------------
    // Sand (if using water)
    // -------------------------
    val waterHeight = noise.waterHeight
    diffuseColor.lerp(
        SAND_COLOR,
        smoothstep(
            waterHeight + 0.005f,
            waterHeight,
            posY + breakup * 0.01f
        )
    )

    // -------------------------
    // Grass
    // -------------------------
    val grassMix = Vector3f(GRASS_COLOR1).lerp(
        GRASS_COLOR2,
        smoothstep(0.4f, 0.6f, posY - erosion * 0.05f + breakup * 0.3f)
    )

    val grassHeight = noise.grassHeight
    val grassMask =
        smoothstep(
            grassHeight + 0.05f,
            grassHeight + 0.02f,
            posY + 0.01f + (occlusion - 0.8f) * 0.05f - breakup * 0.02f
        ) * smoothstep(
            0.8f, 1.0f,
            1f - (1f - normal.y) * (1f - trees) + breakup * 0.1f
        )

    diffuseColor.lerp(grassMix, grassMask)

    // -------------------------
    // Trees
    // -------------------------
    val treeFactor = clamp01(trees * 2.2f - 0.8f) * 0.6f
    val treeColor = Vector3f(TREE_COLOR).mul(pow(trees, 8f))

    diffuseColor.lerp(treeColor, treeFactor)

    // -------------------------
    // Breakup noise
    // -------------------------
    diffuseColor.mul(1f + breakup * 0.5f)

    // -------------------------
    // Drainage
    // -------------------------
    val drainageWidth = 0.3f
    val drainage = clamp01((1f - clamp01(ridgeMap / drainageWidth)) * 1.5f)
    diffuseColor.lerp(Vector3f(1f), drainage)

    return diffuseColor
}

fun calculateStrataColor(posY: Float, diffuseColor: Vector3f): Vector3f {
    val strata = Vector3f(
        cos(posY * 130f),
        cos(posY * 190f),
        cos(posY * 250f)
    )

    strata.set(
        smoothstep(0f, 1f, strata.x),
        smoothstep(0f, 1f, strata.y),
        smoothstep(0f, 1f, strata.z)
    )

    diffuseColor.set(0.3f)

    diffuseColor.lerp(Vector3f(0.50f), strata.x)
    diffuseColor.lerp(Vector3f(0.55f), strata.y)
    diffuseColor.lerp(Vector3f(0.60f), strata.z)

    return diffuseColor.mul(
        exp(posY * 10f),
        exp(posY * 10f) * 0.9f,
        exp(posY * 10f) * 0.7f
    )
}

fun calculateWaterColor(
    posY: Float,
    normal: Vector3f,
    breakup: Float, // detail noise (optional, can be 0f)
    diffuseColor: Vector3f,
): Vector3f {
    val shore = if (normal.y > 1e-2f) exp(-posY * 60f) else 0f
    val foam = if (normal.y > 1e-2f)
        smoothstep(0.005f, 0.0f, posY + breakup * 0.005f)
    else 0f

    WATER_COLOR.mix(WATER_SHORE_COLOR, shore, diffuseColor)
    return diffuseColor.lerp(Vector3f(1f), foam)
}


val CLIFF_COLOR = Vector3f(0.22, 0.2, 0.2)
val DIRT_COLOR = Vector3f(0.6, 0.5, 0.4)
val TREE_COLOR = Vector3f(0.12, 0.26, 0.1)
val GRASS_COLOR1 = Vector3f(0.15, 0.3, 0.1)
val GRASS_COLOR2 = Vector3f(0.4, 0.5, 0.2)
val SAND_COLOR = Vector3f(0.8, 0.7, 0.6)

val WATER_COLOR = Vector3f(0.0, 0.05, 0.1)
val WATER_SHORE_COLOR = Vector3f(0.0, 0.25, 0.25)