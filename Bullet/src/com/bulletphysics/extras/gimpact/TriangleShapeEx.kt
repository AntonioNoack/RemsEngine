package com.bulletphysics.extras.gimpact

import com.bulletphysics.collision.shapes.TriangleShape
import com.bulletphysics.linearmath.Transform
import cz.advel.stack.Stack
import org.joml.Vector3d

/**
 * @author jezek2
 */
class TriangleShapeEx : TriangleShape {

    constructor() : super()
    constructor(p0: Vector3d, p1: Vector3d, p2: Vector3d) : super(p0, p1, p2)

    override fun getBounds(t: Transform, aabbMin: Vector3d, aabbMax: Vector3d) {
        val tv0 = t.transformPosition(vertices[0], Stack.newVec())
        val tv1 = t.transformPosition(vertices[1], Stack.newVec())
        val tv2 = t.transformPosition(vertices[2], Stack.newVec())

        val triangleBox = AABB()
        triangleBox.calcFromTriangleMargin(tv0, tv1, tv2, margin)

        aabbMin.set(triangleBox.min)
        aabbMax.set(triangleBox.max)

        Stack.subVec(3)
    }
}
