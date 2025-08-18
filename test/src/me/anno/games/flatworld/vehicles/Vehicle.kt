package me.anno.games.flatworld.vehicles

import me.anno.Time
import me.anno.ecs.Component
import me.anno.ecs.systems.OnUpdate
import me.anno.engine.debug.DebugLine
import me.anno.engine.debug.DebugShapes.showDebugArrow
import me.anno.engine.serialization.NotSerializedProperty
import me.anno.games.flatworld.FlatWorld
import me.anno.games.flatworld.streets.StreetSegment
import me.anno.maths.Maths.dtTo01
import me.anno.maths.Maths.mix
import org.joml.Vector3d
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.min

class Vehicle(
    var currentT: Double,
    val route: List<StreetSegment>
) : Component(), OnUpdate {

    constructor() : this(0.0, emptyList())

    // todo two possible models:
    //  - player created/bought (Workers and Resources: Soviet Union)
    //  - automatically spawned (Cities Skylines, Sim City)

    // todo always transport stuff
    //  - have a deterministic, pre-computable route
    //  - replan, if streets go missing, or sth is taking much longer than expected

    var currentSegmentIndex = 0
    var speed = 0.0
    var maxSpeed = 10.0

    var length = 4.0
    val lengthPlusExtra get() = length * 1.25

    // var maxSpeed = 30.0
    // var acceleration = 3.0
    val prevPosition = Vector3d()
    var angle = 0.0

    var vehicleOffsetY = 0.5
    lateinit var world: FlatWorld

    // todo set this, when we spawn a vehicle
    @NotSerializedProperty
    var previousVehicle: Vehicle? = null

    val currentSegment get() = route.getOrNull(currentSegmentIndex)

    override fun onUpdate() {
        // move vehicle
        val dt = Time.deltaTime
        val curr = route[currentSegmentIndex]
        val previousVehicle = previousVehicle
        val prevT = if (previousVehicle != null) {
            if (previousVehicle.currentSegment == curr) {
                previousVehicle.currentT
            } else previousVehicle.currentT + 1.0
        } else 2.0

        if (previousVehicle?.currentSegment != null) {
            // show link to previous vehicle with arrow
            val dy = Vector3d(0.0, 1.0, 0.0)
            showDebugArrow(
                DebugLine(
                    prevPosition + dy, previousVehicle.prevPosition + dy,
                    -1, 0f
                )
            )
        }

        // todo regulate speed better:
        //  - slow down on intersections (if needed)
        // - accelerate when possible
        val trafficSpeed = if (previousVehicle != null) {
            max(speed, previousVehicle.speed)
        } else speed
        speed = mix(speed, maxSpeed, dtTo01(1.5 * dt)) // try to accelerate
        // 1.0s, or distance between cars
        // todo choose make this logic drive the front of the car, the rest is towed, and then
        //  use the length of the followed car for distance-keeping
        // todo test it with trucks/trains ^^
        val minDistanceBetweenVehicles = max(lengthPlusExtra, trafficSpeed)
        val ds = 3.0 * speed * dt / curr.length // extra 3x will be corrected later down
        val safetyT = prevT - minDistanceBetweenVehicles / curr.length
        val previousT = currentT
        currentT = min(min(currentT + ds, max(safetyT, currentT)), 1.0)
        // update position and rotation
        val transform = transform
        if (transform != null) {
            // add orthogonal lane offset
            val position = curr.interpolate(currentT)
            position.y += vehicleOffsetY
            val actualSpeed = position.distance(prevPosition) / dt
            if (actualSpeed > 1e-5) {

                if (actualSpeed > speed) {
                    // if distance is longer than expected (intersection), take longer
                    val allowedRelativeSpeed = speed / actualSpeed
                    prevPosition.mix(position, allowedRelativeSpeed, position)
                    currentT = mix(previousT, currentT, allowedRelativeSpeed)
                } else speed = actualSpeed

                val dx = position.x - prevPosition.x
                val dz = position.z - prevPosition.z
                prevPosition.set(position)
                angle = atan2(dx, dz)
                transform.localPosition = position
                transform.localRotation = transform.localRotation.rotationY(angle.toFloat())
                invalidateBounds()
            } else speed = 0.0
        }
        if (currentT >= 1.0) {
            val next = route.getOrNull(++currentSegmentIndex)
            if (next != null) {
                if (world.canInsertVehicle(this, next)) {
                    currentT = 0.0
                    world.removeVehicle(this, curr)
                    world.insertVehicle(this, next, 0.0)
                } else {
                    // waiting for gap
                    currentT = 1.0
                    currentSegmentIndex = route.lastIndex
                }
            } else {
                world.removeVehicle(this, route.last())
                entity?.destroy()
            }
        }
    }
}