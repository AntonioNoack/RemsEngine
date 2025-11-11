package com.bulletphysics.softbody.data

import org.joml.Vector3f

class SoftBodyJoint {
    lateinit var bodyA: Any
    lateinit var bodyB: Any

    val refs = Array(2) { Vector3f() }
    var cfm = 0f
    var erp = 0f
    var split = 0f
    var delete = false
    val relPosition = Array(2) { Vector3f() } // linear
    var bodyAType = 0
    var bodyBType = 0
    var jointType = 0

}