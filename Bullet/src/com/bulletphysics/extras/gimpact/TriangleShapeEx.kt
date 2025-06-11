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

    override fun getAabb(t: Transform, aabbMin: Vector3d, aabbMax: Vector3d) {
        val tv0 = Stack.newVec(vertices[0])
        t.transform(tv0)
        val tv1 = Stack.newVec(vertices[1])
        t.transform(tv1)
        val tv2 = Stack.newVec(vertices[2])
        t.transform(tv2)

        val triangleBox = AABB()
        triangleBox.init(tv0, tv1, tv2, margin)

        aabbMin.set(triangleBox.min)
        aabbMax.set(triangleBox.max)

        Stack.subVec(3)
    }
}
