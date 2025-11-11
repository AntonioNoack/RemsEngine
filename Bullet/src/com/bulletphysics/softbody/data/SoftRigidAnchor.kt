package com.bulletphysics.softbody.data

import com.bulletphysics.dynamics.RigidBody
import org.joml.Matrix3f
import org.joml.Vector3d
import org.joml.Vector3f

class SoftRigidAnchor(
    val impulseMatrix: Matrix3f, // c0
    val relativeAnchor: Vector3f, // c1
    val localFrame: Vector3d, // anchor position in local space
    val rigidBody: RigidBody,
    val nodeIndex: Int,
    /**
     * ima * dt
     * */
    val c2: Float
)