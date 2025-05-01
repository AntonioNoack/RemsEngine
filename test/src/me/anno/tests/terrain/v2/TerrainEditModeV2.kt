package me.anno.tests.terrain.v2

import me.anno.Time
import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.EntityQuery.getComponent
import me.anno.ecs.annotations.EditorField
import me.anno.ecs.components.mesh.terrain.v2.BrushMode
import me.anno.ecs.components.mesh.terrain.v2.TriTerrainChunk
import me.anno.ecs.components.mesh.terrain.v2.TriTerrainRenderer
import me.anno.ecs.interfaces.CustomEditMode
import me.anno.engine.raycast.RayQuery
import me.anno.engine.raycast.Raycast
import me.anno.engine.serialization.NotSerializedProperty
import me.anno.engine.ui.render.RenderView
import me.anno.input.Input
import me.anno.input.Key
import me.anno.utils.types.Vectors.normalToQuaternionY
import org.apache.logging.log4j.LogManager
import org.joml.Matrix4x3f
import org.joml.Quaternionf
import org.joml.Vector3d
import org.joml.Vector3f

class TerrainEditModeV2 : Component(), CustomEditMode {

    companion object {
        private val LOGGER = LogManager.getLogger(TerrainEditModeV2::class)
    }

    @EditorField
    @NotSerializedProperty
    var brushMode = BrushMode.FLATTEN

    var cursor: Entity? = null

    override fun onEditClick(button: Key, long: Boolean): Boolean {
        return true // ignore clicks
    }

    private fun hideCursor() {
        val transform = cursor?.transform ?: return
        transform.localPosition.set(0.0, 1e16, 0.0)
        transform.localScale = transform.localScale.set(1e-16)
        transform.teleportUpdate()
    }

    private fun updateCursor(query: RayQuery, renderView: RenderView) {
        val cursor = cursor?.transform
        if (cursor != null) {
            val normal = query.result.shadingNormalWS
            cursor.localPosition =
                query.result.positionWS + Vector3d(renderView.mouseDirection).mul(-0.1 * query.result.distance)
            cursor.localRotation = normal.normalToQuaternionY()
            cursor.localScale = cursor.localScale.set(0.1 * query.result.distance)
        }
    }

    private fun applyBrush(query: RayQuery, terrain: TriTerrainChunk) {
        val transform = Matrix4x3f()
        transform.translate(Vector3f(query.result.positionWS))
        if (brushMode.rotate) {
            transform.rotate(Vector3f(query.result.geometryNormalWS).normalToQuaternionY(Quaternionf()))
        }
        transform.scale(0.3f * query.result.distance.toFloat())
        terrain.applyBrush(transform, brushMode.createBrush())
    }

    private var currTime = 0L
    override fun onEditMove(x: Float, y: Float, dx: Float, dy: Float): Boolean {
        // only run once per frame, because applying the brush can be expensive,
        // and we can get many onEditMove()-calls per frame
        if (currTime == Time.gameTimeN) return true
        currTime = Time.gameTimeN
        val terrain = getComponent(TriTerrainRenderer::class)
        // raycast, then apply brush
        val renderView = RenderView.currentInstance!!
        val query = renderView.rayQuery()
        if (terrain != null && Raycast.raycast(terrain.entity!!, query)) {
            if (Input.isLeftDown) applyBrush(query, terrain.terrain)
            updateCursor(query, renderView)
            return Input.isLeftDown
        } else {
            LOGGER.warn("Missed scene")
            hideCursor()
            return false
        }
    }
}
