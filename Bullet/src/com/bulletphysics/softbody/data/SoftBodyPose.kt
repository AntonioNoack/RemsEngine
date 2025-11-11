package com.bulletphysics.softbody.data

import org.joml.Matrix3f
import org.joml.Vector3f

class SoftBodyPose(
    val rotation: Matrix3f,
    val scale: Matrix3f,
    /** base scaling */
    val aqq: Matrix3f,
    val com: Vector3f,

    val positions: FloatArray, // vec3f
    val weights: FloatArray,

    val hasValidVolume: Boolean, // bvolume
    val hasValidFrame: Boolean, // bframe
    val restVolume: Float,
)