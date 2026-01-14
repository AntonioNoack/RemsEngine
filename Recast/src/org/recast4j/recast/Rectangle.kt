package org.recast4j.recast

data class Rectangle(
    var minX: Float,
    var maxX: Float,
    var minZ: Float,
    var maxZ: Float,
    val minY: Float,
)