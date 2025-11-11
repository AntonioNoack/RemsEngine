package com.bulletphysics.softbody.data

import org.joml.Vector3f

class SoftBodyTetra(
    val gradients: Array<Vector3f>, // 4x
    val material: SoftBodyMaterial,
    val nodeIndices: IntArray, // 4x
    val restVolume: Float,
    /**
     * (4*kVST)/(im0+im1+im2+im3)
     * */
    val c1: Float,
    /**
     * c1 / sum(|g0..3|^2)
     * */
    val c2: Float
)