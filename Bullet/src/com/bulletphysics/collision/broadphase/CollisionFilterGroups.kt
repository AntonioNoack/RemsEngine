package com.bulletphysics.collision.broadphase

/**
 * Common collision filter groups.
 *
 * @author jezek2
 */
object CollisionFilterGroups {
    const val DEFAULT_FILTER: Short = 1
    const val STATIC_FILTER: Short = 2
    const val KINEMATIC_FILTER: Short = 4
    const val DEBRIS_FILTER: Short = 8
    const val SENSOR_TRIGGER: Short = 16
    const val CHARACTER_FILTER: Short = 32

    /**
     *  all bits sets: DefaultFilter | StaticFilter | KinematicFilter | DebrisFilter | SensorTrigger
     * */
    const val ALL_FILTER: Short = -1
}
