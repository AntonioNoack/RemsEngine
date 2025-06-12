package com.bulletphysics.collision.narrowphase

import com.bulletphysics.collision.shapes.ConvexShape
import com.bulletphysics.collision.shapes.TriangleCallback
import com.bulletphysics.collision.shapes.TriangleShape
import com.bulletphysics.linearmath.Transform
import cz.advel.stack.Stack
import org.joml.Vector3d

/**
 * @author jezek2
 */
abstract class TriangleConvexCastCallback(
    var convexShape: ConvexShape,
    convexShapeFrom: Transform,
    convexShapeTo: Transform,
    triangleToWorld: Transform,
    triangleCollisionMargin: Double
) : TriangleCallback {

    val convexShapeFrom: Transform = Transform()
    val convexShapeTo: Transform = Transform()
    val triangleToWorld: Transform = Transform()
    var hitFraction: Double
    var triangleCollisionMargin: Double

    init {
        this.convexShapeFrom.set(convexShapeFrom)
        this.convexShapeTo.set(convexShapeTo)
        this.triangleToWorld.set(triangleToWorld)
        this.hitFraction = 1.0
        this.triangleCollisionMargin = triangleCollisionMargin
    }

    override fun processTriangle(triangle: Array<Vector3d>, partId: Int, triangleIndex: Int) {
        val triangleShape = TriangleShape(triangle[0], triangle[1], triangle[2])
        triangleShape.margin = triangleCollisionMargin

        val simplexSolver = Stack.newVSS()

        // TODO: implement ContinuousConvexCollision
        val convexCaster = SubSimplexConvexCast(convexShape, triangleShape, simplexSolver)

        val castResult = Stack.newCastResult()
        castResult.fraction = 1.0
        if (convexCaster.calcTimeOfImpact(
                convexShapeFrom, convexShapeTo,
                triangleToWorld, triangleToWorld, castResult
            )
        ) {
            // add hit
            if (castResult.normal.lengthSquared() > 0.0001f) {
                if (castResult.fraction < hitFraction) {
                    /* btContinuousConvexCast's normal is already in world space */
                    /*
					// rotate normal into worldspace
					convexShapeFrom.basis.transform(castResult.normal);
					*/

                    castResult.normal.normalize()

                    reportHit(
                        castResult.normal,
                        castResult.hitPoint,
                        castResult.fraction,
                        partId,
                        triangleIndex
                    )
                }
            }
        }

        Stack.subVSS(1)
        Stack.subCastResult(1)
    }

    abstract fun reportHit(
        hitNormalLocal: Vector3d,
        hitPointLocal: Vector3d,
        hitFraction: Double,
        partId: Int,
        triangleIndex: Int
    ): Double
}
