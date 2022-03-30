//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package org.joml;

public class RayAabIntersection2 {
    private double originX;
    private double originY;
    private double originZ;
    private double dirX;
    private double dirY;
    private double dirZ;
    private double c_xy;
    private double c_yx;
    private double c_zy;
    private double c_yz;
    private double c_xz;
    private double c_zx;
    private double s_xy;
    private double s_yx;
    private double s_zy;
    private double s_yz;
    private double s_xz;
    private double s_zx;
    private byte classification;

    public RayAabIntersection2() {
    }

    public RayAabIntersection2(double originX, double originY, double originZ, double dirX, double dirY, double dirZ) {
        this.set(originX, originY, originZ, dirX, dirY, dirZ);
    }

    public void set(double originX, double originY, double originZ, double dirX, double dirY, double dirZ) {
        this.originX = originX;
        this.originY = originY;
        this.originZ = originZ;
        this.dirX = dirX;
        this.dirY = dirY;
        this.dirZ = dirZ;
        this.precomputeSlope();
    }

    private static int sign(double f) {
        return f != 0.0F && !Double.isNaN(f) ? (int) (1 - Double.doubleToLongBits(f) >>> 63 << 1) - 1 : 0;
    }

    private void precomputeSlope() {
        double invDirX = 1.0F / this.dirX;
        double invDirY = 1.0F / this.dirY;
        double invDirZ = 1.0F / this.dirZ;
        this.s_yx = this.dirX * invDirY;
        this.s_xy = this.dirY * invDirX;
        this.s_zy = this.dirY * invDirZ;
        this.s_yz = this.dirZ * invDirY;
        this.s_xz = this.dirZ * invDirX;
        this.s_zx = this.dirX * invDirZ;
        this.c_xy = this.originY - this.s_xy * this.originX;
        this.c_yx = this.originX - this.s_yx * this.originY;
        this.c_zy = this.originY - this.s_zy * this.originZ;
        this.c_yz = this.originZ - this.s_yz * this.originY;
        this.c_xz = this.originZ - this.s_xz * this.originX;
        this.c_zx = this.originX - this.s_zx * this.originZ;
        int sgnX = sign(this.dirX);
        int sgnY = sign(this.dirY);
        int sgnZ = sign(this.dirZ);
        this.classification = (byte) ((sgnZ + 1) << 4 | (sgnY + 1) << 2 | (sgnX + 1));
    }

    public boolean test(AABBd aabb) {
        return test(aabb.minX, aabb.minY, aabb.minZ,
                aabb.maxX, aabb.maxY, aabb.maxZ);
    }

    public boolean test(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        switch (this.classification) {
            case 0:
                return this.MMM(minX, minY, minZ, maxX, maxY, maxZ);
            case 1:
                return this.OMM(minX, minY, minZ, maxX, maxY, maxZ);
            case 2:
                return this.PMM(minX, minY, minZ, maxX, maxY, maxZ);
            case 3:
                return false;
            case 4:
                return this.MOM(minX, minY, minZ, maxX, maxY, maxZ);
            case 5:
                return this.OOM(minX, minY, minZ, maxX, maxY);
            case 6:
                return this.POM(minX, minY, minZ, maxX, maxY, maxZ);
            case 7:
                return false;
            case 8:
                return this.MPM(minX, minY, minZ, maxX, maxY, maxZ);
            case 9:
                return this.OPM(minX, minY, minZ, maxX, maxY, maxZ);
            case 10:
                return this.PPM(minX, minY, minZ, maxX, maxY, maxZ);
            case 11:
            case 12:
            case 13:
            case 14:
            case 15:
                return false;
            case 16:
                return this.MMO(minX, minY, minZ, maxX, maxY, maxZ);
            case 17:
                return this.OMO(minX, minY, minZ, maxX, maxZ);
            case 18:
                return this.PMO(minX, minY, minZ, maxX, maxY, maxZ);
            case 19:
                return false;
            case 20:
                return this.MOO(minX, minY, minZ, maxY, maxZ);
            case 21:
                return false;
            case 22:
                return this.POO(minY, minZ, maxX, maxY, maxZ);
            case 23:
                return false;
            case 24:
                return this.MPO(minX, minY, minZ, maxX, maxY, maxZ);
            case 25:
                return this.OPO(minX, minZ, maxX, maxY, maxZ);
            case 26:
                return this.PPO(minX, minY, minZ, maxX, maxY, maxZ);
            case 27:
            case 28:
            case 29:
            case 30:
            case 31:
                return false;
            case 32:
                return this.MMP(minX, minY, minZ, maxX, maxY, maxZ);
            case 33:
                return this.OMP(minX, minY, minZ, maxX, maxY, maxZ);
            case 34:
                return this.PMP(minX, minY, minZ, maxX, maxY, maxZ);
            case 35:
                return false;
            case 36:
                return this.MOP(minX, minY, minZ, maxX, maxY, maxZ);
            case 37:
                return this.OOP(minX, minY, maxX, maxY, maxZ);
            case 38:
                return this.POP(minX, minY, minZ, maxX, maxY, maxZ);
            case 39:
                return false;
            case 40:
                return this.MPP(minX, minY, minZ, maxX, maxY, maxZ);
            case 41:
                return this.OPP(minX, minY, minZ, maxX, maxY, maxZ);
            case 42:
                return this.PPP(minX, minY, minZ, maxX, maxY, maxZ);
            default:
                return false;
        }
    }

