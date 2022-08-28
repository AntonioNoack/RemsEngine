package me.anno.tests

fun main() {
    for (j in 2 until 100) {
        val vs = FloatArray(j) { (it + 1f) / j.toFloat() }
        val counts = IntArray(vs.size)
        for (i in 0 until j * 1000) {
            val searched = Maths.random()
            var i0 = -1
            var i1 = vs.size - 1
            while (i1 > i0) {
                val im = (i0 + i1) shr 1
                if (im < 0) break
                val v = vs[im]
                if (searched < v) {
                    i1 = im
                } else if (i0 < im) {
                    i0 = im
                } else break
            }
            counts[i0 + 1]++
            if (!((if (i0 < 0) 0f else vs[i0]) <= searched && searched <= vs[i1])) {
                throw IllegalStateException("$j, [$i0, $i1] -> [${vs.joinToString()}], ${vs[i0]} <= $searched <= ${vs[i1]}")
            }
        }
        println(counts.joinToString())
    }
}