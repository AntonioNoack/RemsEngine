package com.bulletphysics.collision.narrowphase

import org.joml.Vector3d
import org.joml.Vector3f

/**
 * ManifoldPoint collects and maintains persistent contactpoints. Used to improve
 * stability and performance of rigidbody dynamics response.
 *
 * @author jezek2
 */
class ManifoldPoint {
    @JvmField
    val localPointA = Vector3d()

    @JvmField
    val localPointB = Vector3d()

    @JvmField
    val positionWorldOnB = Vector3d()

    @JvmField
    val positionWorldOnA = Vector3d()

    @JvmField
    val normalWorldOnB = Vector3f()

    var distance = 0f

    @JvmField
    var combinedFriction = 0f

    @JvmField
    var combinedRestitution = 0f

    // BP mod, store contact triangles.
    var partId0: Int = 0
    var partId1: Int = 0
    var index0: Int = 0
    var index1: Int = 0

    @JvmField
    var userPersistentData: Any? = null

    @JvmField
    var appliedImpulse = 0f

    @JvmField
    var lateralFrictionInitialized: Boolean = false

    @JvmField
    var appliedImpulseLateral1 = 0f

    @JvmField
    var appliedImpulseLateral2 = 0f

    @JvmField
    var lifeTime: Int = 0 // lifetime of the contact point in frames

    @JvmField
    val lateralFrictionDir1 = Vector3f()

    @JvmField
    val lateralFrictionDir2 = Vector3f()

    constructor() {
        this.userPersistentData = null
        this.appliedImpulse = 0f
        this.lateralFrictionInitialized = false
        this.lifeTime = 0
    }

    fun init(pointA: Vector3d, pointB: Vector3d, normal: Vector3f, distance: Float) {
        this.localPointA.set(pointA)
        this.localPointB.set(pointB)
        this.normalWorldOnB.set(normal)
        this.distance = distance
        this.combinedFriction = 0f
        this.combinedRestitution = 0f
        this.userPersistentData = null
        this.appliedImpulse = 0f
        this.lateralFrictionInitialized = false
        this.appliedImpulseLateral1 = 0f
        this.appliedImpulseLateral2 = 0f
        this.lifeTime = 0
    }

    fun set(p: ManifoldPoint) {
        localPointA.set(p.localPointA)
        localPointB.set(p.localPointB)
        positionWorldOnA.set(p.positionWorldOnA)
        positionWorldOnB.set(p.positionWorldOnB)
        normalWorldOnB.set(p.normalWorldOnB)
        this.distance = p.distance
        combinedFriction = p.combinedFriction
        combinedRestitution = p.combinedRestitution
        partId0 = p.partId0
        partId1 = p.partId1
        index0 = p.index0
        index1 = p.index1
        userPersistentData = p.userPersistentData
        appliedImpulse = p.appliedImpulse
        lateralFrictionInitialized = p.lateralFrictionInitialized
        appliedImpulseLateral1 = p.appliedImpulseLateral1
        appliedImpulseLateral2 = p.appliedImpulseLateral2
        lifeTime = p.lifeTime
        lateralFrictionDir1.set(p.lateralFrictionDir1)
        lateralFrictionDir2.set(p.lateralFrictionDir2)
    }

    fun getPositionWorldOnA(out: Vector3d): Vector3d {
        out.set(positionWorldOnA)
        return out
    }

    fun getPositionWorldOnB(out: Vector3d): Vector3d {
        out.set(positionWorldOnB)
        return out
    }
}
