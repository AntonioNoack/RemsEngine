package me.anno.objects.animation

enum class Interpolation(val code: Int, val symbol: String, val displayName: String){

    SPLINE(0, "S", "Spline"),
    LINEAR_BOUNDED(1, "/", "Linear"),
    LINEAR_UNBOUNDED(2, "//", "Linear (unbounded)"),
    STEP(3, "L", "Step"),
    SINE(4, "~", "Sine")
    ;

    companion object {
        fun getType(code: Int) = values().firstOrNull { it.code == code } ?: SPLINE
    }

}