package me.anno.maths.geometry

import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.length
import me.anno.maths.MinMax.max
import me.anno.maths.Maths.sq
import me.anno.maths.optimization.GradientDescent.simplexAlgorithm
import me.anno.maths.geometry.MarchingSquares.findZero
import me.anno.utils.structures.lists.Lists.createArrayList
import me.anno.utils.types.Booleans.toInt
import org.joml.Matrix3f
import org.joml.Vector3f
import org.joml.Vector4f

/**
 * theoretically nice, practically pretty bad :/
 * use MarchingCubes instead
 * */
object DualContouring3d {

    fun interface Func3d {
        fun calc(x: Float, y: Float, z: Float): Float
    }

    fun interface Grad3d {
        fun calc(x: Float, y: Float, z: Float, dst: Vector3f)
    }

    class QEF3d(val w: Float) {

        val m = Matrix3f()
        val v = Vector3f()

        val avg = Vector4f()

        fun reset() {
            m.zero()
            v.zero()
            avg.zero()
            // half weight for bias towards zero
            add(0.5f, 0.5f, 0.5f, +w, 0f, 0f)
            add(0.5f, 0.5f, 0.5f, 0f, +w, 0f)
            add(0.5f, 0.5f, 0.5f, 0f, 0f, +w)
        }

        fun add(px: Float, py: Float, pz: Float, g: Vector3f) {
            g.normalize()
            add(px, py, pz, g.x, g.y, g.z, 1f)
        }

        /**
         * add ((x-px)*dx + (y-py)*dy + (z-z0)*dz)² as terms to the equation
         * */
        fun add(
            px: Float, py: Float, pz: Float,
            dx: Float, dy: Float, dz: Float,
            w: Float = length(dx, dy, dz)
        ) {

            avg.add(px * w, py * w, pz * w, w)

            val dx2 = dx * dx
            val dy2 = dy * dy
            val dz2 = dz * dz

            // x²
            m.m00 += 2f * dx2
            v.x += 2f * dx2 * px

            // y²
            m.m11 += 2f * dy2
            v.y += 2f * dy2 * py

            // z²
            m.m22 += 2f * dz2
            v.z += 2f * dz2 * pz

            // 2xy
            val dxy = dx * dy * 2f
            m.m01 += dxy
            v.x += py * dxy
            v.y += px * dxy

            // 2yz
            val dyz = dy * dz * 2f
            m.m12 += dyz
            v.y += pz * dyz
            v.z += py * dyz

            // 2zx
            val dzx = dz * dx * 2f
            m.m02 += dzx
            v.z += px * dzx
            v.x += pz * dzx
        }

        /**
         * find minimum point of equation within cell boundaries
         * */
        fun findExtremum(dst: Vector3f): Vector3f {

            // todo this isn't ideal yet...

            m.m10 = m.m01 // required
            m.m20 = m.m02
            m.m21 = m.m12
            m.invert().transform(v, dst)

            // if out of bounds, use average... not
            // the best solution, but at least sth
            if (!(dst.x in 0f..1f && dst.y in 0f..1f && dst.z in 0f..1f)) {
                dst.set(clamp(dst.x), clamp(dst.y), clamp(dst.z))
                // dst.set(avg.x, avg.y, avg.z).div(avg.w)
            }

            return dst
        }
    }

    fun gradient(func: Func3d): Grad3d {
        return Grad3d { x, y, z, dst ->
            val e = 1f
            val gx = func.calc(x + e, y, z) - func.calc(x - e, y, z)
            val gy = func.calc(x, y + e, z) - func.calc(x, y - e, z)
            val gz = func.calc(x, y, z + e) - func.calc(x, y, z - e)
            dst.set(gx, gy, gz).mul(0.5f / e)
        }
    }

