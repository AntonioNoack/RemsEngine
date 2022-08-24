package org.joml;

public class AxisAngle4d {
    
    public double angle;
    public double x;
    public double y;
    public double z;

    public AxisAngle4d() {
        this.z = 1.0;
    }

    public AxisAngle4d(AxisAngle4d a) {
        this.x = a.x;
        this.y = a.y;
        this.z = a.z;
        this.angle = (a.angle < 0.0 ? 6.283185307179586 + a.angle % 6.283185307179586 : a.angle) % 6.283185307179586;
    }

    public AxisAngle4d(AxisAngle4f a) {
        this.x = a.x;
        this.y = a.y;
        this.z = a.z;
        this.angle = ((double) a.angle < 0.0 ? 6.283185307179586 + (double) a.angle % 6.283185307179586 : (double) a.angle) % 6.283185307179586;
    }

    public AxisAngle4d(Quaternionf q) {
        float acos = Math.safeAcos(q.w);
        float invSqrt = Math.invsqrt(1f - q.w * q.w);
        if (Float.isInfinite(invSqrt)) {
            this.x = 0.0;
            this.y = 0.0;
            this.z = 1.0;
        } else {
            this.x = q.x * invSqrt;
            this.y = q.y * invSqrt;
            this.z = q.z * invSqrt;
        }

        this.angle = acos + acos;
    }

    public AxisAngle4d(Quaterniond q) {
        double acos = Math.safeAcos(q.w);
        double invSqrt = Math.invsqrt(1.0 - q.w * q.w);
        if (Double.isInfinite(invSqrt)) {
            this.x = 0.0;
            this.y = 0.0;
            this.z = 1.0;
        } else {
            this.x = q.x * invSqrt;
            this.y = q.y * invSqrt;
            this.z = q.z * invSqrt;
        }

        this.angle = acos + acos;
    }

    public AxisAngle4d(double angle, double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.angle = (angle < 0.0 ? 6.283185307179586 + angle % 6.283185307179586 : angle) % 6.283185307179586;
    }

    public AxisAngle4d(double angle, Vector3d v) {
        this(angle, v.x, v.y, v.z);
    }

    public AxisAngle4d(double angle, Vector3f v) {
        this(angle, v.x, v.y, v.z);
    }

    public AxisAngle4d set(AxisAngle4d a) {
        this.x = a.x;
        this.y = a.y;
        this.z = a.z;
        this.angle = (a.angle < 0.0 ? 6.283185307179586 + a.angle % 6.283185307179586 : a.angle) % 6.283185307179586;
        return this;
    }

    public AxisAngle4d set(AxisAngle4f a) {
        this.x = a.x;
        this.y = a.y;
        this.z = a.z;
        this.angle = ((double) a.angle < 0.0 ? 6.283185307179586 + (double) a.angle % 6.283185307179586 : (double) a.angle) % 6.283185307179586;
        return this;
    }

    public AxisAngle4d set(double angle, double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.angle = (angle < 0.0 ? 6.283185307179586 + angle % 6.283185307179586 : angle) % 6.283185307179586;
        return this;
    }

    public AxisAngle4d set(double angle, Vector3d v) {
        return this.set(angle, v.x, v.y, v.z);
    }

    public AxisAngle4d set(double angle, Vector3f v) {
        return this.set(angle, v.x, v.y, v.z);
    }

    public AxisAngle4d set(Quaternionf q) {
        float acos = Math.safeAcos(q.w);
        float invSqrt = Math.invsqrt(1f - q.w * q.w);
        if (Float.isInfinite(invSqrt)) {
            this.x = 0.0;
            this.y = 0.0;
            this.z = 1.0;
        } else {
            this.x = q.x * invSqrt;
            this.y = q.y * invSqrt;
            this.z = q.z * invSqrt;
        }

        this.angle = acos + acos;
        return this;
    }

