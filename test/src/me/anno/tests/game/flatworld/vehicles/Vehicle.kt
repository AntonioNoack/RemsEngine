package me.anno.tests.game.flatworld.vehicles

import me.anno.Time
import me.anno.ecs.Component
import me.anno.ecs.systems.OnUpdate
import me.anno.engine.serialization.NotSerializedProperty
import me.anno.tests.game.flatworld.streets.ReversibleSegment
import org.joml.Vector3d
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

class Vehicle(
    var currentT: Double,
    val route: List<ReversibleSegment>
) : Component(), OnUpdate {

    constructor() : this(0.0, emptyList())

    // todo two possible models:
    //  - player created/bought (Workers and Resources: Soviet Union)
    //  - automatically spawned (Cities Skylines, Sim City)

    // todo always transport stuff
    //  - have a deterministic, pre-computable route
    //  - replan, if streets go missing, or sth is taking much longer than expected

    var currentSegmentIndex = 0
    var speed = 10.0

    // var maxSpeed = 30.0
    // var acceleration = 3.0
    val prevPosition = Vector3d()
    var angle = 0.0

    var vehicleOffsetY = 0.5

    @NotSerializedProperty
    var previousVehicle: Vehicle? = null

    val currentSegment get() = route[currentSegmentIndex]

    override fun onUpdate() {
        // move vehicle
        val curr = route[currentSegmentIndex]
        val previousVehicle = previousVehicle
        val prevT = if (previousVehicle != null) {
            if (previousVehicle.currentSegment == curr) {
                previousVehicle.currentT
            } else previousVehicle.currentT + 1.0
        } else 2.0
        // todo regulate speed better:
        //  - accelerate when possible
        //  - slow down on intersections (if needed)
        //  -
        val trafficSpeed = if (previousVehicle != null) {
            max(speed, previousVehicle.speed)
        } else speed
        val minDistanceBetweenVehicles = 1.0 * trafficSpeed // 1.0s
        val ds = speed * Time.deltaTime / curr.segment.length
        val safetyT = prevT - minDistanceBetweenVehicles / curr.segment.length
        currentT = min(currentT + ds, safetyT)
        // update position and rotation
        val transform = transform
        if (transform != null) {
            // todo handle intersections smoothly
            // add orthogonal lane offset
            val offsetFromCenter = 1.3
            val position = curr.interpolate(currentT)
            position.y += vehicleOffsetY
            val dx = position.x - prevPosition.x
            val dz = position.z - prevPosition.z
            prevPosition.set(position)
            position.add(-cos(angle) * offsetFromCenter, 0.0, sin(angle) * offsetFromCenter)
            angle = atan2(dx, dz)
            transform.localPosition = position
            transform.localRotation = transform.localRotation
                .identity().rotateY(angle)
            transform.smoothUpdate()
            invalidateAABB()
        }
        if (currentT >= 1.0) {
            val next = route.getOrNull(++currentSegmentIndex)
            if (next != null) {
                currentT = (currentT - 1.0) * curr.length / next.length
            } else {
                entity?.destroy()
            }
        }
    }
}