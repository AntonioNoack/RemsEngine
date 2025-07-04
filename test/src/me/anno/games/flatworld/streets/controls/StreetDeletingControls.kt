package me.anno.games.flatworld.streets.controls

import me.anno.ecs.Transform
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.material.Material
import me.anno.engine.raycast.Raycast
import me.anno.engine.ui.control.DraggingControls
import me.anno.engine.ui.render.RenderView
import me.anno.games.flatworld.FlatWorld
import me.anno.games.flatworld.streets.StreetMeshBuilder
import me.anno.games.flatworld.streets.StreetSegment
import me.anno.gpu.pipeline.Pipeline
import me.anno.input.Input
import me.anno.ui.UIColors
import org.joml.Vector3d
import kotlin.math.max

class StreetDeletingControls(val world: FlatWorld, rv: RenderView) : DraggingControls(rv) {

    companion object {
        val deleteMaterial = Material.diffuse(UIColors.fireBrick)
        val deleteMaterialList = listOf(deleteMaterial.ref)
    }

    data class ToBeDeleted(val segment: StreetSegment, val t0: Double, val t1: Double, val helperMesh: Mesh)

    val meshCache = ArrayList<Mesh>()

    override fun onUpdate() {
        super.onUpdate()
        // only delete a subsection of these segments:
        //  - cut along radius around mouse, maybe 50px
        //  - if farther than that, check if it is long enough to stay (2m?)
        //  - if not, delete it, too;
        meshCache.addAll(selected.map { it.helperMesh })
        selected.clear()
        // find selected segments:
        //  all segments, where
        //   distance to line < street thickness / 2 + 10px
        val pos = renderView.mousePosition
        val dir = renderView.mouseDirection
        // find hit distance
        val query = renderView.rayQuery()
        val hitDistance = if (Raycast.raycast(world.terrain, query)) query.result.distance else 1.0
        val tolerance10px = hitDistance * 10.0 / max(renderView.width, renderView.height)
        for (segment in world.streetSegments) {
            val streetThickness = 4.8 // todo depends on street profile/type
            val segmentLength = segment.length
            val hit = segment.distanceToRay(pos, Vector3d(dir))
            if (hit.distance < max(streetThickness * 0.5, tolerance10px)) {
                val margin = max(5.0 * tolerance10px, 1.0) / segmentLength
                val marginMin = 1.25 * margin + 2.0 / segmentLength
                val t0 = if (hit.t > marginMin) hit.t - margin else 0.0
                val t1 = if (1.0 - hit.t > marginMin) hit.t + margin else 1.0
                val helper = segment.splitSegment(t0, t1)
                val mesh = meshCache.removeLastOrNull() ?: Mesh()
                mesh.materials = deleteMaterialList
                StreetMeshBuilder.buildMesh(helper, mesh)
                selected.add(ToBeDeleted(segment, t0, t1, mesh))
            }
        }
    }

    val comp = MeshComponent()
    val transform = Transform()

    init {
        // make it better visible by raising it
        transform.setLocalPosition(0.0, 0.1, 0.0)
        transform.teleportUpdate()
    }

    override fun fill(pipeline: Pipeline) {
        super.fill(pipeline)
        for (select in selected) {
            pipeline.addMesh(select.helperMesh, comp, transform)
        }
    }

    // todo drag to delete a t-range
    // todo different move to delete whole street segments only

    // todo highlight end-caps/crossings, if they're selected
    val selected = ArrayList<ToBeDeleted>()

    override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float) {
        if (Input.isLeftDown) deleteSelected()
        else super.onMouseMoved(x, y, dx, dy)
    }

    fun deleteSelected() {
        for (select in selected) {
            world.removeStreetSegment(select.segment)
        }
        for (select in selected) {
            // add back in remaining street segments
            val segment = select.segment
            if (select.t0 > 0.0) {
                world.addStreetSegment(segment.splitSegment(0.0, select.t0))
            }
            if (select.t1 < 1.0) {
                world.addStreetSegment(segment.splitSegment(select.t1, 1.0))
            }
        }
        selected.clear()
        world.validateMeshes()
    }
}