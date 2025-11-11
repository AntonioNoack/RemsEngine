package com.bulletphysics.softbody.data

import org.joml.Vector3d

class SoftBodyNode(
    val material: SoftBodyMaterial,
    val position: Vector3d,
    val prevPosition: Vector3d,
    val velocity: Vector3d,
    val accuForce: Vector3d,
    val normal: Vector3d,
    val inverseMass: Double,
    val area: Double,
    var attach: Int
)