    public AxisAngle4d set(Quaterniond q) {
        double acos = Math.safeAcos(q.w);
        double invSqrt = Math.invsqrt(1.0 - q.w * q.w);
        if (Double.isInfinite(invSqrt)) {
            this.x = 0.0;
            this.y = 0.0;
            this.z = 1.0;
        } else {
            this.x = q.x * invSqrt;
            this.y = q.y * invSqrt;
            this.z = q.z * invSqrt;
        }

        this.angle = acos + acos;
        return this;
    }

    public AxisAngle4d set(Matrix3f m) {
        double nm00 = m.m00();
        double nm01 = m.m01();
        double nm02 = m.m02();
        double nm10 = m.m10();
        double nm11 = m.m11();
        double nm12 = m.m12();
        double nm20 = m.m20();
        double nm21 = m.m21();
        double nm22 = m.m22();
        double lenX = Math.invsqrt(m.m00() * m.m00() + m.m01() * m.m01() + m.m02() * m.m02());
        double lenY = Math.invsqrt(m.m10() * m.m10() + m.m11() * m.m11() + m.m12() * m.m12());
        double lenZ = Math.invsqrt(m.m20() * m.m20() + m.m21() * m.m21() + m.m22() * m.m22());
        nm00 *= lenX;
        nm01 *= lenX;
        nm02 *= lenX;
        nm10 *= lenY;
        nm11 *= lenY;
        nm12 *= lenY;
        nm20 *= lenZ;
        nm21 *= lenZ;
        nm22 *= lenZ;
        double epsilon = 1.0E-4;
        double epsilon2 = 0.001;
        double xx;
        if (Math.abs(nm10 - nm01) < epsilon && Math.abs(nm20 - nm02) < epsilon && Math.abs(nm21 - nm12) < epsilon) {
            if (Math.abs(nm10 + nm01) < epsilon2 && Math.abs(nm20 + nm02) < epsilon2 && Math.abs(nm21 + nm12) < epsilon2 && Math.abs(nm00 + nm11 + nm22 - 3.0) < epsilon2) {
                this.x = 0.0;
                this.y = 0.0;
                this.z = 1.0;
                this.angle = 0.0;
            } else {
                this.angle = java.lang.Math.PI;
                xx = (nm00 + 1.0) / 2.0;
                double yy = (nm11 + 1.0) / 2.0;
                double zz = (nm22 + 1.0) / 2.0;
                double xy = (nm10 + nm01) / 4.0;
                double xz = (nm20 + nm02) / 4.0;
                double yz = (nm21 + nm12) / 4.0;
                if (xx > yy && xx > zz) {
                    this.x = Math.sqrt(xx);
                    this.y = xy / this.x;
                    this.z = xz / this.x;
                } else if (yy > zz) {
                    this.y = Math.sqrt(yy);
                    this.x = xy / this.y;
                    this.z = yz / this.y;
                } else {
                    this.z = Math.sqrt(zz);
                    this.x = xz / this.z;
                    this.y = yz / this.z;
                }

            }
        } else {
            xx = Math.sqrt((nm12 - nm21) * (nm12 - nm21) + (nm20 - nm02) * (nm20 - nm02) + (nm01 - nm10) * (nm01 - nm10));
            this.angle = Math.safeAcos((nm00 + nm11 + nm22 - 1.0) / 2.0);
            this.x = (nm12 - nm21) / xx;
            this.y = (nm20 - nm02) / xx;
            this.z = (nm01 - nm10) / xx;
        }
        return this;
    }

