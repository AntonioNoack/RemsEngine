package me.anno.mesh.blender.impl.attr

/**
 * https://github.com/blender/blender/blob/df05d3baea4fd8b210243ee226cea00e14b12e6d/source/blender/blenkernel/BKE_attribute.hh#L53
 * */
enum class AttributeType {
    Bool,
    Int8,
    Int16_2D,
    Int32,
    Int32_2D,
    Float,
    Float2,
    Float3,
    Float4x4,
    ColorByte,
    ColorFloat,
    Quaternion,
    String,
}