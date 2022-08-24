package org.joml

/**
 * Ray Intersections using Doubles
 */
class RayAabIntersection2 {
    private var originX = 0.0
    private var originY = 0.0
    private var originZ = 0.0
    private var dirX = 0.0
    private var dirY = 0.0
    private var dirZ = 0.0
    private var c_xy = 0.0
    private var c_yx = 0.0
    private var c_zy = 0.0
    private var c_yz = 0.0
    private var c_xz = 0.0
    private var c_zx = 0.0
    private var s_xy = 0.0
    private var s_yx = 0.0
    private var s_zy = 0.0
    private var s_yz = 0.0
    private var s_xz = 0.0
    private var s_zx = 0.0
    private var classification: Byte = 0

    constructor() {}
    constructor(originX: Double, originY: Double, originZ: Double, dirX: Double, dirY: Double, dirZ: Double) {
        this[originX, originY, originZ, dirX, dirY] = dirZ
    }

    operator fun set(originX: Double, originY: Double, originZ: Double, dirX: Double, dirY: Double, dirZ: Double) {
        this.originX = originX
        this.originY = originY
        this.originZ = originZ
        this.dirX = dirX
        this.dirY = dirY
        this.dirZ = dirZ
        precomputeSlope()
    }

    private fun precomputeSlope() {
        val invDirX = 1.0f / dirX
        val invDirY = 1.0f / dirY
        val invDirZ = 1.0f / dirZ
        s_yx = dirX * invDirY
        s_xy = dirY * invDirX
        s_zy = dirY * invDirZ
        s_yz = dirZ * invDirY
        s_xz = dirZ * invDirX
        s_zx = dirX * invDirZ
        c_xy = originY - s_xy * originX
        c_yx = originX - s_yx * originY
        c_zy = originY - s_zy * originZ
        c_yz = originZ - s_yz * originY
        c_xz = originZ - s_xz * originX
        c_zx = originX - s_zx * originZ
        val sgnX = sign(dirX)
        val sgnY = sign(dirY)
        val sgnZ = sign(dirZ)
        classification = (sgnZ + 1 shl 4 or (sgnY + 1 shl 2) or sgnX + 1).toByte()
    }

    fun test(aabb: AABBd): Boolean {
        return test(
            aabb.minX, aabb.minY, aabb.minZ,
            aabb.maxX, aabb.maxY, aabb.maxZ
        )
    }

    fun test(minX: Double, minY: Double, minZ: Double, maxX: Double, maxY: Double, maxZ: Double): Boolean {
        return when (classification.toInt()) {
            0 -> MMM(minX, minY, minZ, maxX, maxY, maxZ)
            1 -> OMM(minX, minY, minZ, maxX, maxY, maxZ)
            2 -> PMM(minX, minY, minZ, maxX, maxY, maxZ)
            3 -> false
            4 -> MOM(minX, minY, minZ, maxX, maxY, maxZ)
            5 -> OOM(minX, minY, minZ, maxX, maxY)
            6 -> POM(minX, minY, minZ, maxX, maxY, maxZ)
            7 -> false
            8 -> MPM(minX, minY, minZ, maxX, maxY, maxZ)
            9 -> OPM(minX, minY, minZ, maxX, maxY, maxZ)
            10 -> PPM(minX, minY, minZ, maxX, maxY, maxZ)
            11, 12, 13, 14, 15 -> false
            16 -> MMO(minX, minY, minZ, maxX, maxY, maxZ)
            17 -> OMO(minX, minY, minZ, maxX, maxZ)
            18 -> PMO(minX, minY, minZ, maxX, maxY, maxZ)
            19 -> false
            20 -> MOO(minX, minY, minZ, maxY, maxZ)
            21 -> false
            22 -> POO(minY, minZ, maxX, maxY, maxZ)
            23 -> false
            24 -> MPO(minX, minY, minZ, maxX, maxY, maxZ)
            25 -> OPO(minX, minZ, maxX, maxY, maxZ)
            26 -> PPO(minX, minY, minZ, maxX, maxY, maxZ)
            27, 28, 29, 30, 31 -> false
            32 -> MMP(minX, minY, minZ, maxX, maxY, maxZ)
            33 -> OMP(minX, minY, minZ, maxX, maxY, maxZ)
            34 -> PMP(minX, minY, minZ, maxX, maxY, maxZ)
            35 -> false
            36 -> MOP(minX, minY, minZ, maxX, maxY, maxZ)
            37 -> OOP(minX, minY, maxX, maxY, maxZ)
            38 -> POP(minX, minY, minZ, maxX, maxY, maxZ)
            39 -> false
            40 -> MPP(minX, minY, minZ, maxX, maxY, maxZ)
            41 -> OPP(minX, minY, minZ, maxX, maxY, maxZ)
            42 -> PPP(minX, minY, minZ, maxX, maxY, maxZ)
            else -> false
        }
    }