    public AxisAngle4d set(Matrix3d m) {
        double nm00 = m.m00();
        double nm01 = m.m01();
        double nm02 = m.m02();
        double nm10 = m.m10();
        double nm11 = m.m11();
        double nm12 = m.m12();
        double nm20 = m.m20();
        double nm21 = m.m21();
        double nm22 = m.m22();
        double lenX = Math.invsqrt(m.m00() * m.m00() + m.m01() * m.m01() + m.m02() * m.m02());
        double lenY = Math.invsqrt(m.m10() * m.m10() + m.m11() * m.m11() + m.m12() * m.m12());
        double lenZ = Math.invsqrt(m.m20() * m.m20() + m.m21() * m.m21() + m.m22() * m.m22());
        nm00 *= lenX;
        nm01 *= lenX;
        nm02 *= lenX;
        nm10 *= lenY;
        nm11 *= lenY;
        nm12 *= lenY;
        nm20 *= lenZ;
        nm21 *= lenZ;
        nm22 *= lenZ;
        double epsilon = 1.0E-4;
        double epsilon2 = 0.001;
        double xx;
        if (Math.abs(nm10 - nm01) < epsilon && Math.abs(nm20 - nm02) < epsilon && Math.abs(nm21 - nm12) < epsilon) {
            if (Math.abs(nm10 + nm01) < epsilon2 && Math.abs(nm20 + nm02) < epsilon2 && Math.abs(nm21 + nm12) < epsilon2 && Math.abs(nm00 + nm11 + nm22 - 3.0) < epsilon2) {
                this.x = 0.0;
                this.y = 0.0;
                this.z = 1.0;
                this.angle = 0.0;
            } else {
                this.angle = java.lang.Math.PI;
                xx = (nm00 + 1.0) / 2.0;
                double yy = (nm11 + 1.0) / 2.0;
                double zz = (nm22 + 1.0) / 2.0;
                double xy = (nm10 + nm01) / 4.0;
                double xz = (nm20 + nm02) / 4.0;
                double yz = (nm21 + nm12) / 4.0;
                if (xx > yy && xx > zz) {
                    this.x = Math.sqrt(xx);
                    this.y = xy / this.x;
                    this.z = xz / this.x;
                } else if (yy > zz) {
                    this.y = Math.sqrt(yy);
                    this.x = xy / this.y;
                    this.z = yz / this.y;
                } else {
                    this.z = Math.sqrt(zz);
                    this.x = xz / this.z;
                    this.y = yz / this.z;
                }

            }
        } else {
            xx = Math.sqrt((nm12 - nm21) * (nm12 - nm21) + (nm20 - nm02) * (nm20 - nm02) + (nm01 - nm10) * (nm01 - nm10));
            this.angle = Math.safeAcos((nm00 + nm11 + nm22 - 1.0) / 2.0);
            this.x = (nm12 - nm21) / xx;
            this.y = (nm20 - nm02) / xx;
            this.z = (nm01 - nm10) / xx;
        }
        return this;
    }

    public AxisAngle4d set(Matrix4f m) {
        double nm00 = m.m00();
        double nm01 = m.m01();
        double nm02 = m.m02();
        double nm10 = m.m10();
        double nm11 = m.m11();
        double nm12 = m.m12();
        double nm20 = m.m20();
        double nm21 = m.m21();
        double nm22 = m.m22();
        double lenX = Math.invsqrt(m.m00() * m.m00() + m.m01() * m.m01() + m.m02() * m.m02());
        double lenY = Math.invsqrt(m.m10() * m.m10() + m.m11() * m.m11() + m.m12() * m.m12());
        double lenZ = Math.invsqrt(m.m20() * m.m20() + m.m21() * m.m21() + m.m22() * m.m22());
        nm00 *= lenX;
        nm01 *= lenX;
        nm02 *= lenX;
        nm10 *= lenY;
        nm11 *= lenY;
        nm12 *= lenY;
        nm20 *= lenZ;
        nm21 *= lenZ;
        nm22 *= lenZ;
        double epsilon = 1.0E-4;
        double epsilon2 = 0.001;
        double xx;
        if (Math.abs(nm10 - nm01) < epsilon && Math.abs(nm20 - nm02) < epsilon && Math.abs(nm21 - nm12) < epsilon) {
            if (Math.abs(nm10 + nm01) < epsilon2 && Math.abs(nm20 + nm02) < epsilon2 && Math.abs(nm21 + nm12) < epsilon2 && Math.abs(nm00 + nm11 + nm22 - 3.0) < epsilon2) {
                this.x = 0.0;
                this.y = 0.0;
                this.z = 1.0;
                this.angle = 0.0;
            } else {
                this.angle = java.lang.Math.PI;
                xx = (nm00 + 1.0) / 2.0;
                double yy = (nm11 + 1.0) / 2.0;
                double zz = (nm22 + 1.0) / 2.0;
                double xy = (nm10 + nm01) / 4.0;
                double xz = (nm20 + nm02) / 4.0;
                double yz = (nm21 + nm12) / 4.0;
                if (xx > yy && xx > zz) {
                    this.x = Math.sqrt(xx);
                    this.y = xy / this.x;
                    this.z = xz / this.x;
                } else if (yy > zz) {
                    this.y = Math.sqrt(yy);
                    this.x = xy / this.y;
                    this.z = yz / this.y;
                } else {
                    this.z = Math.sqrt(zz);
                    this.x = xz / this.z;
                    this.y = yz / this.z;
                }

            }
        } else {
            xx = Math.sqrt((nm12 - nm21) * (nm12 - nm21) + (nm20 - nm02) * (nm20 - nm02) + (nm01 - nm10) * (nm01 - nm10));
            this.angle = Math.safeAcos((nm00 + nm11 + nm22 - 1.0) / 2.0);
            this.x = (nm12 - nm21) / xx;
            this.y = (nm20 - nm02) / xx;
            this.z = (nm01 - nm10) / xx;
        }
        return this;
    }

