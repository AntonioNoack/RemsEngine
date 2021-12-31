package me.anno.image.gimp

enum class DataType(val value: Int, val bpp: Int) {

    U8_LINEAR(100, 1),
    U8_NON_LINEAR(150, 1),
    U8_PERCEPTUAL(175, 1),
    U16_LINEAR(200, 2),
    U16_NON_LINEAR(250, 2),
    U16_PERCEPTUAL(275, 2),
    U32_LINEAR(300, 4),
    U32_NON_LINEAR(350, 4),
    U32_PERCEPTUAL(375, 4),
    HALF_LINEAR(500, 2),
    HALF_NON_LINEAR(550, 2),
    HALF_PERCEPTUAL(575, 2),
    FLOAT_LINEAR(600, 4),
    FLOAT_NON_LINEAR(650, 4),
    FLOAT_PERCEPTUAL(675, 4),
    DOUBLE_LINEAR(700, 8),
    DOUBLE_NON_LINEAR(750, 8),
    DOUBLE_PERCEPTUAL(775, 8);

    companion object {

        val valueById = values().associateBy { it.value }

    }
}