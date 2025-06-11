package com.bulletphysics.dynamics.vehicle

import org.joml.Vector3d

/**
 * interface for rays between vehicle simulation and raycasting.
 *
 * @author jezek2
 */
abstract class VehicleRaycaster {
    abstract fun castRay(from: Vector3d, to: Vector3d, result: VehicleRaycasterResult): Any?
}