    public AxisAngle4d set(Matrix4x3f m) {
        double nm00 = m.m00();
        double nm01 = m.m01();
        double nm02 = m.m02();
        double nm10 = m.m10();
        double nm11 = m.m11();
        double nm12 = m.m12();
        double nm20 = m.m20();
        double nm21 = m.m21();
        double nm22 = m.m22();
        double lenX = Math.invsqrt(m.m00() * m.m00() + m.m01() * m.m01() + m.m02() * m.m02());
        double lenY = Math.invsqrt(m.m10() * m.m10() + m.m11() * m.m11() + m.m12() * m.m12());
        double lenZ = Math.invsqrt(m.m20() * m.m20() + m.m21() * m.m21() + m.m22() * m.m22());
        nm00 *= lenX;
        nm01 *= lenX;
        nm02 *= lenX;
        nm10 *= lenY;
        nm11 *= lenY;
        nm12 *= lenY;
        nm20 *= lenZ;
        nm21 *= lenZ;
        nm22 *= lenZ;
        double epsilon = 1.0E-4;
        double epsilon2 = 0.001;
        double xx;
        if (Math.abs(nm10 - nm01) < epsilon && Math.abs(nm20 - nm02) < epsilon && Math.abs(nm21 - nm12) < epsilon) {
            if (Math.abs(nm10 + nm01) < epsilon2 && Math.abs(nm20 + nm02) < epsilon2 && Math.abs(nm21 + nm12) < epsilon2 && Math.abs(nm00 + nm11 + nm22 - 3.0) < epsilon2) {
                this.x = 0.0;
                this.y = 0.0;
                this.z = 1.0;
                this.angle = 0.0;
            } else {
                this.angle = java.lang.Math.PI;
                xx = (nm00 + 1.0) / 2.0;
                double yy = (nm11 + 1.0) / 2.0;
                double zz = (nm22 + 1.0) / 2.0;
                double xy = (nm10 + nm01) / 4.0;
                double xz = (nm20 + nm02) / 4.0;
                double yz = (nm21 + nm12) / 4.0;
                if (xx > yy && xx > zz) {
                    this.x = Math.sqrt(xx);
                    this.y = xy / this.x;
                    this.z = xz / this.x;
                } else if (yy > zz) {
                    this.y = Math.sqrt(yy);
                    this.x = xy / this.y;
                    this.z = yz / this.y;
                } else {
                    this.z = Math.sqrt(zz);
                    this.x = xz / this.z;
                    this.y = yz / this.z;
                }

            }
        } else {
            xx = Math.sqrt((nm12 - nm21) * (nm12 - nm21) + (nm20 - nm02) * (nm20 - nm02) + (nm01 - nm10) * (nm01 - nm10));
            this.angle = Math.safeAcos((nm00 + nm11 + nm22 - 1.0) / 2.0);
            this.x = (nm12 - nm21) / xx;
            this.y = (nm20 - nm02) / xx;
            this.z = (nm01 - nm10) / xx;
        }
        return this;
    }

