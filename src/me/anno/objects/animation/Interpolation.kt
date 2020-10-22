package me.anno.objects.animation

enum class Interpolation(val code: Int, val displayName: String){

    SPLINE(0, "Spline"),
    LINEAR_BOUNDED(1, "Linear"),
    LINEAR_UNBOUNDED(2, "Linear (unbounded)"),
    STEP(3, "Step"),
    ;

    companion object {
        fun getType(code: Int) = values().firstOrNull { it.code == code } ?: SPLINE
    }

}