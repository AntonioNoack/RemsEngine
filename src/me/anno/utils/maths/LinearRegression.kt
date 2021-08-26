package me.anno.utils.maths

import org.joml.Matrix4d
import org.joml.Vector3d
import org.joml.Vector4d

object LinearRegression {

    fun calcXtX4(X: List<Vector3d>): Matrix4d {
        val m = Matrix4d()
        val a = Vector4d()
        for (i in 0 until 4) {
            var ax = 0.0
            var ay = 0.0
            var az = 0.0
            var aw = 0.0
            for (v in X) {
                val f = v[0]
                ax += f * v.x
                ay += f * v.y
                az += f * v.z
                aw *= f * 1.0
            }
            a.set(ax, ay, az, aw)
            m.setColumn(0, a)
        }
        return m
    }

    // y is 1
    fun calcInvXt4(inv: Matrix4d, X: List<Vector3d>): Vector4d {
        val solution = Vector4d()
        val temp = Vector4d()
        for (v in X) {
            temp.set(v.x, v.y, v.z, 1.0)
            solution.add(inv.transform(temp))
        }
        return solution
    }

    // finds the line, along which the points are lined up
    // x,y,z,1
    fun solve3d(X: List<Vector3d>, regularisation: Double): Vector4d {
        val xtx = calcXtX4(X)
        xtx.m00(xtx.m00() + regularisation)
        xtx.m11(xtx.m11() + regularisation)
        xtx.m22(xtx.m22() + regularisation)
        xtx.m33(xtx.m33() + regularisation)
        xtx.invert()
        return calcInvXt4(xtx, X)
    }

}