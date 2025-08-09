package me.anno.maths.optimization

object Bisection {

    fun bisect(min: Float, max: Float, objective: FloatBisectionFunction): Float {
        var min = min
        var max = max
        repeat(100) {
            val middle = (min + max) * 0.5f
            if (objective.calc(middle)) {
                if (min >= middle) return@repeat
                min = middle
            } else {
                if (max <= middle) return@repeat
                max = middle
            }
        }
        return (min + max) * 0.5f
    }

    fun bisect(min: Double, max: Double, objective: DoubleBisectionFunction): Double {
        var min = min
        var max = max
        repeat(100) {
            val middle = (min + max) * 0.5
            if (objective.calc(middle)) {
                if (min >= middle) return@repeat
                min = middle
            } else {
                if (max <= middle) return@repeat
                max = middle
            }
        }
        return (min + max) * 0.5
    }
}