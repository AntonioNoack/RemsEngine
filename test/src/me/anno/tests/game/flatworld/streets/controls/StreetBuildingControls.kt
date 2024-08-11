package me.anno.tests.game.flatworld.streets.controls

import me.anno.ecs.Transform
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.engine.raycast.RayQuery
import me.anno.engine.raycast.Raycast
import me.anno.engine.ui.control.DraggingControls
import me.anno.engine.ui.render.RenderView
import me.anno.gpu.pipeline.Pipeline
import me.anno.input.Key
import me.anno.maths.Maths.sq
import me.anno.tests.game.flatworld.FlatWorld
import me.anno.tests.game.flatworld.streets.StreetSegment
import me.anno.tests.game.flatworld.streets.StreetMeshBuilder
import me.anno.tests.gmaps.pos
import org.joml.Vector3d
import kotlin.math.max

class StreetBuildingControls(val world: FlatWorld, rv: RenderView) : DraggingControls(rv) {

    // todo dragging is also intuitive -> define street by dragging
    //  - collect points
    //  - find middle
    //  - calculate anchors
    //  - place street, if dragging was long enough

    // todo handle double click as additional normal click

    enum class StreetPlacingStage {
        EXPLORING,
        DRAGGING_2,
        DRAGGING_3
    }

    var stage = StreetPlacingStage.EXPLORING

    var anchor0: Vector3d? = null
    var anchor1: Vector3d? = null
    var anchor2: Vector3d? = null

    val previewMesh = Mesh()

    override fun onUpdate() {
        super.onUpdate()
        when (stage) {
            StreetPlacingStage.EXPLORING -> {}
            StreetPlacingStage.DRAGGING_2 -> {
                anchor1 = getAnchorAt(false)
                if (anchor1 != null) {
                    buildMesh()
                }
            }
            StreetPlacingStage.DRAGGING_3 -> {
                anchor2 = getAnchorAt(true)
                buildMesh()
            }
        }
    }

    fun getCurrentSegment(): StreetSegment {
        return if (anchor2 != null) {
            StreetSegment(anchor0!!, anchor1, anchor2!!)
        } else {
            StreetSegment(anchor0!!, null, anchor1!!)
        }
    }

    fun buildMesh() {
        StreetMeshBuilder.buildMesh(getCurrentSegment(), previewMesh)
    }


    val comp = MeshComponent()
    val transform = Transform()

    override fun fill(pipeline: Pipeline) {
        super.fill(pipeline)
        if (anchor0 != null && anchor1 != null) {
            pipeline.addMesh(previewMesh, comp, transform)
        }
    }

    fun getAnchorAt(snap: Boolean): Vector3d? {
        val query = RayQuery(renderView.cameraPosition, renderView.mouseDirection, 1e6)
        return if (Raycast.raycastClosestHit(world.terrain, query)) {
            // todo if anchor already exists within 10px radius, use that
            val position = query.result.positionWS
            val hitDistance = query.result.distance
            val tolerance10px = hitDistance * 10.0 / max(renderView.width, renderView.height)
            val anchor0 = anchor0
            // todo snap to tangent, if present
            if (!snap && anchor0 != null && position.distance(anchor0) < tolerance10px) { // middle
                position.set(anchor0)
            }
            if (snap) {
                val bestPosition = world.streetSegments
                    .flatMap { listOf(it.a, it.c) }
                    .minByOrNull { it.distanceSquared(position) }
                if (bestPosition != null && bestPosition.distanceSquared(position) < sq(tolerance10px)) {
                    position.set(bestPosition)
                }
            }
            position
        } else null
    }

    override fun onMouseClicked(x: Float, y: Float, button: Key, long: Boolean) {
        when (button) {
            Key.BUTTON_LEFT -> nextStep()
            Key.BUTTON_RIGHT -> undoStep()
            else -> {}
        }
    }

    override fun onDoubleClick(x: Float, y: Float, button: Key) {
        onMouseClicked(x, y, button, false)
    }

    override fun onEscapeKey(x: Float, y: Float) {
        undoStep()
    }

    fun nextStep() {
        when (stage) {
            StreetPlacingStage.EXPLORING -> {
                anchor0 = getAnchorAt(true)
                if (anchor0 != null) stage = StreetPlacingStage.DRAGGING_2
            }
            StreetPlacingStage.DRAGGING_2 -> {
                if (anchor1 != null) stage = StreetPlacingStage.DRAGGING_3
            }
            StreetPlacingStage.DRAGGING_3 -> {

                // officially add street to world
                val segment = getCurrentSegment()
                world.addStreetSegment(segment)
                world.validateIntersections()

                // or shall we continue in straight stage? probably better :)
                // todo keep tangent stable...
                //  -> snapping to tangent line
                anchor0 = anchor2 ?: anchor1
                anchor1 = null
                anchor2 = null
                stage = StreetPlacingStage.DRAGGING_2
            }
        }
    }

    fun undoStep() {
        when (stage) {
            StreetPlacingStage.EXPLORING -> {}
            StreetPlacingStage.DRAGGING_2 -> {
                anchor1 = null
                anchor2 = null
                stage = StreetPlacingStage.EXPLORING
            }
            StreetPlacingStage.DRAGGING_3 -> {
                anchor2 = null
                stage = StreetPlacingStage.DRAGGING_2
            }
        }
    }
}