    public AxisAngle4d set(Matrix4d m) {
        double nm00 = m.m00();
        double nm01 = m.m01();
        double nm02 = m.m02();
        double nm10 = m.m10();
        double nm11 = m.m11();
        double nm12 = m.m12();
        double nm20 = m.m20();
        double nm21 = m.m21();
        double nm22 = m.m22();
        double lenX = Math.invsqrt(m.m00() * m.m00() + m.m01() * m.m01() + m.m02() * m.m02());
        double lenY = Math.invsqrt(m.m10() * m.m10() + m.m11() * m.m11() + m.m12() * m.m12());
        double lenZ = Math.invsqrt(m.m20() * m.m20() + m.m21() * m.m21() + m.m22() * m.m22());
        nm00 *= lenX;
        nm01 *= lenX;
        nm02 *= lenX;
        nm10 *= lenY;
        nm11 *= lenY;
        nm12 *= lenY;
        nm20 *= lenZ;
        nm21 *= lenZ;
        nm22 *= lenZ;
        double epsilon = 1.0E-4;
        double epsilon2 = 0.001;
        double xx;
        if (Math.abs(nm10 - nm01) < epsilon && Math.abs(nm20 - nm02) < epsilon && Math.abs(nm21 - nm12) < epsilon) {
            if (Math.abs(nm10 + nm01) < epsilon2 && Math.abs(nm20 + nm02) < epsilon2 && Math.abs(nm21 + nm12) < epsilon2 && Math.abs(nm00 + nm11 + nm22 - 3.0) < epsilon2) {
                this.x = 0.0;
                this.y = 0.0;
                this.z = 1.0;
                this.angle = 0.0;
            } else {
                this.angle = java.lang.Math.PI;
                xx = (nm00 + 1.0) / 2.0;
                double yy = (nm11 + 1.0) / 2.0;
                double zz = (nm22 + 1.0) / 2.0;
                double xy = (nm10 + nm01) / 4.0;
                double xz = (nm20 + nm02) / 4.0;
                double yz = (nm21 + nm12) / 4.0;
                if (xx > yy && xx > zz) {
                    this.x = Math.sqrt(xx);
                    this.y = xy / this.x;
                    this.z = xz / this.x;
                } else if (yy > zz) {
                    this.y = Math.sqrt(yy);
                    this.x = xy / this.y;
                    this.z = yz / this.y;
                } else {
                    this.z = Math.sqrt(zz);
                    this.x = xz / this.z;
                    this.y = yz / this.z;
                }

            }
        } else {
            xx = Math.sqrt((nm12 - nm21) * (nm12 - nm21) + (nm20 - nm02) * (nm20 - nm02) + (nm01 - nm10) * (nm01 - nm10));
            this.angle = Math.safeAcos((nm00 + nm11 + nm22 - 1.0) / 2.0);
            this.x = (nm12 - nm21) / xx;
            this.y = (nm20 - nm02) / xx;
            this.z = (nm01 - nm10) / xx;
        }
        return this;
    }

    public Quaternionf get(Quaternionf q) {
        return q.set(this);
    }

    public Quaterniond get(Quaterniond q) {
        return q.set(this);
    }

    public Matrix4f get(Matrix4f m) {
        return m.set(this);
    }

    public Matrix3f get(Matrix3f m) {
        return m.set(this);
    }

    public Matrix4d get(Matrix4d m) {
        return m.set(this);
    }

    public Matrix3d get(Matrix3d m) {
        return m.set(this);
    }

    public AxisAngle4d get(AxisAngle4d dest) {
        return dest.set(this);
    }

    public AxisAngle4f get(AxisAngle4f dest) {
        return dest.set(this);
    }

    public AxisAngle4d normalize() {
        double invLength = Math.invsqrt(this.x * this.x + this.y * this.y + this.z * this.z);
        this.x *= invLength;
        this.y *= invLength;
        this.z *= invLength;
        return this;
    }

