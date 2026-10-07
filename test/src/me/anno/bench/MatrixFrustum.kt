package me.anno.bench

import org.joml.Matrix4d

class MatrixFrustum(m: Matrix4d) {

    val nxX = m.m03 + m.m00
    val nxY = m.m13 + m.m10
    val nxZ = m.m23 + m.m20
    val nxW = m.m33 + m.m30
    val pxX = m.m03 - m.m00
    val pxY = m.m13 - m.m10
    val pxZ = m.m23 - m.m20
    val pxW = m.m33 - m.m30
    val nyX = m.m03 + m.m01
    val nyY = m.m13 + m.m11
    val nyZ = m.m23 + m.m21
    val nyW = m.m33 + m.m31
    val pyX = m.m03 - m.m01
    val pyY = m.m13 - m.m11
    val pyZ = m.m23 - m.m21
    val pyW = m.m33 - m.m31
    val nzX = m.m03 + m.m02
    val nzY = m.m13 + m.m12
    val nzZ = m.m23 + m.m22
    val nzW = m.m33 + m.m32
    val pzX = m.m03 - m.m02
    val pzY = m.m13 - m.m12
    val pzZ = m.m23 - m.m22
    val pzW = m.m33 - m.m32

    // from JOML, too, they seem to often change their classes why ever
    fun testAab(minX: Double, minY: Double, minZ: Double, maxX: Double, maxY: Double, maxZ: Double): Boolean {
        return nxX * (if (nxX < 0.0) minX else maxX) + nxY * (if (nxY < 0.0) minY else maxY) + nxZ * (if (nxZ < 0.0) minZ else maxZ) >= -nxW &&
                pxX * (if (pxX < 0.0) minX else maxX) + pxY * (if (pxY < 0.0) minY else maxY) + pxZ * (if (pxZ < 0.0) minZ else maxZ) >= -pxW &&
                nyX * (if (nyX < 0.0) minX else maxX) + nyY * (if (nyY < 0.0) minY else maxY) + nyZ * (if (nyZ < 0.0) minZ else maxZ) >= -nyW &&
                pyX * (if (pyX < 0.0) minX else maxX) + pyY * (if (pyY < 0.0) minY else maxY) + pyZ * (if (pyZ < 0.0) minZ else maxZ) >= -pyW &&
                nzX * (if (nzX < 0.0) minX else maxX) + nzY * (if (nzY < 0.0) minY else maxY) + nzZ * (if (nzZ < 0.0) minZ else maxZ) >= -nzW &&
                pzX * (if (pzX < 0.0) minX else maxX) + pzY * (if (pzY < 0.0) minY else maxY) + pzZ * (if (pzZ < 0.0) minZ else maxZ) >= -pzW
    }

    // from https://github.com/JOML-CI/JOML/blob/main/src/main/java/org/joml/FrustumIntersection.java
    fun testSphere(x: Double, y: Double, z: Double, r: Double): Boolean {
        var inside = true
        var dist: Double
        dist = nxX * x + nxY * y + nxZ * z + nxW
        if (dist >= -r) {
            inside = inside and (dist >= r)
            dist = pxX * x + pxY * y + pxZ * z + pxW
            if (dist >= -r) {
                inside = inside and (dist >= r)
                dist = nyX * x + nyY * y + nyZ * z + nyW
                if (dist >= -r) {
                    inside = inside and (dist >= r)
                    dist = pyX * x + pyY * y + pyZ * z + pyW
                    if (dist >= -r) {
                        inside = inside and (dist >= r)
                        dist = nzX * x + nzY * y + nzZ * z + nzW
                        if (dist >= -r) {
                            inside = inside and (dist >= r)
                            dist = pzX * x + pzY * y + pzZ * z + pzW
                            if (dist >= -r) {
                                inside = inside and (dist >= r)
                                return inside
                            }
                        }
                    }
                }
            }
        }
        return false
    }
}