    fun x(
        v0: Float, v1: Float,
        x0: Float, y0: Float, z0: Float,
        dy: Float, dz: Float,
        g: Vector3f, qef: QEF3d, gradient: Grad3d
    ) {
        val dx = findZero(v0, v1)
        gradient.calc(x0 + dx, y0, z0, g)
        qef.add(dx, dy, dz, g)
    }

    fun y(
        v0: Float, v1: Float,
        x0: Float, y0: Float, z0: Float,
        dx: Float, dz: Float,
        g: Vector3f, qef: QEF3d, gradient: Grad3d
    ) {
        val dy = findZero(v0, v1)
        gradient.calc(x0, y0 + dy, z0, g)
        qef.add(dx, dy, dz, g)
    }

    fun z(
        v0: Float, v1: Float,
        x0: Float, y0: Float, z0: Float,
        px: Float, py: Float,
        g: Vector3f, qef: QEF3d, gradient: Grad3d
    ) {
        val dz = findZero(v0, v1)
        gradient.calc(x0, y0, z0 + dz, g)
        qef.add(px, py, dz, g)
    }

    fun findBestVertex3d(
        i0: Int, diy: Int, diz: Int,
        values: FloatArray,
        fn: Func3d,
        gr: Grad3d,
        x0: Float, x1: Float,
        y0: Float, y1: Float,
        z0: Float, z1: Float,
        g: Vector3f, q: QEF3d,
        wi: Int,
        vertices: ArrayList<Vector3f>,
    ) {
        val v0 = values[i0]
        val v1 = values[i0 + diz]
        val v2 = values[i0 + diy]
        val v3 = values[i0 + diy + diz]
        val v4 = values[i0 + 1]
        val v5 = values[i0 + 1 + diz]
        val v6 = values[i0 + diy + 1]
        val v7 = values[i0 + diy + 1 + diz]
        val b0 = v0 > 0f
        val b1 = v1 > 0f
        val b2 = v2 > 0f
        val b3 = v3 > 0f
        val b4 = v4 > 0f
        val b6 = v6 > 0f
        val b5 = v5 > 0f
        val b7 = v7 > 0f
        val ctr = b0.toInt() + b1.toInt() + b2.toInt() + b3.toInt() +
                b4.toInt() + b5.toInt() + b6.toInt() + b7.toInt()
        if ((ctr and 7) != 0) {

            q.reset()

            // all edges of the cell
            if (b0 != b1) z(v0, v1, x0, y0, z0, 0f, 0f, g, q, gr)
            if (b2 != b3) z(v2, v3, x0, y1, z0, 0f, 1f, g, q, gr)
            if (b4 != b5) z(v4, v5, x1, y0, z0, 1f, 0f, g, q, gr)
            if (b6 != b7) z(v6, v7, x1, y1, z0, 1f, 1f, g, q, gr)

            if (b0 != b2) y(v0, v2, x0, y0, z0, 0f, 0f, g, q, gr)
            if (b1 != b3) y(v1, v3, x0, y0, z1, 0f, 1f, g, q, gr)
            if (b4 != b6) y(v4, v6, x1, y0, z0, 1f, 0f, g, q, gr)
            if (b5 != b7) y(v5, v7, x1, y0, z1, 1f, 1f, g, q, gr)

            if (b0 != b4) x(v0, v4, x0, y0, z0, 0f, 0f, g, q, gr)
            if (b1 != b5) x(v1, v5, x0, y0, z1, 0f, 1f, g, q, gr)
            if (b2 != b6) x(v2, v6, x0, y1, z0, 1f, 0f, g, q, gr)
            if (b3 != b7) x(v3, v7, x0, y1, z1, 1f, 1f, g, q, gr)

            // find intersection between different normals
            q.findExtremum(g)

            // optimize the theoretical extremum
            val s = simplexAlgorithm(
                floatArrayOf(g.x + x0, g.y + y0, g.z + z0),
                0.25f, 0f, 32
            ) { params ->
                val (px, py, pz) = params
                sq(fn.calc(px, py, pz)) +
                        10f * (0f +
                        max(0f, x0 - px) +
                        max(0f, y0 - py) +
                        max(0f, z0 - pz) +
                        max(0f, px - x1) +
                        max(0f, py - y1) +
                        max(0f, pz - z1)
                        )
            }.second

            vertices[wi] = Vector3f(s)
        }
    }

