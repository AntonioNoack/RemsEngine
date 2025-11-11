package com.bulletphysics.softbody.data

class SoftBodyLink(
    val material: SoftBodyMaterial,
    val nodeIndex0: Int,
    val nodeIndex1: Int,
    val restLength: Double,
    val bendingLink: Int // bbending
)