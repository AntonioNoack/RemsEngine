package me.sp4cerat.fqms

import org.joml.Vector3d

class Vertex {

    val position = Vector3d()
    val normalMatrix = SymmetricMatrix()

    var firstRefIndex = 0
    var numTriangles = 0
    var border = false
}