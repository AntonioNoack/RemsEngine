package me.anno.objects.animation

enum class Interpolation(
    val code: Int, val symbol: String,
    val displayName: String, val description: String
) {

    SPLINE(0, "S", "Spline", "Smooth curve"),
    LINEAR_BOUNDED(1, "/", "Linear", "Straight curve segments, mix(a,b,clamp(t,0,1))"),
    LINEAR_UNBOUNDED(2, "//", "Linear (unbounded)", "Straight curve segments, extending into infinity, mix(a,b,t)"),
    STEP(3, "L", "Step", "First half is the first value, second half is the second value, t > 0.5 ? a : b"),
    SINE(4, "~", "Sine", "Uses a cosine function, mix(a, b, (1-cos(pi*t))/2)")
    ;

    companion object {
        fun getType(code: Int) = values().firstOrNull { it.code == code } ?: SPLINE
    }

}