package me.anno.io.binary

object BinaryTypes {
    // objects
    const val OBJECT_NULL = 0
    const val OBJECT_IMPL = 1
    const val OBJECT_ARRAY = 2
    const val OBJECT_ARRAY_2D = 3
    const val OBJECTS_HOMOGENOUS_ARRAY = 4
    const val OBJECT_LIST_UNKNOWN_LENGTH = 5
    const val OBJECT_PTR = 6
    // all other types have been moved to SimpleType.scalarId + 0/1/2 for their scalars/arrays/2d-arrays
}