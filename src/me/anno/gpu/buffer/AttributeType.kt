package me.anno.gpu.buffer

@Suppress("unused")
enum class AttributeType(val byteSize: Int, val normalized: Boolean) {

    HALF(2, false),
    FLOAT(4, false),
    DOUBLE(8, false),

    UINT8(1, false),
    UINT16(2, false),
    UINT32(4, false),
    SINT8(1, false),
    SINT16(2, false),
    SINT32(4, false),

    UINT8_NORM(1, true),
    UINT16_NORM(2, true),
    UINT32_NORM(4, true),
    SINT8_NORM(1, true),
    SINT16_NORM(2, true),
    SINT32_NORM(4, true),

}