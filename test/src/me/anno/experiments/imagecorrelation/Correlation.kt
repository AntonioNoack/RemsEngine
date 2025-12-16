package me.anno.experiments.imagecorrelation

class Correlation {
    var sumI = 0f
    var sumT = 0f
    var sumII = 0f
    var sumTT = 0f
    var sumIT = 0f
    var n = 0

    fun push(i: Float, t: Float) {
        sumI += i
        sumT += t
        sumII += i * i
        sumTT += t * t
        sumIT += i * t
        n++
    }

    fun eval(): Float {
        val numerator = sumIT - (sumI * sumT / n)
        val denomI = sumII - (sumI * sumI / n)
        val denomT = sumTT - (sumT * sumT / n)

        return if (denomI <= 0 || denomT <= 0) 0f
        else numerator / kotlin.math.sqrt(denomI * denomT)
    }
}