    private boolean MMM(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        return this.originX >= minX && this.originY >= minY && this.originZ >= minZ && this.s_xy * minX - maxY + this.c_xy <= 0.0F && this.s_yx * minY - maxX + this.c_yx <= 0.0F && this.s_zy * minZ - maxY + this.c_zy <= 0.0F && this.s_yz * minY - maxZ + this.c_yz <= 0.0F && this.s_xz * minX - maxZ + this.c_xz <= 0.0F && this.s_zx * minZ - maxX + this.c_zx <= 0.0F;
    }

    private boolean OMM(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        return this.originX >= minX && this.originX <= maxX && this.originY >= minY && this.originZ >= minZ && this.s_zy * minZ - maxY + this.c_zy <= 0.0F && this.s_yz * minY - maxZ + this.c_yz <= 0.0F;
    }

    private boolean PMM(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        return this.originX <= maxX && this.originY >= minY && this.originZ >= minZ && this.s_xy * maxX - maxY + this.c_xy <= 0.0F && this.s_yx * minY - minX + this.c_yx >= 0.0F && this.s_zy * minZ - maxY + this.c_zy <= 0.0F && this.s_yz * minY - maxZ + this.c_yz <= 0.0F && this.s_xz * maxX - maxZ + this.c_xz <= 0.0F && this.s_zx * minZ - minX + this.c_zx >= 0.0F;
    }

    private boolean MOM(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        return this.originY >= minY && this.originY <= maxY && this.originX >= minX && this.originZ >= minZ && this.s_xz * minX - maxZ + this.c_xz <= 0.0F && this.s_zx * minZ - maxX + this.c_zx <= 0.0F;
    }

    private boolean OOM(double minX, double minY, double minZ, double maxX, double maxY) {
        return this.originZ >= minZ && this.originX >= minX && this.originX <= maxX && this.originY >= minY && this.originY <= maxY;
    }

    private boolean POM(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        return this.originY >= minY && this.originY <= maxY && this.originX <= maxX && this.originZ >= minZ && this.s_xz * maxX - maxZ + this.c_xz <= 0.0F && this.s_zx * minZ - minX + this.c_zx >= 0.0F;
    }

    private boolean MPM(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        return this.originX >= minX && this.originY <= maxY && this.originZ >= minZ && this.s_xy * minX - minY + this.c_xy >= 0.0F && this.s_yx * maxY - maxX + this.c_yx <= 0.0F && this.s_zy * minZ - minY + this.c_zy >= 0.0F && this.s_yz * maxY - maxZ + this.c_yz <= 0.0F && this.s_xz * minX - maxZ + this.c_xz <= 0.0F && this.s_zx * minZ - maxX + this.c_zx <= 0.0F;
    }

    private boolean OPM(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        return this.originX >= minX && this.originX <= maxX && this.originY <= maxY && this.originZ >= minZ && this.s_zy * minZ - minY + this.c_zy >= 0.0F && this.s_yz * maxY - maxZ + this.c_yz <= 0.0F;
    }

    private boolean PPM(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        return this.originX <= maxX && this.originY <= maxY && this.originZ >= minZ && this.s_xy * maxX - minY + this.c_xy >= 0.0F && this.s_yx * maxY - minX + this.c_yx >= 0.0F && this.s_zy * minZ - minY + this.c_zy >= 0.0F && this.s_yz * maxY - maxZ + this.c_yz <= 0.0F && this.s_xz * maxX - maxZ + this.c_xz <= 0.0F && this.s_zx * minZ - minX + this.c_zx >= 0.0F;
    }

    private boolean MMO(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        return this.originZ >= minZ && this.originZ <= maxZ && this.originX >= minX && this.originY >= minY && this.s_xy * minX - maxY + this.c_xy <= 0.0F && this.s_yx * minY - maxX + this.c_yx <= 0.0F;
    }

    private boolean OMO(double minX, double minY, double minZ, double maxX, double maxZ) {
        return this.originY >= minY && this.originX >= minX && this.originX <= maxX && this.originZ >= minZ && this.originZ <= maxZ;
    }

    private boolean PMO(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        return this.originZ >= minZ && this.originZ <= maxZ && this.originX <= maxX && this.originY >= minY && this.s_xy * maxX - maxY + this.c_xy <= 0.0F && this.s_yx * minY - minX + this.c_yx >= 0.0F;
    }

