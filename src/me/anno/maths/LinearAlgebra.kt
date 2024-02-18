package me.anno.maths

import me.anno.utils.types.Floats.f5s
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * linear algebra functions
 * N = matrix
 * T = transposed matrix
 * V = vector
 *
 * row major
 * */
@Suppress("unused")
object LinearAlgebra {

    private fun checkDimensions(
        a: DoubleArray, b: DoubleArray, c: DoubleArray,
        m: Int, n: Int, k: Int
    ) {
        if (a.size != m * n) throw RuntimeException("a has incorrect size, expected $m * $n, got ${a.size}")
        if (b.size != n * k) throw RuntimeException("b has incorrect size, expected $n * $k, got ${b.size}")
        if (c.size != m * k) throw RuntimeException("c has incorrect size, expected $m * $k, got ${c.size}")
    }

    /**
     * C += At * B
     * */
    fun addAtB(
        a: DoubleArray, b: DoubleArray, m: Int, n: Int, k: Int,
        c: DoubleArray
    ): DoubleArray {
        checkDimensions(a, b, c, m, n, k)
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
     * C = At * B
     * */
    fun setAtB(
        a: DoubleArray, b: DoubleArray, m: Int, n: Int, k: Int,
        c: DoubleArray = DoubleArray(m * k)
    ): DoubleArray {
        checkDimensions(a, b, c, m, n, k)
        c.fill(0.0)
        return addAtB(a, b, m, n, k, c)
    }

    /**
     * C += A * Bt
     * */
    fun addABt(
        a: DoubleArray, b: DoubleArray, m: Int, n: Int, k: Int,
        c: DoubleArray
    ): DoubleArray {
        checkDimensions(a, b, c, m, n, k)
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
     * C = A * Bt
     * */
    fun setABt(
        a: DoubleArray, b: DoubleArray, m: Int, n: Int, k: Int,
        c: DoubleArray = DoubleArray(m * k)
    ): DoubleArray {
        checkDimensions(a, b, c, m, n, k)
        c.fill(0.0)
        return addABt(a, b, m, n, k, c)
    }

    /**
     * c += At * b
     * */
    fun addAtX(a: DoubleArray, b: DoubleArray, m: Int, n: Int, c: DoubleArray): DoubleArray {
        return addAtB(a, b, m, n, 1, c)
    }

    /**
     * c = At * b
     * */
    fun setAtX(a: DoubleArray, b: DoubleArray, m: Int, n: Int, c: DoubleArray = DoubleArray(m)): DoubleArray {
        return setAtB(a, b, m, n, 1, c)
    }

    /**
     * C = A * B
     * */
    fun addAB(
        a: DoubleArray, b: DoubleArray,
        m: Int, n: Int, k: Int,
        c: DoubleArray = DoubleArray(m * k)
    ): DoubleArray {
        checkDimensions(a, b, c, m, n, k)
        for (mi in 0 until m) {
            val mn = mi * n
            val mk = mi * k
            for (ki in 0 until k) {
                var sum = 0.0
                for (ni in 0 until n) {
                    sum += a[mn + ni] * b[ni * k + ki]
                }
                c[mk + ki] += sum
            }
        }
        return c
    }

    /**
     * C = A * B
     * */
    fun setAB(
        a: DoubleArray, b: DoubleArray,
        m: Int, n: Int, k: Int,
        c: DoubleArray = DoubleArray(m * k)
    ): DoubleArray {
        checkDimensions(a, b, c, m, n, k)
        c.fill(0.0)
        return addAB(a, b, m, n, k, c)
    }

    /**
     * c = Ab
     * */
    fun setAx(a: DoubleArray, b: DoubleArray, m: Int, n: Int, c: DoubleArray = DoubleArray(m)): DoubleArray {
        return setAB(a, b, m, n, 1, c)
    }

    /**
     * calculates the inverse of matrix a; destroys a in the process
     * */
    fun inverse(a: DoubleArray, size: Int, b: DoubleArray = DoubleArray(size * size)): DoubleArray? {

        if (a.size != size * size || b.size != size * size)
            throw RuntimeException("Dimensions don't match array size, expected $size² = ${size * size}, got ${a.size}, ${b.size}")

        // make b a unit matrix
        b.fill(0.0)
        for (i in 0 until size) {
            b[i * (size + 1)] = 1.0
        }

        // solve the matrix
        fun score(f: Double): Double {
            return abs(f)
        }

        for (m in 0 until size) {

            // which column is optimized
            // find the largest value in this column
            var largest = score(a[m * (size + 1)])
            var bestRow = m
            for (rowIndex in m + 1 until size) {
                val v = score(a[rowIndex * size + m])
                if (v > largest) {
                    largest = v
                    bestRow = rowIndex
                }
            }

            if (a[bestRow * size + m] == 0.0) { // matrix is not invertible
                // LOGGER.warn("$size x $size matrix is not invertible, ${a.size}, ${b.size}")
                return null
            }

            // switch the rows m and bestRow
            if (bestRow != m) {
                switchRow(a, size, bestRow, m)
                switchRow(b, size, bestRow, m)
                bestRow = m
            }

            val offset0 = bestRow * size
            largest = a[offset0 + m]

            // subtract a[i,j]/largest from every other row
            for (row in 0 until size) {
                if (row != bestRow) {
                    val offsetI = row * size
                    val factor = a[offsetI + m] / largest
                    a[offsetI + m] = 0.0
                    for (i in m + 1 until size) {
                        a[offsetI + i] -= factor * a[offset0 + i]
                    }
                    subRow(b, size, bestRow, row, factor)
                }
            }
            // divide this row
            a[offset0 + m] = 1.0
            for (ni in m + 1 until size) {
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

    fun printMatrix(a: DoubleArray, m: Int = sqrt(a.size.toDouble()).toInt(), n: Int = a.size / m) {
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