    private fun MMM(minX: Double, minY: Double, minZ: Double, maxX: Double, maxY: Double, maxZ: Double): Boolean {
        return originX >= minX && originY >= minY && originZ >= minZ && s_xy * minX - maxY + c_xy <= 0.0f && s_yx * minY - maxX + c_yx <= 0.0f && s_zy * minZ - maxY + c_zy <= 0.0f && s_yz * minY - maxZ + c_yz <= 0.0f && s_xz * minX - maxZ + c_xz <= 0.0f && s_zx * minZ - maxX + c_zx <= 0.0f
    }

    private fun OMM(minX: Double, minY: Double, minZ: Double, maxX: Double, maxY: Double, maxZ: Double): Boolean {
        return originX in minX..maxX && originY >= minY && originZ >= minZ && s_zy * minZ - maxY + c_zy <= 0.0f && s_yz * minY - maxZ + c_yz <= 0.0f
    }

    private fun PMM(minX: Double, minY: Double, minZ: Double, maxX: Double, maxY: Double, maxZ: Double): Boolean {
        return originX <= maxX && originY >= minY && originZ >= minZ && s_xy * maxX - maxY + c_xy <= 0.0f && s_yx * minY - minX + c_yx >= 0.0f && s_zy * minZ - maxY + c_zy <= 0.0f && s_yz * minY - maxZ + c_yz <= 0.0f && s_xz * maxX - maxZ + c_xz <= 0.0f && s_zx * minZ - minX + c_zx >= 0.0f
    }

    private fun MOM(minX: Double, minY: Double, minZ: Double, maxX: Double, maxY: Double, maxZ: Double): Boolean {
        return originY in minY..maxY && originX >= minX && originZ >= minZ && s_xz * minX - maxZ + c_xz <= 0.0f && s_zx * minZ - maxX + c_zx <= 0.0f
    }

    private fun OOM(minX: Double, minY: Double, minZ: Double, maxX: Double, maxY: Double): Boolean {
        return originZ >= minZ && originX >= minX && originX <= maxX && originY >= minY && originY <= maxY
    }

    private fun POM(minX: Double, minY: Double, minZ: Double, maxX: Double, maxY: Double, maxZ: Double): Boolean {
        return originY in minY..maxY && originX <= maxX && originZ >= minZ && s_xz * maxX - maxZ + c_xz <= 0.0f && s_zx * minZ - minX + c_zx >= 0.0f
    }

    private fun MPM(minX: Double, minY: Double, minZ: Double, maxX: Double, maxY: Double, maxZ: Double): Boolean {
        return originX >= minX && originY <= maxY && originZ >= minZ && s_xy * minX - minY + c_xy >= 0.0f && s_yx * maxY - maxX + c_yx <= 0.0f && s_zy * minZ - minY + c_zy >= 0.0f && s_yz * maxY - maxZ + c_yz <= 0.0f && s_xz * minX - maxZ + c_xz <= 0.0f && s_zx * minZ - maxX + c_zx <= 0.0f
    }

    private fun OPM(minX: Double, minY: Double, minZ: Double, maxX: Double, maxY: Double, maxZ: Double): Boolean {
        return originX in minX..maxX && originY <= maxY && originZ >= minZ && s_zy * minZ - minY + c_zy >= 0.0f && s_yz * maxY - maxZ + c_yz <= 0.0f
    }

    private fun PPM(minX: Double, minY: Double, minZ: Double, maxX: Double, maxY: Double, maxZ: Double): Boolean {
        return originX <= maxX && originY <= maxY && originZ >= minZ && s_xy * maxX - minY + c_xy >= 0.0f && s_yx * maxY - minX + c_yx >= 0.0f && s_zy * minZ - minY + c_zy >= 0.0f && s_yz * maxY - maxZ + c_yz <= 0.0f && s_xz * maxX - maxZ + c_xz <= 0.0f && s_zx * minZ - minX + c_zx >= 0.0f
    }

    private fun MMO(minX: Double, minY: Double, minZ: Double, maxX: Double, maxY: Double, maxZ: Double): Boolean {
        return originZ in minZ..maxZ && originX >= minX && originY >= minY && s_xy * minX - maxY + c_xy <= 0.0f && s_yx * minY - maxX + c_yx <= 0.0f
    }

    private fun OMO(minX: Double, minY: Double, minZ: Double, maxX: Double, maxZ: Double): Boolean {
        return originY >= minY && originX >= minX && originX <= maxX && originZ >= minZ && originZ <= maxZ
    }

    private fun PMO(minX: Double, minY: Double, minZ: Double, maxX: Double, maxY: Double, maxZ: Double): Boolean {
        return originZ in minZ..maxZ && originX <= maxX && originY >= minY && s_xy * maxX - maxY + c_xy <= 0.0f && s_yx * minY - minX + c_yx >= 0.0f
    }

    private fun MOO(minX: Double, minY: Double, minZ: Double, maxY: Double, maxZ: Double): Boolean {
        return originX >= minX && originY >= minY && originY <= maxY && originZ >= minZ && originZ <= maxZ
    }

    private fun POO(minY: Double, minZ: Double, maxX: Double, maxY: Double, maxZ: Double): Boolean {
        return originX <= maxX && originY >= minY && originY <= maxY && originZ >= minZ && originZ <= maxZ
    }

