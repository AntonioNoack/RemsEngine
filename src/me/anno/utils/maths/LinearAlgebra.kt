package me.anno.utils.maths

import me.anno.utils.types.Floats.f5s
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * linear algebra functions
 * N = matrix
 * T = transposed matrix
 * V = vector
 * */
object LinearAlgebra {

    /**
     * C += At * B
     * */
    fun mulTN(
        a: DoubleArray, b: DoubleArray, m: Int, n: Int, k: Int,
        c: DoubleArray = DoubleArray(m * k)
    ): DoubleArray {
        if (a.size < m * n) throw RuntimeException("a has wrong dimensions")
        if (b.size < n * k) throw RuntimeException("b has wrong dimensions")
        if (c.size < m * k) throw RuntimeException("c has wrong dimensions")
        for (ni in 0 until n) {
            val mn = ni * m
            val kn = ni * k
            for (mi in 0 until m) {
                val ai = a[mn + mi]
                val mk = mi * k
                for (ki in 0 until k) {
                    c[mk + ki] += ai * b[kn + ki]
                }
            }
        }
        return c
    }

    /**
     * C += A * Bt
     * */
    fun mulNT(
        a: DoubleArray, b: DoubleArray, m: Int, n: Int, k: Int,
        c: DoubleArray = DoubleArray(m * k)
    ): DoubleArray {
        if (a.size < m * n) throw RuntimeException("a has wrong dimensions")
        if (b.size < n * k) throw RuntimeException("b has wrong dimensions")
        if (c.size < m * k) throw RuntimeException("c has wrong dimensions")
        for (mi in 0 until m) {
            val mk = mi * k
            for (ki in 0 until k) {
                var sum = 0.0
                var mn = mi * n
                var kn = ki * n
                for (ni in 0 until n) {
                    sum += a[mn++] * b[kn++]
                }
                c[mk + ki] = sum
            }
        }
        return c
    }

    /**
     * y += At * x
     * */
    fun mulTV(a: DoubleArray, x: DoubleArray, m: Int, n: Int, y: DoubleArray = DoubleArray(m)): DoubleArray {
        if (a.size < m * n) throw RuntimeException("a has wrong dimensions")
        if (x.size < n * 1) throw RuntimeException("x has wrong dimensions")
        if (y.size < m * 1) throw RuntimeException("y has wrong dimensions")
        for (ni in 0 until n) {
            val xi = x[ni]
            var nm = ni * m
            for (mi in 0 until m) {
                y[mi] += a[nm++] * xi
            }
        }
        return y
    }

    /**
     * C = A * B
     * */
    fun mulNN(
        a: DoubleArray,
        b: DoubleArray,
        m: Int,
        n: Int,
        k: Int,
        c: DoubleArray = DoubleArray(m * k)
    ): DoubleArray {
        if (a.size < m * n) throw RuntimeException("a has wrong dimensions")
        if (b.size < n * k) throw RuntimeException("b has wrong dimensions")
        if (c.size < m * k) throw RuntimeException("c has wrong dimensions")
        for (mi in 0 until m) {
            val mn = mi * n
            val mk = mi * k
            for (ki in 0 until k) {
                var sum = 0.0
                for (ni in 0 until n) {
                    sum += a[mn + ni] * b[ni * k + ki]
                }
                c[mk + ki] = sum
            }
        }
        return c
    }

    /**
     * y = Ax
     * */
    fun mulNV(a: DoubleArray, x: DoubleArray, m: Int, n: Int, y: DoubleArray = DoubleArray(m)): DoubleArray {
        if (a.size < m * n) throw RuntimeException("a has wrong dimensions")
        if (x.size < n * 1) throw RuntimeException("x has wrong dimensions")
        if (y.size < m * 1) throw RuntimeException("y has wrong dimensions")
        for (mi in 0 until m) {
            var sum = 0.0
            var mn = mi * n
            for (ni in 0 until n) {
                sum += a[mn++] * x[ni]
            }
            y[mi] = sum
        }
        return y
    }

    fun inv(a: DoubleArray, size: Int, b: DoubleArray = DoubleArray(size * size)): DoubleArray? {

        // make b a unit matrix
        b.fill(0.0)
        for (i in 0 until size) {
            b[i * (size + 1)] = 1.0
        }

        // solve the matrix
        fun score(f: Double): Double {
            return -abs(Maths.log(abs(f)))
        }
        for (mi in 0 until size) {

            // which column is optimized
            // find largest value in this column
            var largest = score(a[mi * (size + 1)])
            var bestRow = mi
            for (rowIndex in mi + 1 until size) {
                val v = score(a[rowIndex * size + mi])
                if (v > largest) {
                    largest = v
                    bestRow = rowIndex
                }
            }

            if (a[bestRow * size + mi] == 0.0) return null // matrix is not invertible

            // switch the rows mi and bestRow
            if (bestRow != mi) {
                switchRow(a, size, bestRow, mi)
                switchRow(b, size, bestRow, mi)
                bestRow = mi
            }

            val offset0 = bestRow * size
            largest = a[offset0 + mi]

            // subtract a[i,j]/largest from every other row
            for (row in 0 until size) {
                if (row != bestRow) {
                    val offsetI = row * size
                    val factor = a[offsetI + mi] / largest
                    a[offsetI + mi] = 0.0
                    for (i in mi + 1 until size) {
                        a[offsetI + i] -= factor * a[offset0 + i]
                    }
                    subRow(b, size, bestRow, row, factor)
                }
            }
            // divide this row
            a[offset0 + mi] = 1.0
            for (ni in mi + 1 until size) {
                a[offset0 + ni] /= largest
            }
            divRow(b, size, bestRow, largest)
        }
        return b
    }

    private fun subRow(a: DoubleArray, n: Int, ri: Int, rj: Int, factor: Double) {
        val offset0 = ri * n
        val offset1 = rj * n
        for (ni in 0 until n) {
            a[offset1 + ni] -= factor * a[offset0 + ni]
        }
    }

    private fun divRow(a: DoubleArray, n: Int, ri: Int, factor: Double) {
        val offset0 = ri * n
        for (ni in 0 until n) {
            a[offset0 + ni] /= factor
        }
    }

    private fun switchRow(a: DoubleArray, n: Int, ri: Int, rj: Int) {
        if (ri != rj) {
            val offset0 = ri * n
            val offset1 = rj * n
            for (ni in 0 until n) {
                val i0 = offset0 + ni
                val i1 = offset1 + ni
                val ta = a[i0]
                a[i0] = a[i1]
                a[i1] = ta
            }
        }
    }

    fun printMatrix(a: DoubleArray, m: Int = sqrt(a.size.toDouble()).toInt(), n: Int = m) {
        val str = StringBuilder()
        for (mi in 0 until m) {
            val mn = mi * n
            for (ni in 0 until n - 1) {
                str.append(a[mn + ni].f5s())
                str.append(' ')
            }
            str.append(a[mn + n - 1].f5s())
            println(str)
            str.clear()
        }
    }

}