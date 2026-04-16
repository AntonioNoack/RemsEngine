package me.anno.mesh.usd

enum class ValueType {
    INVALID,
    BOOL,
    UCHAR,
    INT,
    UINT,
    LONG,
    ULONG,

    HALF,
    FLOAT,
    DOUBLE,

    STRING,
    TOKEN,
    ASSET_PATH,

    MATRIX2,
    MATRIX3,
    MATRIX4,

    QUATD,
    QUATF,
    QUATH,

    VEC2D,
    VEC2F,
    VEC2H,
    VEC2I,

    VEC3D,
    VEC3F,
    VEC3H,
    VEC3I,

    VEC4D,
    VEC4F,
    VEC4H,
    VEC4I,

    DICTIONARY,
    TOKEN_LIST,
    STRING_LIST,
    PATH_LIST,
    REFERENCE_LIST,

    INT_LIST,
    LONG_LIST,
    UINT_LIST,
    ULONG_LIST,

    PATH_VECTOR,
    TOKEN_VECTOR,
    SPECIFIER,
    PERMISSION,
    VARIABILITY,

    VARIANT_SELECTION_MAP,
    TIME_SAMPLES,
    PAYLOAD,
    DOUBLE_VECTOR,
    LAYER_OFFSET_VECTOR,
    STRING_VECTOR,
    VALUE_BLOCK,
    VALUE, // contains value rep
    UNREGISTERED_VALUE, // string or dict
    UNREGISTERED_VALUE_LIST,
    PAYLOAD_LIST,
    TIME_CODE;

    companion object {
        init {
            check(TIME_CODE.ordinal == 56)
        }
    }
}
