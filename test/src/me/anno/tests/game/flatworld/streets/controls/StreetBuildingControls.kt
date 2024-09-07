package me.anno.tests.game.flatworld.streets.controls

import me.anno.ecs.Transform
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.engine.debug.DebugPoint
import me.anno.engine.debug.DebugShapes
import me.anno.engine.raycast.RayQuery
import me.anno.engine.raycast.Raycast
import me.anno.engine.ui.control.DraggingControls
import me.anno.engine.ui.render.RenderView
import me.anno.gpu.pipeline.Pipeline
import me.anno.input.Key
import me.anno.maths.Maths.min
import me.anno.maths.Maths.sq
import me.anno.tests.game.flatworld.FlatWorld
import me.anno.tests.game.flatworld.streets.StreetMeshBuilder
import me.anno.tests.game.flatworld.streets.StreetSegment
import me.anno.ui.UIColors
import org.joml.Vector2d
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
            StreetPlacingStage.EXPLORING -> {
                anchor0 = getAnchorAt(0)
                if (anchor0 != null) {
                    buildMesh()
                }
            }
            StreetPlacingStage.DRAGGING_2 -> {
                anchor1 = getAnchorAt(1)
                if (anchor1 != null) {
                    buildMesh()
                }
            }
            StreetPlacingStage.DRAGGING_3 -> {
                anchor2 = getAnchorAt(2)
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
        if (anchor1 == null) {
            // todo add a circular piece...
            DebugShapes.debugPoints.add(
                DebugPoint(
                    Vector3d(anchor0!!).add(0.0, 1.0, 0.0),
                    UIColors.greenYellow, 0f
                )
            )
            return
        } else {
            StreetMeshBuilder.buildMesh(getCurrentSegment(), previewMesh)
        }
    }


    val comp = MeshComponent()
    val transform = Transform()

    override fun fill(pipeline: Pipeline) {
        super.fill(pipeline)
        if (anchor0 != null && anchor1 != null) {
            pipeline.addMesh(previewMesh, comp, transform)
        }
    }

    fun getAnchorAt(i: Int): Vector3d? {
        val query = RayQuery(renderView.cameraPosition, renderView.mouseDirection, 1e6)
        return if (Raycast.raycastClosestHit(world.terrain, query)) {
            val position0 = query.result.positionWS
            val position = Vector3d(position0)
            val hitDistance = query.result.distance
            val streetThickness = 4.8
            val tolerance10px = hitDistance * 10.0 / max(renderView.width, renderView.height)
            val tolerance = max(tolerance10px, streetThickness)
            // todo snap to tangent, if present
            // todo snap to certain degrees / distances of the map? definitely must be configurable
            // snap to other streets, not just their anchor points
            val bestOnRoad = world.streetSegments
                .map { it to it.distanceToRay(renderView.cameraPosition, renderView.mouseDirection) }
                .minByOrNull { it.second }
            if (bestOnRoad != null && bestOnRoad.second.distance < tolerance) {
                position.set(bestOnRoad.first.interpolate(bestOnRoad.second.t))
            }
            // if anchor already exists within 10px radius, use that
            val bestOnAnchor = world.streetSegments
                .flatMap { listOf(it.a, it.c) }
                .minByOrNull { it.distanceSquared(position) }
            if (bestOnAnchor != null && bestOnAnchor.distanceSquared(position) < sq(tolerance)) {
                position.set(bestOnAnchor)
            }
            if (i > 0 && anchor0!!.distance(position) < 5.0) return null
            if (i > 1 && anchor1!!.distance(position) < 5.0) return null
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
                if (anchor0 != null) stage = StreetPlacingStage.DRAGGING_2
            }
            StreetPlacingStage.DRAGGING_2 -> {
                if (anchor1 != null) stage = StreetPlacingStage.DRAGGING_3
            }
            StreetPlacingStage.DRAGGING_3 -> {

                // todo if there is two relatively straight-connected segments,
                //  and we go through close to their intersection, move that intersection
                //  instead of cutting one of them

                // officially add street to world
                val segment = getCurrentSegment()
                // calculate all potential intersections
                // todo what if the same road is intersected twice???
                // todo prevent too small angles between crossing roads
                val intersections = ArrayList<Triple<Vector2d, Vector3d, StreetSegment>>()
                for (seg in world.streetSegments) {
                    val intersection = segment.intersects(seg, false) ?: continue
                    val tolerance = min(5.0 / seg.length, 0.5)
                    val point = if (intersection.y < 0.0 + tolerance) seg.a
                    else if (intersection.y > 1.0 - tolerance) seg.c
                    else {
                        segment.interpolate(intersection.x)
                            .add(seg.interpolate(intersection.y)).mul(0.5)
                    }
                    intersections.add(Triple(intersection, point, seg))
                }

                println("Intersections: $intersections")

                // split segments accordingly
                if (intersections.isEmpty()) {
                    world.addStreetSegment(segment)
                } else {

                    // todo if destination is anchor point, just use it instead of interpolating
                    for (intersection in intersections) {
                        val t = intersection.first.y
                        val tolerance = min(5.0 / intersection.third.length, 0.5)
                        if (t < 0.0 + tolerance || t > 1.0 - tolerance) continue
                        // split that segment
                        world.removeStreetSegment(intersection.third)
                        val seg = intersection.third
                        world.addStreetSegment(seg.splitSegment(0.0, t, seg.a, intersection.second))
                        world.addStreetSegment(seg.splitSegment(t, 1.0, intersection.second, seg.c))
                    }
                    intersections.sortBy { it.first.x }
                    val i0 = intersections.first()
                    val tolerance = min(5.0 / segment.length, 0.5)
                    if (i0.first.x > 0.0 + tolerance) {
                        world.addStreetSegment(segment.splitSegment(0.0, i0.first.x, segment.a, i0.second))
                    }
                    for (i in 1 until intersections.size) {
                        val t0 = intersections[i - 1]
                        val t1 = intersections[i]
                        if (t0.first.x + tolerance < t1.first.x) {
                            world.addStreetSegment(
                                segment.splitSegment(
                                    t0.first.x, t1.first.x,
                                    t0.second, t1.second
                                )
                            )
                        }
                    }
                    val il = intersections.last()
                    if (il.first.x < 1.0 - tolerance) {
                        world.addStreetSegment(
                            segment.splitSegment(
                                il.first.x, 1.0,
                                il.second, segment.c
                            )
                        )
                    }
                }
                world.validateMeshes()

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

