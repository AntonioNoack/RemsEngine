package me.sp4cerat.fqms

import org.joml.Vector3d

class Triangle {

    val vertexIds = IntArray(3)
    val errors = DoubleArray(4)

    var deleted = false
    var dirty = false

    var attrFlags = 0
    var materialId = 0

    val normal = Vector3d()
    val uvs = Array(3) { Vector3d() }
}