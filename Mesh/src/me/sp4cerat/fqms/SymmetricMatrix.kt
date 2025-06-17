package me.sp4cerat.fqms

class SymmetricMatrix() {

    val m = DoubleArray(10)

    /**
     * Define a plane
     * */
    fun set(nx: Double, ny: Double, nz: Double, nw: Double) {
        m[0] = nx * nx
        m[1] = nx * ny
        m[2] = nx * nz
        m[3] = nx * nw
        m[4] = ny * ny
        m[5] = ny * nz
        m[6] = ny * nw
        m[7] = nz * nz
        m[8] = nz * nw
        m[9] = nw * nw
    }

    operator fun plusAssign(other: SymmetricMatrix) {
        for (i in m.indices) {
            m[i] += other.m[i]
        }
    }

    operator fun get(idx: Int): Double {
        return m[idx]
    }

    fun add(other: SymmetricMatrix, dst: SymmetricMatrix): SymmetricMatrix {
        val sum = dst
        for (i in m.indices) {
            sum.m[i] = m[i] + other.m[i]
        }
        return sum
    }

    fun det(
        a11: Int, a12: Int, a13: Int,
        a21: Int, a22: Int, a23: Int,
        a31: Int, a32: Int, a33: Int
    ): Double {
        val m = m
        return m[a11] * m[a22] * m[a33] + m[a13] * m[a21] * m[a32] +
                m[a12] * m[a23] * m[a31] - m[a13] * m[a22] * m[a31] -
                m[a11] * m[a23] * m[a32] - m[a12] * m[a21] * m[a33]
    }
}