    private boolean MOO(double minX, double minY, double minZ, double maxY, double maxZ) {
        return this.originX >= minX && this.originY >= minY && this.originY <= maxY && this.originZ >= minZ && this.originZ <= maxZ;
    }

    private boolean POO(double minY, double minZ, double maxX, double maxY, double maxZ) {
        return this.originX <= maxX && this.originY >= minY && this.originY <= maxY && this.originZ >= minZ && this.originZ <= maxZ;
    }

    private boolean MPO(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        return this.originZ >= minZ && this.originZ <= maxZ && this.originX >= minX && this.originY <= maxY && this.s_xy * minX - minY + this.c_xy >= 0.0F && this.s_yx * maxY - maxX + this.c_yx <= 0.0F;
    }

    private boolean OPO(double minX, double minZ, double maxX, double maxY, double maxZ) {
        return this.originY <= maxY && this.originX >= minX && this.originX <= maxX && this.originZ >= minZ && this.originZ <= maxZ;
    }

    private boolean PPO(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        return this.originZ >= minZ && this.originZ <= maxZ && this.originX <= maxX && this.originY <= maxY && this.s_xy * maxX - minY + this.c_xy >= 0.0F && this.s_yx * maxY - minX + this.c_yx >= 0.0F;
    }

    private boolean MMP(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        return this.originX >= minX && this.originY >= minY && this.originZ <= maxZ && this.s_xy * minX - maxY + this.c_xy <= 0.0F && this.s_yx * minY - maxX + this.c_yx <= 0.0F && this.s_zy * maxZ - maxY + this.c_zy <= 0.0F && this.s_yz * minY - minZ + this.c_yz >= 0.0F && this.s_xz * minX - minZ + this.c_xz >= 0.0F && this.s_zx * maxZ - maxX + this.c_zx <= 0.0F;
    }

    private boolean OMP(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        return this.originX >= minX && this.originX <= maxX && this.originY >= minY && this.originZ <= maxZ && this.s_zy * maxZ - maxY + this.c_zy <= 0.0F && this.s_yz * minY - minZ + this.c_yz >= 0.0F;
    }

    private boolean PMP(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        return this.originX <= maxX && this.originY >= minY && this.originZ <= maxZ && this.s_xy * maxX - maxY + this.c_xy <= 0.0F && this.s_yx * minY - minX + this.c_yx >= 0.0F && this.s_zy * maxZ - maxY + this.c_zy <= 0.0F && this.s_yz * minY - minZ + this.c_yz >= 0.0F && this.s_xz * maxX - minZ + this.c_xz >= 0.0F && this.s_zx * maxZ - minX + this.c_zx >= 0.0F;
    }

    private boolean MOP(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        return this.originY >= minY && this.originY <= maxY && this.originX >= minX && this.originZ <= maxZ && this.s_xz * minX - minZ + this.c_xz >= 0.0F && this.s_zx * maxZ - maxX + this.c_zx <= 0.0F;
    }

    private boolean OOP(double minX, double minY, double maxX, double maxY, double maxZ) {
        return this.originZ <= maxZ && this.originX >= minX && this.originX <= maxX && this.originY >= minY && this.originY <= maxY;
    }

    private boolean POP(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        return this.originY >= minY && this.originY <= maxY && this.originX <= maxX && this.originZ <= maxZ && this.s_xz * maxX - minZ + this.c_xz >= 0.0F && this.s_zx * maxZ - minX + this.c_zx <= 0.0F;
    }

    private boolean MPP(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        return this.originX >= minX && this.originY <= maxY && this.originZ <= maxZ && this.s_xy * minX - minY + this.c_xy >= 0.0F && this.s_yx * maxY - maxX + this.c_yx <= 0.0F && this.s_zy * maxZ - minY + this.c_zy >= 0.0F && this.s_yz * maxY - minZ + this.c_yz >= 0.0F && this.s_xz * minX - minZ + this.c_xz >= 0.0F && this.s_zx * maxZ - maxX + this.c_zx <= 0.0F;
    }

    private boolean OPP(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        return this.originX >= minX && this.originX <= maxX && this.originY <= maxY && this.originZ <= maxZ && this.s_zy * maxZ - minY + this.c_zy <= 0.0F && this.s_yz * maxY - minZ + this.c_yz <= 0.0F;
    }

    private boolean PPP(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        return this.originX <= maxX && this.originY <= maxY && this.originZ <= maxZ && this.s_xy * maxX - minY + this.c_xy >= 0.0F && this.s_yx * maxY - minX + this.c_yx >= 0.0F && this.s_zy * maxZ - minY + this.c_zy >= 0.0F && this.s_yz * maxY - minZ + this.c_yz >= 0.0F && this.s_xz * maxX - minZ + this.c_xz >= 0.0F && this.s_zx * maxZ - minX + this.c_zx >= 0.0F;
    }
}
