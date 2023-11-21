package me.anno.io.json.saveable

/**
 * todo use this for binary serializers, too
 *
 * todo use this everywhere, where necessary
 * */
enum class SimpleType(val scalar: String) {

    BOOLEAN("b"),
    CHAR("c"),
    BYTE("B"),
    SHORT("s"),
    INT("i"),
    LONG("l"),
    FLOAT("f"),
    DOUBLE("d"),
    STRING("S"),
    REFERENCE("R"),

    VECTOR2F("v2"),
    VECTOR2D("v2d"),
    VECTOR2I("v2i"),
    VECTOR3F("v3"),
    VECTOR3D("v3d"),
    VECTOR3I("v3i"),
    VECTOR4F("v4"),
    VECTOR4D("v4d"),
    VECTOR4I("v4i"),

    MATRIX2X2F("mat2"),
    MATRIX3X3F("mat3"),
    MATRIX4X4F("mat4"),
    MATRIX3X2F("mat3x2"),
    MATRIX4X3F("mat4x3"),

    MATRIX2X2D("mat2d"),
    MATRIX3X3D("mat3d"),
    MATRIX4X4D("mat4d"),
    MATRIX3X2D("mat3x2d"),
    MATRIX4X3D("mat4x3d"),

    QUATERNIONF("q4"),
    QUATERNIOND("q4d"),

    PLANEF("p4"),
    PLANED("p4d"),

    COLOR("col"),
    ;

    val array = "$scalar[]"
    val array2d = "$scalar[][]"
}