    public AxisAngle4d rotate(double ang) {
        this.angle += ang;
        this.angle = (this.angle < 0.0 ? 6.283185307179586 + this.angle % 6.283185307179586 : this.angle) % 6.283185307179586;
        return this;
    }

    public Vector3d transform(Vector3d v) {
        return this.transform(v, v);
    }

    public Vector3d transform(Vector3d v, Vector3d dest) {
        double sin = Math.sin(this.angle);
        double cos = Math.cosFromSin(sin, this.angle);
        double dot = this.x * v.x + this.y * v.y + this.z * v.z;
        dest.set(v.x * cos + sin * (this.y * v.z - this.z * v.y) + (1.0 - cos) * dot * this.x, v.y * cos + sin * (this.z * v.x - this.x * v.z) + (1.0 - cos) * dot * this.y, v.z * cos + sin * (this.x * v.y - this.y * v.x) + (1.0 - cos) * dot * this.z);
        return dest;
    }

    public Vector3f transform(Vector3f v) {
        return this.transform(v, v);
    }

    public Vector3f transform(Vector3f v, Vector3f dest) {
        double sin = Math.sin(this.angle);
        double cos = Math.cosFromSin(sin, this.angle);
        double dot = this.x * (double) v.x + this.y * (double) v.y + this.z * (double) v.z;
        dest.set((float) ((double) v.x * cos + sin * (this.y * (double) v.z - this.z * (double) v.y) + (1.0 - cos) * dot * this.x),
                (float) ((double) v.y * cos + sin * (this.z * (double) v.x - this.x * (double) v.z) + (1.0 - cos) * dot * this.y),
                (float) ((double) v.z * cos + sin * (this.x * (double) v.y - this.y * (double) v.x) + (1.0 - cos) * dot * this.z));
        return dest;
    }

    public Vector4d transform(Vector4d v) {
        return this.transform(v, v);
    }

    public Vector4d transform(Vector4d v, Vector4d dest) {
        double sin = Math.sin(this.angle);
        double cos = Math.cosFromSin(sin, this.angle);
        double dot = this.x * v.x + this.y * v.y + this.z * v.z;
        dest.set(v.x * cos + sin * (this.y * v.z - this.z * v.y) + (1.0 - cos) * dot * this.x, v.y * cos + sin * (this.z * v.x - this.x * v.z) + (1.0 - cos) * dot * this.y, v.z * cos + sin * (this.x * v.y - this.y * v.x) + (1.0 - cos) * dot * this.z, dest.w);
        return dest;
    }

    public String toString() {
        return "(" + x + "," + y + "," + z + " <| " + angle + ")";
    }

    public int hashCode() {
        int result = 1;
        long temp = Double.doubleToLongBits((this.angle < 0.0 ? 6.283185307179586 + this.angle % 6.283185307179586 : this.angle) % 6.283185307179586);
        result = 31 * result + (int) (temp ^ temp >>> 32);
        temp = Double.doubleToLongBits(this.x);
        result = 31 * result + (int) (temp ^ temp >>> 32);
        temp = Double.doubleToLongBits(this.y);
        result = 31 * result + (int) (temp ^ temp >>> 32);
        temp = Double.doubleToLongBits(this.z);
        result = 31 * result + (int) (temp ^ temp >>> 32);
        return result;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null) {
            return false;
        } else if (this.getClass() != obj.getClass()) {
            return false;
        } else {
            AxisAngle4d other = (AxisAngle4d) obj;
            if (Double.doubleToLongBits((this.angle < 0.0 ? 6.283185307179586 + this.angle % 6.283185307179586 : this.angle) % 6.283185307179586) != Double.doubleToLongBits((other.angle < 0.0 ? 6.283185307179586 + other.angle % 6.283185307179586 : other.angle) % 6.283185307179586)) {
                return false;
            } else if (Double.doubleToLongBits(this.x) != Double.doubleToLongBits(other.x)) {
                return false;
            } else if (Double.doubleToLongBits(this.y) != Double.doubleToLongBits(other.y)) {
                return false;
            } else {
                return Double.doubleToLongBits(this.z) == Double.doubleToLongBits(other.z);
            }
        }
    }

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
