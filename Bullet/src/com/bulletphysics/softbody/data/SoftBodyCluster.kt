package com.bulletphysics.softbody.data

import com.bulletphysics.linearmath.Transform
import org.joml.Matrix3f
import org.joml.Vector3f

class SoftBodyCluster {
    val framexform = Transform()
    val locii = Matrix3f()
    val invwi = Matrix3f()
    val com = Vector3f()
    val vimpulses = Array(2) { Vector3f() }
    val dimpulses = Array(2) { Vector3f() }
    val lv = Vector3f() // linear velocity?
    val av = Vector3f() // angular velocity?

    lateinit var frameRefs: FloatArray // ???
    lateinit var nodeIndices: IntArray
    lateinit var masses: FloatArray

    var idmass = 0f
    var imass = 0f
    var nvImpulses = 0
    var ndImpulses = 0
    var ndamping = 0f // ??
    var ldamping = 0f // linear
    var adamping = 0f // angular
    var matching = 0f // ??
    var maxSelfCollisionImpulse = 0f
    var selfCollisionImpulseFactor = 0f
    var containsAnchor = false
    var collide = false
    var clusterIndex = 0
}