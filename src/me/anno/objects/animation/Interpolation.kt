package me.anno.objects.animation

val leValues = HashMap<Int, Interpolation>()
enum class Interpolation(val code: Int){
    LINEAR_BOUNDED(0),
    LINEAR_UNBOUNDED(1),
    STEP(2);

    init {
        leValues[code] = this
    }

    companion object {
        fun getType(code: Int) = leValues[code] ?: LINEAR_BOUNDED
    }

}