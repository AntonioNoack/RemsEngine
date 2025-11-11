package com.bulletphysics.collision.broadphase

/**
 * Dispatcher uses these types.
 *
 * IMPORTANT NOTE: The types are ordered polyhedral, implicit convex and concave
 * to facilitate type checking.
 *
 * @author jezek2
 */
enum class BroadphaseNativeType {
    // polyhedral convex shapes:
    CUBOID,
    TRIANGLE,
    TETRAHEDRAL,
    CONVEX_TRIANGLE_MESH,
    CONVEX_HULL,

    // implicit convex shapes:
    SPHERE,
    MULTI_SPHERE,
    CAPSULE,
    CONE,
    CONVEX,
    CYLINDER,
    UNIFORM_SCALING,
    MINKOWSKI_SUM,
    MINKOWSKI_DIFFERENCE,

    // concave shapes:
    CONCAVE_SHAPES_START_HERE,

    // keep all the convex shapetype below here, for the check IsConvexShape in broadphase proxy!
    CONCAVE_TRIANGLE_MESH,
    CONCAVE_SCALED_TRIANGLE_MESH,

    CONCAVE_TERRAIN,
    CONCAVE_SIGNED_DISTANCE_FIELD,
    CONCAVE_GIMPACT_TRIANGLE_MESH,

    // multimaterial mesh:
    MULTIMATERIAL_TRIANGLE_MESH,

    EMPTY,
    STATIC_PLANE,
    CONCAVE_SHAPES_END_HERE,

    COMPOUND,
    SOFTBODY,

    ;

    val isConvex: Boolean
        get() = (ordinal < CONCAVE_SHAPES_START_HERE.ordinal)

    val isConcave: Boolean
        get() = ((ordinal > CONCAVE_SHAPES_START_HERE.ordinal) &&
                (ordinal < CONCAVE_SHAPES_END_HERE.ordinal))

    val isCompound: Boolean
        get() = this == COMPOUND

    val isSoftBody: Boolean
        get() = this == SOFTBODY

}
