package me.anno.games.roadcraft

import me.anno.Time
import me.anno.ecs.Component
import me.anno.ecs.EntityQuery.getComponent
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshAttributes.color0
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.MeshIterators.forEachPointIndex
import me.anno.ecs.interfaces.CustomEditMode
import me.anno.engine.raycast.RaycastMesh
import me.anno.engine.ui.render.RenderView
import me.anno.input.Input
import me.anno.input.Key
import me.anno.maths.Maths.dtTo10
import me.anno.maths.Maths.sq
import me.anno.utils.Color.mixARGB

// todo for each vertex remember how high the terrain below is,
//  and how much sand/stone/gravel is on top
//  and whether tarmac is on top of that

// todo smoothen sand over time, if possible

class TerrainPainting : Component(), CustomEditMode {

    enum class PaintMode {

        DRIVE_IN_SAND_OR_DIRT, // driving over it, makes a groove
        SMOOTH_SAND_HARSHLY, // bulldozing it

        ADD_SAND,
        ADD_TARMAC, // adds a visual layer of tarmac; only adds height if no tarmac is present
        SMOOTH_TARMAC, // aka roll tarmac; flattens sand with angle > 15°

        SMOOTH_ROCK, // debug

    }

    var mode = PaintMode.ADD_SAND

    override fun onEditClick(button: Key, long: Boolean): Boolean {
        return button == Key.BUTTON_LEFT
    }

    override fun onEditMove(x: Float, y: Float, dx: Float, dy: Float): Boolean {
        if (!Input.isLeftDown) return false
        val mesh = getComponent(MeshComponent::class)?.getMeshOrNull() as? Mesh ?: return false
        val rv = RenderView.currentInstance ?: return false
        val ray = rv.rayQuery()
        if (RaycastMesh.raycastGlobalMesh(ray, null, mesh)) {
            val hit = ray.result.positionWS
            // paint here / expand terrain
            val positions = mesh.positions!!
            val colors = mesh.color0!!
            val radiusSq = sq(ray.result.distance.toFloat() + 1f)
            val dt = Time.deltaTime.toFloat()
            val drawnColor = 0
            // todo implement these operations as compute shaders
            mesh.forEachPointIndex(false) { i ->
                val distanceSq = hit.distanceXZSquared(
                    positions[i * 3].toDouble(),
                    positions[i * 3 + 2].toDouble()
                ).toFloat()
                val force = 3f * dt * (1f / (1f + 2e3f * distanceSq / radiusSq) - 0.1f)
                if (force > 0.0f) {
                    // lerp the color
                    colors[i] = mixARGB(drawnColor, colors[i], dtTo10(force))
                    positions[i * 3 + 1] += force
                }
                false
            }
            mesh.calculateNormals(true)
            mesh.invalidateGeometry()
        }
        return true
    }
}
