package me.anno.io.json.saveable

/**
 * gives types a name / id for serialization
 * */
enum class SimpleType(val scalar: String, val scalarId: Int) {

    BOOLEAN("b", 7),
    BYTE("B", 10),
    CHAR("c", 13),
    SHORT("s", 16),
    INT("i", 19),
    LONG("l", 22),
    FLOAT("f", 25),
    DOUBLE("d", 28),
    STRING("S", 31),
    REFERENCE("R", 115),

    VECTOR2F("v2", 34),
    VECTOR3F("v3", 37),
    VECTOR4F("v4", 40),

    VECTOR2D("v2d", 43),
    VECTOR3D("v3d", 46),
    VECTOR4D("v4d", 49),

    VECTOR2I("v2i", 58),
    VECTOR3I("v3i", 61),
    VECTOR4I("v4i", 64),

    MATRIX2X2F("mat2", 67),
    MATRIX3X2F("mat3x2", 70),
    MATRIX3X3F("mat3", 76),
    MATRIX4X3F("mat4x3", 79),
    MATRIX4X4F("mat4", 82),

    MATRIX2X2D("mat2d", 85),
    MATRIX3X2D("mat3x2d", 88),
    MATRIX3X3D("mat3d", 94),
    MATRIX4X3D("mat4x3d", 97),
    MATRIX4X4D("mat4d", 100),

    QUATERNIONF("q4", 52),
    QUATERNIOND("q4d", 55),

    PLANEF("p4", 109),
    PLANED("p4d", 112),

    AABBF("AABBf", 103),
    AABBD("AABBd", 106),

    COLOR("col", -1),
    ;

    val array = "$scalar[]"
    val array2d = "$scalar[][]"
}