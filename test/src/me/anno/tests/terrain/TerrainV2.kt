package me.anno.tests.terrain

import me.anno.Time
import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.terrain.TerrainBrush
import me.anno.ecs.components.mesh.terrain.v2.TriTerrainChunk
import me.anno.ecs.components.mesh.terrain.v2.TriTerrainComponent
import me.anno.ecs.interfaces.CustomEditMode
import me.anno.engine.raycast.RayQuery
import me.anno.engine.raycast.Raycast
import me.anno.engine.serialization.SerializedProperty
import me.anno.engine.ui.render.RenderView
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.input.Input
import me.anno.maths.Maths.dtTo01
import me.anno.maths.Maths.dtTo10
import me.anno.maths.Maths.max
import me.anno.maths.Maths.mix
import me.anno.maths.Maths.pow
import me.anno.maths.noise.FullNoise
import me.anno.maths.noise.PerlinNoise
import me.anno.utils.callbacks.F2F
import me.anno.utils.types.Vectors.normalToQuaternionY
import org.apache.logging.log4j.LogManager
import org.joml.AABBf
import org.joml.Matrix4x3f
import org.joml.Quaternionf
import org.joml.Vector2f
import org.joml.Vector2i
import org.joml.Vector3f
import org.joml.Vector4f
import kotlin.math.abs
import kotlin.math.sqrt

// todo gpu-accelerated editing
// todo LODs
// todo (de)serialization

private val LOGGER = LogManager.getLogger("TerrainV2")

fun falloff(v: Vector3f): Float {
    return TriTerrainChunk.falloff(v)
}

fun falloff(v: Float): Float {
    return TriTerrainChunk.falloff(v)
}

enum class BrushMode(val rotate: Boolean, val createBrush: () -> TerrainBrush) {

    SMOOTHEN(true, {
        val strength = dtTo10(2f * Time.deltaTime).toFloat()
        TerrainBrush { it.y = mix(it.y, it.y * strength, falloff(it)) }
    }),

    FLATTEN(false, {
        val strength = dtTo10(2f * Time.deltaTime).toFloat()
        TerrainBrush { it.y = mix(it.y, it.y * strength, falloff(it)) }
    }),

    ADDITIVE(true, {
        val strength = 0.3f * Time.deltaTime.toFloat() * (if (Input.isShiftDown) -1f else +1f)
        TerrainBrush { it.y += strength * pow(falloff(it), 4f) } // higher falloff to make is sharper
    }),

    PYRAMID(false, {
        val strength = 0.3f * Time.deltaTime.toFloat() * (if (Input.isShiftDown) -1f else +1f)
        TerrainBrush {
            val pyramidShape = max(1f - max(abs(it.x), abs(it.z)), 0f)
            it.y += strength * pyramidShape
        }
    }),

    SPHERE(false, { // edge has sharp falloff -> looks weird
        val strength = dtTo01(Time.deltaTime.toFloat()) * (if (Input.isShiftDown) -1f else +1f)
        TerrainBrush {
            val sphereShape = sqrt(max(0.05f - (it.x * it.x + it.z * it.z), 0f))
            it.y = mix(it.y, sphereShape, strength * pow(falloff(it), 4f))
        }
    }),

    // sharp edge/step? -> won't work properly

    // swirl for testing
    SWIRL(false, {
        val factor = 0.5f * Time.deltaTime.toFloat() * (if (Input.isShiftDown) -1f else +1f)
        TerrainBrush { it.rotateY(factor * falloff(it)) }
    }),

    RANDOM(true, {
        val strength = 0.02f * Time.deltaTime.toFloat() * (if (Input.isShiftDown) -1f else +1f)
        val noise = FullNoise(1024)
        TerrainBrush {
            val noise11 = noise.getSmooth(it.x * 100f, it.z * 100f) - 0.5f
            it.y += strength * noise11 * falloff(it)
        }
    })
}

class TerrainEditModeV2(val terrain: TriTerrainComponent) : Component(), CustomEditMode {

    @SerializedProperty
    var brushMode = BrushMode.FLATTEN

    private var currTime = 0L
    override fun onEditMove(x: Float, y: Float, dx: Float, dy: Float): Boolean {
        if (Input.isLeftDown) {
            // only run once per frame
            if (currTime == Time.gameTimeN) return true
            currTime = Time.gameTimeN
            // raycast, then apply brush
            val ui = RenderView.currentInstance!!
            val query = RayQuery(ui.cameraPosition, ui.mouseDirection, Double.POSITIVE_INFINITY)
            if (Raycast.raycastClosestHit(terrain.entity!!, query)) {
                val transform = Matrix4x3f()
                transform.translate(Vector3f(query.result.positionWS))
                if (brushMode.rotate) {
                    transform.rotate(Vector3f(query.result.geometryNormalWS).normalToQuaternionY(Quaternionf()))
                }
                transform.scale(0.3f * query.result.distance.toFloat())
                terrain.terrain.applyBrush(transform, brushMode.createBrush())
            } else LOGGER.warn("Missed scene")
            return true
        }
        return false
    }
}

// todo show ring of influence
// todo prevent losing focus by clicking in edit mode

fun main() {
    val scene = Entity("Scene")
    val terrain = TriTerrainComponent()
    val editor = TerrainEditModeV2(terrain)

    val noise = PerlinNoise(1234L, 5, 0.5f, -7f, 7f, Vector4f(1f / 20f))
    val height = F2F { x, y -> noise.getSmooth(x, y) }
    val tileSize = Vector2f(10f, 10f)
    val tileResolution = Vector2i(20, 20)
    val i0 = -10
    val i1 = +10
    for (ty in i0 until i1) {
        for (tx in i0 until i1) {
            val x0 = tileSize.x * tx
            val y0 = tileSize.y * ty
            terrain.terrain.initTile(
                AABBf(x0, 0f, y0, x0 + tileSize.x, 0f, y0 + tileSize.y),
                tileResolution, height
            )
        }
    }
    scene.add(terrain)
    scene.add(editor)
    testSceneWithUI("TerrainV2", scene)
}