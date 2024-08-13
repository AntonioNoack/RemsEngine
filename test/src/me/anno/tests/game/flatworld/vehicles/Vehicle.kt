package me.anno.tests.game.flatworld.vehicles

import me.anno.Time
import me.anno.ecs.Component
import me.anno.ecs.systems.OnUpdate
import me.anno.engine.Events.addEvent
import me.anno.tests.game.flatworld.streets.ReversibleSegment
import org.joml.Vector3d
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

class Vehicle(
    var currentT: Double,
    val route: List<ReversibleSegment>
) : Component(), OnUpdate {

    constructor() : this(0.0, emptyList())

    // todo two possible models:
    //  - player created/bought
    //  - automatically spawned

    // todo always transport stuff
    //  - have a deterministic, pre-computable route
    //  - replan, if streets go missing, or sth is taking much longer than expected

    var currentSegment = 0
    var speed = 10.0

    // var maxSpeed = 30.0
    // var acceleration = 3.0
    val prevPosition = Vector3d()
    var angle = 0.0

    override fun onUpdate() {
        // move vehicle
        val curr = route[currentSegment]
        val ds = speed * Time.deltaTime / curr.segment.length
        currentT += ds
        // update position and rotation
        val transform = transform
        if (transform != null) {
            // todo handle intersections smoothly
            // add orthogonal lane offset
            val offsetFromCenter = 1.3
            val position = curr.interpolate(currentT)
            val dx = position.x - prevPosition.x
            val dz = position.z - prevPosition.z
            prevPosition.set(position)
            position.add(-cos(angle) * offsetFromCenter, 0.0, sin(angle) * offsetFromCenter)
            angle = atan2(dx, dz)
            transform.localPosition = position
            transform.localRotation = transform.localRotation
                .identity()
                .rotateY(angle)
            transform.teleportUpdate()
            entity?.invalidateOwnAABB()
            entity?.parentEntity?.invalidateOwnAABB() // todo why is that needed???
        }
        if (currentT >= 1.0) {
            val next = route.getOrNull(++currentSegment)
            if (next != null) {
                currentT = (currentT - 1.0) * curr.length / next.length
            } else {
                // todo can we schedule this in a nicer way?
                addEvent {
                    // prevent concurrent modification exceptions,
                    // because we're removing sth from a list what is to be updated
                    entity?.destroy()
                }
            }
        }
    }
}