    private fun MPO(minX: Double, minY: Double, minZ: Double, maxX: Double, maxY: Double, maxZ: Double): Boolean {
        return originZ in minZ..maxZ && originX >= minX && originY <= maxY && s_xy * minX - minY + c_xy >= 0.0f && s_yx * maxY - maxX + c_yx <= 0.0f
    }

    private fun OPO(minX: Double, minZ: Double, maxX: Double, maxY: Double, maxZ: Double): Boolean {
        return originY <= maxY && originX >= minX && originX <= maxX && originZ >= minZ && originZ <= maxZ
    }

    private fun PPO(minX: Double, minY: Double, minZ: Double, maxX: Double, maxY: Double, maxZ: Double): Boolean {
        return originZ in minZ..maxZ && originX <= maxX && originY <= maxY && s_xy * maxX - minY + c_xy >= 0.0f && s_yx * maxY - minX + c_yx >= 0.0f
    }

    private fun MMP(minX: Double, minY: Double, minZ: Double, maxX: Double, maxY: Double, maxZ: Double): Boolean {
        return originX >= minX && originY >= minY && originZ <= maxZ && s_xy * minX - maxY + c_xy <= 0.0f && s_yx * minY - maxX + c_yx <= 0.0f && s_zy * maxZ - maxY + c_zy <= 0.0f && s_yz * minY - minZ + c_yz >= 0.0f && s_xz * minX - minZ + c_xz >= 0.0f && s_zx * maxZ - maxX + c_zx <= 0.0f
    }

    private fun OMP(minX: Double, minY: Double, minZ: Double, maxX: Double, maxY: Double, maxZ: Double): Boolean {
        return originX in minX..maxX && originY >= minY && originZ <= maxZ && s_zy * maxZ - maxY + c_zy <= 0.0f && s_yz * minY - minZ + c_yz >= 0.0f
    }

    private fun PMP(minX: Double, minY: Double, minZ: Double, maxX: Double, maxY: Double, maxZ: Double): Boolean {
        return originX <= maxX && originY >= minY && originZ <= maxZ && s_xy * maxX - maxY + c_xy <= 0.0f && s_yx * minY - minX + c_yx >= 0.0f && s_zy * maxZ - maxY + c_zy <= 0.0f && s_yz * minY - minZ + c_yz >= 0.0f && s_xz * maxX - minZ + c_xz >= 0.0f && s_zx * maxZ - minX + c_zx >= 0.0f
    }

    private fun MOP(minX: Double, minY: Double, minZ: Double, maxX: Double, maxY: Double, maxZ: Double): Boolean {
        return originY in minY..maxY && originX >= minX && originZ <= maxZ && s_xz * minX - minZ + c_xz >= 0.0f && s_zx * maxZ - maxX + c_zx <= 0.0f
    }

    private fun OOP(minX: Double, minY: Double, maxX: Double, maxY: Double, maxZ: Double): Boolean {
        return originZ <= maxZ && originX >= minX && originX <= maxX && originY >= minY && originY <= maxY
    }

    private fun POP(minX: Double, minY: Double, minZ: Double, maxX: Double, maxY: Double, maxZ: Double): Boolean {
        return originY in minY..maxY && originX <= maxX && originZ <= maxZ && s_xz * maxX - minZ + c_xz >= 0.0f && s_zx * maxZ - minX + c_zx <= 0.0f
    }

    private fun MPP(minX: Double, minY: Double, minZ: Double, maxX: Double, maxY: Double, maxZ: Double): Boolean {
        return originX >= minX && originY <= maxY && originZ <= maxZ && s_xy * minX - minY + c_xy >= 0.0f && s_yx * maxY - maxX + c_yx <= 0.0f && s_zy * maxZ - minY + c_zy >= 0.0f && s_yz * maxY - minZ + c_yz >= 0.0f && s_xz * minX - minZ + c_xz >= 0.0f && s_zx * maxZ - maxX + c_zx <= 0.0f
    }

    private fun OPP(minX: Double, minY: Double, minZ: Double, maxX: Double, maxY: Double, maxZ: Double): Boolean {
        return originX in minX..maxX && originY <= maxY && originZ <= maxZ && s_zy * maxZ - minY + c_zy <= 0.0f && s_yz * maxY - minZ + c_yz <= 0.0f
    }

    private fun PPP(minX: Double, minY: Double, minZ: Double, maxX: Double, maxY: Double, maxZ: Double): Boolean {
        return originX <= maxX && originY <= maxY && originZ <= maxZ && s_xy * maxX - minY + c_xy >= 0.0f && s_yx * maxY - minX + c_yx >= 0.0f && s_zy * maxZ - minY + c_zy >= 0.0f && s_yz * maxY - minZ + c_yz >= 0.0f && s_xz * maxX - minZ + c_xz >= 0.0f && s_zx * maxZ - minX + c_zx >= 0.0f
    }

    companion object {
        private fun sign(f: Double): Int {
            return if (f != 0.0 && !f.isNaN()) (1 - f.toBits() ushr 63 shl 1).toInt() - 1 else 0
        }
    }
}