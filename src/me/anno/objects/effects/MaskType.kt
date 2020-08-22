package me.anno.objects.effects

enum class MaskType(val id: Int, val displayName: String){
    MASKING(0, "Masking"),
    PIXELATING(1, "Pixelating"),
    GAUSSIAN_BLUR(2, "Gaussian Blur"),
    BOKEH_BLUR(3, "Bokeh Blur")
}