    fun contour3d(
        sx: Int, sy: Int, sz: Int,
        func: Func3d,
        grad: Grad3d = gradient(func)
    ): List<Vector3f> {
        val pointsInGrid = (sx + 1) * (sy + 1) * (sz + 1)
        // calculate all positions and all gradients
        val values = FloatArray(pointsInGrid)
        // fill positions & gradients
        var vIndex = 0
        for (z in 0..sz) {
            val pz = z.toFloat()
            for (y in 0..sy) {
                val py = y.toFloat()
                for (x in 0..sx) {
                    val px = x.toFloat()
                    values[vIndex++] = func.calc(px, py, pz)
                }
            }
        }
        return contour3d(sx, sy, sz, values, func, grad)
    }

    fun contour3d(
        sx: Int, sy: Int, sz: Int,
        values: FloatArray, function: Func3d, gradient: Grad3d
    ): List<Vector3f> {
        val invalid = Vector3f()
        val vertices = createArrayList(sx * sy * sz, invalid)
        var writeIndex = 0
        var vIndex = 0
        val diy = sx + 1
        val diz = (sy + 1) * diy
        val qef = QEF3d(0.01f)
        val tmp = Vector3f()
        for (z in 0 until sz) {
            val z0 = z.toFloat()
            val z1 = z0 + 1f
            for (y in 0 until sy) {
                val y0 = y.toFloat()
                val y1 = y0 + 1f
                for (x in 0 until sx) {
                    val x0 = x.toFloat()
                    val x1 = x0 + 1f
                    findBestVertex3d(
                        vIndex++, diy, diz, values,
                        function, gradient,
                        x0, x1, y0, y1, z0, z1,
                        tmp, qef, writeIndex++, vertices
                    )
                }
                vIndex++
            }
            vIndex += diy
        }

        val faces = ArrayList<Vector3f>()
        val sx1 = sx + 1
        val sy1 = sy + 1
        val sxy = sx * sy
        val sxy1 = sx1 * sy1
        for (z in 0 until sz) {
            for (y in 0 until sy) {
                for (x in 0 until sx) {
                    val vi = x + sx1 * (y + sy1 * z)
                    val vk = x + sx * (y + sy * z)
                    val b0 = values[vi] > 0f
                    if (y > 0 && z > 0) { // dx faces
                        val vj = vi + 1
                        if (b0 != (values[vj] > 0f)) {
                            faces += vertices[vk - sx - sxy]
                            if (b0) {
                                faces += vertices[vk - sx]
                                faces += vertices[vk - sxy]
                            } else {
                                faces += vertices[vk - sxy]
                                faces += vertices[vk - sx]
                            }
                            faces += vertices[vk]
                        }
                    }
                    if (x > 0 && z > 0) { // dy faces
                        val vj = vi + sx1
                        if (b0 != (values[vj] > 0f)) {
                            faces += vertices[vk - 1 - sxy]
                            if (b0) {
                                faces += vertices[vk - sxy]
                                faces += vertices[vk - 1]
                            } else {
                                faces += vertices[vk - 1]
                                faces += vertices[vk - sxy]
                            }
                            faces += vertices[vk]
                        }
                    }
                    if (x > 0 && y > 0) { // dz faces
                        val vj = vi + sxy1
                        if (b0 != (values[vj] > 0f)) {
                            faces += vertices[vk - 1 - sx]
                            if (b0) {
                                faces += vertices[vk - 1]
                                faces += vertices[vk - sx]
                            } else {
                                faces += vertices[vk - sx]
                                faces += vertices[vk - 1]
                            }
                            faces += vertices[vk]
                        }
                    }
                }
            }
        }
        return faces
    }
}