package com.bulletphysics.softbody.data

import org.joml.Vector3f

class SoftBodyFace(
    val normal: Vector3f,
    val material: SoftBodyMaterial,
    val nodeIndex0: Int,
    val nodeIndex1: Int,
    val nodeIndex2: Int,
    val restArea: Float
)