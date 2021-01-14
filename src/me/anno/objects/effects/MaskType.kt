package me.anno.objects.effects

import me.anno.language.translation.NameDesc

enum class MaskType(val id: Int, val nameing: NameDesc){
    MASKING(0, NameDesc("Masking", "", "obj.maskType.masking")),
    PIXELATING(1, NameDesc("Pixelating", "", "obj.maskType.pixelating")),
    GAUSSIAN_BLUR(2, NameDesc("Gaussian Blur", "", "obj.maskType.gaussianBlur")),
    RADIAL_BLUR_1(7, NameDesc("Radial Blur (1)", "", "obj.maskType.radialBlur1")),
    RADIAL_BLUR_2(8, NameDesc("Radial Blur (2)", "", "obj.maskType.radialBlur2")),
    BOKEH_BLUR(3, NameDesc("Bokeh Blur", "", "obj.maskType.bokehBlur")),
    BLOOM(5, NameDesc("Bloom", "", "obj.maskType.bloom")),
    UV_OFFSET(4, NameDesc("Per-Pixel Offset", "", "obj.maskType.pixelOffset")),
    GREEN_SCREEN(6, NameDesc("Green-Screen", "", "obj.maskType.greenScreen"))
}