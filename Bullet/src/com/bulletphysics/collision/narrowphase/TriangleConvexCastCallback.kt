package com.bulletphysics.collision.narrowphase

import com.bulletphysics.collision.shapes.ConvexShape
import com.bulletphysics.collision.shapes.TriangleCallback
import com.bulletphysics.collision.shapes.TriangleShape
import com.bulletphysics.linearmath.Transform
import cz.advel.stack.Stack
import org.joml.Vector3d
import org.joml.Vector3f

/**
 * @author jezek2
 */
abstract class TriangleConvexCastCallback(
    var convexShape: ConvexShape,
    convexShapeFrom: Transform,
    convexShapeTo: Transform,
    triangleToWorld: Transform,
    var triangleCollisionMargin: Float
) : TriangleCallback {

    val convexShapeFrom = Transform()
    val convexShapeTo = Transform()
    val triangleToWorld = Transform()
    var hitFraction = 1f

    init {
        this.convexShapeFrom.set(convexShapeFrom)
        this.convexShapeTo.set(convexShapeTo)
        this.triangleToWorld.set(triangleToWorld)
    }

    override fun processTriangle(a: Vector3d, b: Vector3d, c: Vector3d, partId: Int, triangleIndex: Int) {
        val triangleShape = TriangleShape(a, b, c)
        triangleShape.margin = triangleCollisionMargin

        val simplexSolver = Stack.newVSS()

        // TODO: implement ContinuousConvexCollision
        val castResult = Stack.newCastResult()
        castResult.fraction = 1f
        if (SubSimplexConvexCast.calcTimeOfImpactImpl(
                convexShape, triangleShape, simplexSolver,
                convexShapeFrom, convexShapeTo,
                triangleToWorld, triangleToWorld, castResult
            )
        ) {
            // add hit
            if (castResult.normal.lengthSquared() > 0.0001f &&
                castResult.fraction < hitFraction
            ) {
                // btContinuousConvexCast's normal is already in world space
                // rotate normal into world space
                // convexShapeFrom.basis.transform(castResult.normal);

                castResult.normal.normalize()
                reportHit(
                    castResult.normal,
                    castResult.hitPoint,
                    castResult.fraction,
                    partId, triangleIndex
                )
            }
        }

        Stack.subVSS(1)
        Stack.subCastResult(1)
    }

    abstract fun reportHit(
        hitNormalLocal: Vector3f,
        hitPointLocal: Vector3d,
        hitFraction: Float,
        partId: Int,
        triangleIndex: Int
    ): Float
}
