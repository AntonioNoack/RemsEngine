package me.anno.tests.terrain

import me.anno.Time
import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.EntityQuery.getComponent
import me.anno.ecs.annotations.EditorField
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.material.AutoTileableMaterial
import me.anno.ecs.components.mesh.terrain.v2.BrushMode
import me.anno.ecs.components.mesh.terrain.v2.TriTerrainComponent
import me.anno.ecs.interfaces.CustomEditMode
import me.anno.engine.OfficialExtensions
import me.anno.engine.raycast.RayQuery
import me.anno.engine.raycast.Raycast
import me.anno.engine.serialization.NotSerializedProperty
import me.anno.engine.ui.render.RenderView
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.input.Input
import me.anno.input.Key
import me.anno.maths.noise.PerlinNoise
import me.anno.utils.OS.pictures
import me.anno.utils.OS.res
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

// todo gpu-accelerated editing
// todo LODs
// todo (de)serialization

private val LOGGER = LogManager.getLogger("TerrainV2")

class TerrainEditModeV2 : Component(), CustomEditMode {

    @EditorField
    @NotSerializedProperty
    var brushMode = BrushMode.FLATTEN

    var cursor: Entity? = null

    fun hideCursor() {
        return
        val transform = cursor?.transform ?: return
        transform.localPosition.set(0.0, 1e16, 0.0)
        transform.localScale = transform.localScale.set(1e-16)
        transform.teleportUpdate()
    }

    override fun onEditClick(button: Key, long: Boolean): Boolean {
        return true // ignore clicks
    }

    private var currTime = 0L
    override fun onEditMove(x: Float, y: Float, dx: Float, dy: Float): Boolean {
        if (Input.isLeftDown) {
            // only run once per frame
            if (currTime == Time.gameTimeN) return true
            currTime = Time.gameTimeN
            val terrain = getComponent(TriTerrainComponent::class)
            // raycast, then apply brush
            val ui = RenderView.currentInstance!!
            val query = RayQuery(ui.cameraPosition, ui.mouseDirection, Double.POSITIVE_INFINITY)
            if (terrain != null && Raycast.raycastClosestHit(terrain.entity!!, query)) {
                val transform = Matrix4x3f()
                transform.translate(Vector3f(query.result.positionWS))
                if (brushMode.rotate) {
                    transform.rotate(Vector3f(query.result.geometryNormalWS).normalToQuaternionY(Quaternionf()))
                }
                transform.scale(0.3f * query.result.distance.toFloat())
                terrain.terrain.applyBrush(transform, brushMode.createBrush())
                val cursor = cursor?.transform
                if (cursor != null) {
                    cursor.localPosition = query.result.positionWS
                    cursor.localScale = cursor.localScale.set(0.01 * query.result.distance)
                    cursor.teleportUpdate()
                    cursor.entity!!.invalidateOwnAABB()
                }
            } else {
                LOGGER.warn("Missed scene")
                hideCursor()
            }
            return true
        } else hideCursor()
        return false
    }
}

// todo show ring of influence
// todo prevent losing focus by clicking in edit mode

fun main() {

    // todo setting additive destroys everything... why???

    val material = AutoTileableMaterial()
    material.diffuseMap = pictures.getChild("textures/grass.jpg")
    material.roughnessMinMax

    OfficialExtensions.initForTests()
    val scene = Entity("Scene")
    val terrain = TriTerrainComponent()
    terrain.material = material
    val editor = TerrainEditModeV2()
    editor.cursor = Entity("Cursor", scene)
        .add(MeshComponent(res.getChild("meshes/TeleportCircle.glb")))

    val noise = PerlinNoise(1234L, 5, 0.5f, -7f, 7f, Vector4f(1f / 20f))
    val height = F2F(noise::getSmooth)
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