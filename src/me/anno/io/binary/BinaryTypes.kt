package me.anno.io.binary

@Suppress("unused")
object BinaryTypes {
    
    // for the values, a system would be nice
    // objects
    const val OBJECT_NULL = 0
    const val OBJECT_IMPL = 1
    const val OBJECT_ARRAY = 2
    const val OBJECT_ARRAY_2D = 3
    const val OBJECTS_HOMOGENOUS_ARRAY = 4
    const val OBJECT_LIST_UNKNOWN_LENGTH = 5
    const val OBJECT_PTR = 6

    // number types
    const val BOOL = 7
    const val BOOL_ARRAY = 8
    const val BOOL_ARRAY_2D = 9
    const val BYTE = 10
    const val BYTE_ARRAY = 11
    const val BYTE_ARRAY_2D = 12
    const val CHAR = 13
    const val CHAR_ARRAY = 14
    const val CHAR_ARRAY_2D = 15
    const val SHORT = 16
    const val SHORT_ARRAY = 17
    const val SHORT_ARRAY_2D = 18
    const val INT = 19
    const val INT_ARRAY = 20
    const val INT_ARRAY_2D = 21
    const val LONG = 22
    const val LONG_ARRAY = 23
    const val LONG_ARRAY_2D = 24
    const val FLOAT = 25
    const val FLOAT_ARRAY = 26
    const val FLOAT_ARRAY_2D = 27
    const val DOUBLE = 28
    const val DOUBLE_ARRAY = 29
    const val DOUBLE_ARRAY_2D = 30

    // strings
    const val STRING = 31
    const val STRING_ARRAY = 32
    const val STRING_ARRAY_2D = 33

    // single precision vectors
    const val VECTOR2F = 34
    const val VECTOR2F_ARRAY = 35
    const val VECTOR2F_ARRAY_2D = 36
    const val VECTOR3F = 37
    const val VECTOR3F_ARRAY = 38
    const val VECTOR3F_ARRAY_2D = 39
    const val VECTOR4F = 40
    const val VECTOR4F_ARRAY = 41
    const val VECTOR4F_ARRAY_2D = 42

    // double precision vectors
    const val VECTOR2D = 43
    const val VECTOR2D_ARRAY = 44
    const val VECTOR2D_ARRAY_2D = 45
    const val VECTOR3D = 46
    const val VECTOR3D_ARRAY = 47
    const val VECTOR3D_ARRAY_2D = 48
    const val VECTOR4D = 49
    const val VECTOR4D_ARRAY = 50
    const val VECTOR4D_ARRAY_2D = 51

    // quaternions
    const val QUATERNION32 = 52
    const val QUATERNION32_ARRAY = 53
    const val QUATERNION32_ARRAY_2D = 54
    const val QUATERNION64 = 55
    const val QUATERNION64_ARRAY = 56
    const val QUATERNION64_ARRAY_2D = 57

    // integer vectors
    const val VECTOR2I = 58
    const val VECTOR2I_ARRAY = 59
    const val VECTOR2I_ARRAY_2D = 60
    const val VECTOR3I = 61
    const val VECTOR3I_ARRAY = 62
    const val VECTOR3I_ARRAY_2D = 63
    const val VECTOR4I = 64
    const val VECTOR4I_ARRAY = 65
    const val VECTOR4I_ARRAY_2D = 66

    // single precision matrices
    const val MATRIX2X2F = 67
    const val MATRIX2X2F_ARRAY = 68
    const val MATRIX2X2F_ARRAY_2D = 69
    const val MATRIX3X2F = 70
    const val MATRIX3X2F_ARRAY = 71
    const val MATRIX3X2F_ARRAY_2D = 72
    const val MATRIX4X2F = 73
    const val MATRIX4X2F_ARRAY = 74
    const val MATRIX4X2F_ARRAY_2D = 75
    const val MATRIX3X3F = 76
    const val MATRIX3X3F_ARRAY = 77
    const val MATRIX3X3F_ARRAY_2D = 78
    const val MATRIX4X3F = 79
    const val MATRIX4X3F_ARRAY = 80
    const val MATRIX4X3F_ARRAY_2D = 81
    const val MATRIX4X4F = 82
    const val MATRIX4X4F_ARRAY = 83
    const val MATRIX4X4F_ARRAY_2D = 84

    // double precision matrices
    const val MATRIX2X2D = 85
    const val MATRIX2X2D_ARRAY = 86
    const val MATRIX2X2D_ARRAY_2D = 87
    const val MATRIX3X2D = 88
    const val MATRIX3X2D_ARRAY = 89
    const val MATRIX3X2D_ARRAY_2D = 90
    const val MATRIX4X2D = 91
    const val MATRIX4X2D_ARRAY = 92
    const val MATRIX4X2D_ARRAY_2D = 93
    const val MATRIX3X3D = 94
    const val MATRIX3X3D_ARRAY = 95
    const val MATRIX3X3D_ARRAY_2D = 96
    const val MATRIX4X3D = 97
    const val MATRIX4X3D_ARRAY = 98
    const val MATRIX4X3D_ARRAY_2D = 99
    const val MATRIX4X4D = 100
    const val MATRIX4X4D_ARRAY = 101
    const val MATRIX4X4D_ARRAY_2D = 102

    // other geometric types
    const val AABB32 = 103
    const val AABB32_ARRAY = 104
    const val AABB32_ARRAY_2D = 105
    const val AABB64 = 106
    const val AABB64_ARRAY = 107
    const val AABB64_ARRAY_2D = 108
    const val PLANE32 = 109
    const val PLANE32_ARRAY = 110
    const val PLANE32_ARRAY_2D = 111
    const val PLANE64 = 112
    const val PLANE64_ARRAY = 113
    const val PLANE64_ARRAY_2D = 114

    // files
    const val FILE = 115
    const val FILE_ARRAY = 116
    const val FILE_ARRAY_2D = 117
}