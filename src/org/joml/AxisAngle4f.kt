package org.joml;

public class AxisAngle4f {

    public float angle;
    public float x;
    public float y;
    public float z;

    public AxisAngle4f() {
        this.z = 1.0F;
    }

    public AxisAngle4f(AxisAngle4f a) {
        this.x = a.x;
        this.y = a.y;
        this.z = a.z;
        this.angle = (float) (((double) a.angle < 0.0 ? 6.283185307179586 + (double) a.angle % 6.283185307179586 : (double) a.angle) % 6.283185307179586);
    }

    public AxisAngle4f(Quaternionf q) {
        float acos = Math.safeAcos(q.w);
        float invSqrt = Math.invsqrt(1.0F - q.w * q.w);
        if (Float.isInfinite(invSqrt)) {
            this.x = 0.0F;
            this.y = 0.0F;
            this.z = 1.0F;
        } else {
            this.x = q.x * invSqrt;
            this.y = q.y * invSqrt;
            this.z = q.z * invSqrt;
        }

        this.angle = acos + acos;
    }

    public AxisAngle4f(float angle, float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.angle = (float) (((double) angle < 0.0 ? 6.283185307179586 + (double) angle % 6.283185307179586 : (double) angle) % 6.283185307179586);
    }

    public AxisAngle4f(float angle, Vector3f v) {
        this(angle, v.x, v.y, v.z);
    }

    public AxisAngle4f set(AxisAngle4f a) {
        this.x = a.x;
        this.y = a.y;
        this.z = a.z;
        this.angle = a.angle;
        this.angle = (float) (((double) this.angle < 0.0 ? 6.283185307179586 + (double) this.angle % 6.283185307179586 : (double) this.angle) % 6.283185307179586);
        return this;
    }

    public AxisAngle4f set(AxisAngle4d a) {
        this.x = (float) a.x;
        this.y = (float) a.y;
        this.z = (float) a.z;
        this.angle = (float) a.angle;
        this.angle = (float) (((double) this.angle < 0.0 ? 6.283185307179586 + (double) this.angle % 6.283185307179586 : (double) this.angle) % 6.283185307179586);
        return this;
    }

    public AxisAngle4f set(float angle, float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.angle = (float) (((double) angle < 0.0 ? 6.283185307179586 + (double) angle % 6.283185307179586 : (double) angle) % 6.283185307179586);
        return this;
    }

    public AxisAngle4f set(float angle, Vector3f v) {
        return this.set(angle, v.x, v.y, v.z);
    }

    public AxisAngle4f set(Quaternionf q) {
        float acos = Math.safeAcos(q.w);
        float invSqrt = Math.invsqrt(1.0F - q.w * q.w);
        if (Float.isInfinite(invSqrt)) {
            this.x = 0.0F;
            this.y = 0.0F;
            this.z = 1.0F;
        } else {
            this.x = q.x * invSqrt;
            this.y = q.y * invSqrt;
            this.z = q.z * invSqrt;
        }

        this.angle = acos + acos;
        return this;
    }

    public AxisAngle4f set(Quaterniond q) {
        double acos = Math.safeAcos(q.w);
        double invSqrt = Math.invsqrt(1.0 - q.w * q.w);
        if (Double.isInfinite(invSqrt)) {
            this.x = 0.0F;
            this.y = 0.0F;
            this.z = 1.0F;
        } else {
            this.x = (float) (q.x * invSqrt);
            this.y = (float) (q.y * invSqrt);
            this.z = (float) (q.z * invSqrt);
        }

        this.angle = (float) (acos + acos);
        return this;
    }

    public AxisAngle4f set(Matrix3f m) {
        float nm00 = m.m00;
        float nm01 = m.m01;
        float nm02 = m.m02;
        float nm10 = m.m10;
        float nm11 = m.m11;
        float nm12 = m.m12;
        float nm20 = m.m20;
        float nm21 = m.m21;
        float nm22 = m.m22;
        float lenX = Math.invsqrt(m.m00 * m.m00 + m.m01 * m.m01 + m.m02 * m.m02);
        float lenY = Math.invsqrt(m.m10 * m.m10 + m.m11 * m.m11 + m.m12 * m.m12);
        float lenZ = Math.invsqrt(m.m20 * m.m20 + m.m21 * m.m21 + m.m22 * m.m22);
        nm00 *= lenX;
        nm01 *= lenX;
        nm02 *= lenX;
        nm10 *= lenY;
        nm11 *= lenY;
        nm12 *= lenY;
        nm20 *= lenZ;
        nm21 *= lenZ;
        nm22 *= lenZ;
        float epsilon = 1.0E-4F;
        float epsilon2 = 0.001F;
        float xx;
        if (Math.abs(nm10 - nm01) < epsilon && Math.abs(nm20 - nm02) < epsilon && Math.abs(nm21 - nm12) < epsilon) {
            if (Math.abs(nm10 + nm01) < epsilon2 && Math.abs(nm20 + nm02) < epsilon2 && Math.abs(nm21 + nm12) < epsilon2 && Math.abs(nm00 + nm11 + nm22 - 3.0F) < epsilon2) {
                this.x = 0.0F;
                this.y = 0.0F;
                this.z = 1.0F;
                this.angle = 0.0F;
            } else {
                this.angle = 3.1415927F;
                xx = (nm00 + 1.0F) / 2.0F;
                float yy = (nm11 + 1.0F) / 2.0F;
                float zz = (nm22 + 1.0F) / 2.0F;
                float xy = (nm10 + nm01) / 4.0F;
                float xz = (nm20 + nm02) / 4.0F;
                float yz = (nm21 + nm12) / 4.0F;
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
            this.angle = Math.safeAcos((nm00 + nm11 + nm22 - 1.0F) / 2.0F);
            this.x = (nm12 - nm21) / xx;
            this.y = (nm20 - nm02) / xx;
            this.z = (nm01 - nm10) / xx;
        }
        return this;
    }

    public AxisAngle4f set(Matrix3d m) {
        double nm00 = m.m00;
        double nm01 = m.m01;
        double nm02 = m.m02;
        double nm10 = m.m10;
        double nm11 = m.m11;
        double nm12 = m.m12;
        double nm20 = m.m20;
        double nm21 = m.m21;
        double nm22 = m.m22;
        double lenX = Math.invsqrt(m.m00 * m.m00 + m.m01 * m.m01 + m.m02 * m.m02);
        double lenY = Math.invsqrt(m.m10 * m.m10 + m.m11 * m.m11 + m.m12 * m.m12);
        double lenZ = Math.invsqrt(m.m20 * m.m20 + m.m21 * m.m21 + m.m22 * m.m22);
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
                this.x = 0.0F;
                this.y = 0.0F;
                this.z = 1.0F;
                this.angle = 0.0F;
            } else {
                this.angle = 3.1415927F;
                xx = (nm00 + 1.0) / 2.0;
                double yy = (nm11 + 1.0) / 2.0;
                double zz = (nm22 + 1.0) / 2.0;
                double xy = (nm10 + nm01) / 4.0;
                double xz = (nm20 + nm02) / 4.0;
                double yz = (nm21 + nm12) / 4.0;
                if (xx > yy && xx > zz) {
                    this.x = (float) Math.sqrt(xx);
                    this.y = (float) (xy / (double) this.x);
                    this.z = (float) (xz / (double) this.x);
                } else if (yy > zz) {
                    this.y = (float) Math.sqrt(yy);
                    this.x = (float) (xy / (double) this.y);
                    this.z = (float) (yz / (double) this.y);
                } else {
                    this.z = (float) Math.sqrt(zz);
                    this.x = (float) (xz / (double) this.z);
                    this.y = (float) (yz / (double) this.z);
                }

            }
        } else {
            xx = Math.sqrt((nm12 - nm21) * (nm12 - nm21) + (nm20 - nm02) * (nm20 - nm02) + (nm01 - nm10) * (nm01 - nm10));
            this.angle = (float) Math.safeAcos((nm00 + nm11 + nm22 - 1.0) / 2.0);
            this.x = (float) ((nm12 - nm21) / xx);
            this.y = (float) ((nm20 - nm02) / xx);
            this.z = (float) ((nm01 - nm10) / xx);
        }
        return this;
    }

    public AxisAngle4f set(Matrix4f m) {
        float nm00 = m.m00;
        float nm01 = m.m01;
        float nm02 = m.m02;
        float nm10 = m.m10;
        float nm11 = m.m11;
        float nm12 = m.m12;
        float nm20 = m.m20;
        float nm21 = m.m21;
        float nm22 = m.m22;
        float lenX = Math.invsqrt(m.m00 * m.m00 + m.m01 * m.m01 + m.m02 * m.m02);
        float lenY = Math.invsqrt(m.m10 * m.m10 + m.m11 * m.m11 + m.m12 * m.m12);
        float lenZ = Math.invsqrt(m.m20 * m.m20 + m.m21 * m.m21 + m.m22 * m.m22);
        nm00 *= lenX;
        nm01 *= lenX;
        nm02 *= lenX;
        nm10 *= lenY;
        nm11 *= lenY;
        nm12 *= lenY;
        nm20 *= lenZ;
        nm21 *= lenZ;
        nm22 *= lenZ;
        float epsilon = 1.0E-4F;
        float epsilon2 = 0.001F;
        float xx;
        if (Math.abs(nm10 - nm01) < epsilon && Math.abs(nm20 - nm02) < epsilon && Math.abs(nm21 - nm12) < epsilon) {
            if (Math.abs(nm10 + nm01) < epsilon2 && Math.abs(nm20 + nm02) < epsilon2 && Math.abs(nm21 + nm12) < epsilon2 && Math.abs(nm00 + nm11 + nm22 - 3.0F) < epsilon2) {
                this.x = 0.0F;
                this.y = 0.0F;
                this.z = 1.0F;
                this.angle = 0.0F;
            } else {
                this.angle = 3.1415927F;
                xx = (nm00 + 1.0F) / 2.0F;
                float yy = (nm11 + 1.0F) / 2.0F;
                float zz = (nm22 + 1.0F) / 2.0F;
                float xy = (nm10 + nm01) / 4.0F;
                float xz = (nm20 + nm02) / 4.0F;
                float yz = (nm21 + nm12) / 4.0F;
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
            this.angle = Math.safeAcos((nm00 + nm11 + nm22 - 1.0F) / 2.0F);
            this.x = (nm12 - nm21) / xx;
            this.y = (nm20 - nm02) / xx;
            this.z = (nm01 - nm10) / xx;
        }
        return this;
    }

    public AxisAngle4f set(Matrix4x3f m) {
        float nm00 = m.m00;
        float nm01 = m.m01;
        float nm02 = m.m02;
        float nm10 = m.m10;
        float nm11 = m.m11;
        float nm12 = m.m12;
        float nm20 = m.m20;
        float nm21 = m.m21;
        float nm22 = m.m22;
        float lenX = Math.invsqrt(m.m00 * m.m00 + m.m01 * m.m01 + m.m02 * m.m02);
        float lenY = Math.invsqrt(m.m10 * m.m10 + m.m11 * m.m11 + m.m12 * m.m12);
        float lenZ = Math.invsqrt(m.m20 * m.m20 + m.m21 * m.m21 + m.m22 * m.m22);
        nm00 *= lenX;
        nm01 *= lenX;
        nm02 *= lenX;
        nm10 *= lenY;
        nm11 *= lenY;
        nm12 *= lenY;
        nm20 *= lenZ;
        nm21 *= lenZ;
        nm22 *= lenZ;
        float epsilon = 1.0E-4F;
        float epsilon2 = 0.001F;
        float xx;
        if (Math.abs(nm10 - nm01) < epsilon && Math.abs(nm20 - nm02) < epsilon && Math.abs(nm21 - nm12) < epsilon) {
            if (Math.abs(nm10 + nm01) < epsilon2 && Math.abs(nm20 + nm02) < epsilon2 && Math.abs(nm21 + nm12) < epsilon2 && Math.abs(nm00 + nm11 + nm22 - 3.0F) < epsilon2) {
                this.x = 0.0F;
                this.y = 0.0F;
                this.z = 1.0F;
                this.angle = 0.0F;
            } else {
                this.angle = 3.1415927F;
                xx = (nm00 + 1.0F) / 2.0F;
                float yy = (nm11 + 1.0F) / 2.0F;
                float zz = (nm22 + 1.0F) / 2.0F;
                float xy = (nm10 + nm01) / 4.0F;
                float xz = (nm20 + nm02) / 4.0F;
                float yz = (nm21 + nm12) / 4.0F;
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
            this.angle = Math.safeAcos((nm00 + nm11 + nm22 - 1.0F) / 2.0F);
            this.x = (nm12 - nm21) / xx;
            this.y = (nm20 - nm02) / xx;
            this.z = (nm01 - nm10) / xx;
        }
        return this;
    }

    public AxisAngle4f set(Matrix4d m) {
        double nm00 = m.m00;
        double nm01 = m.m01;
        double nm02 = m.m02;
        double nm10 = m.m10;
        double nm11 = m.m11;
        double nm12 = m.m12;
        double nm20 = m.m20;
        double nm21 = m.m21;
        double nm22 = m.m22;
        double lenX = Math.invsqrt(m.m00 * m.m00 + m.m01 * m.m01 + m.m02 * m.m02);
        double lenY = Math.invsqrt(m.m10 * m.m10 + m.m11 * m.m11 + m.m12 * m.m12);
        double lenZ = Math.invsqrt(m.m20 * m.m20 + m.m21 * m.m21 + m.m22 * m.m22);
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
                this.x = 0.0F;
                this.y = 0.0F;
                this.z = 1.0F;
                this.angle = 0.0F;
            } else {
                this.angle = 3.1415927F;
                xx = (nm00 + 1.0) / 2.0;
                double yy = (nm11 + 1.0) / 2.0;
                double zz = (nm22 + 1.0) / 2.0;
                double xy = (nm10 + nm01) / 4.0;
                double xz = (nm20 + nm02) / 4.0;
                double yz = (nm21 + nm12) / 4.0;
                if (xx > yy && xx > zz) {
                    this.x = (float) Math.sqrt(xx);
                    this.y = (float) (xy / (double) this.x);
                    this.z = (float) (xz / (double) this.x);
                } else if (yy > zz) {
                    this.y = (float) Math.sqrt(yy);
                    this.x = (float) (xy / (double) this.y);
                    this.z = (float) (yz / (double) this.y);
                } else {
                    this.z = (float) Math.sqrt(zz);
                    this.x = (float) (xz / (double) this.z);
                    this.y = (float) (yz / (double) this.z);
                }

            }
        } else {
            xx = Math.sqrt((nm12 - nm21) * (nm12 - nm21) + (nm20 - nm02) * (nm20 - nm02) + (nm01 - nm10) * (nm01 - nm10));
            this.angle = (float) Math.safeAcos((nm00 + nm11 + nm22 - 1.0) / 2.0);
            this.x = (float) ((nm12 - nm21) / xx);
            this.y = (float) ((nm20 - nm02) / xx);
            this.z = (float) ((nm01 - nm10) / xx);
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

    public AxisAngle4f normalize() {
        float invLength = Math.invsqrt(this.x * this.x + this.y * this.y + this.z * this.z);
        this.x *= invLength;
        this.y *= invLength;
        this.z *= invLength;
        return this;
    }

    public AxisAngle4f rotate(float ang) {
        this.angle += ang;
        this.angle = (float) (((double) this.angle < 0.0 ? 6.283185307179586 + (double) this.angle % 6.283185307179586 : (double) this.angle) % 6.283185307179586);
        return this;
    }

    public Vector3f transform(Vector3f v) {
        return this.transform(v, v);
    }

    public Vector3f transform(Vector3f v, Vector3f dest) {
        double sin = Math.sin(this.angle);
        double cos = Math.cosFromSin(sin, this.angle);
        float dot = this.x * v.x + this.y * v.y + this.z * v.z;
        dest.set((float) ((double) v.x * cos + sin * (double) (this.y * v.z - this.z * v.y) + (1.0 - cos) * (double) dot * (double) this.x), (float) ((double) v.y * cos + sin * (double) (this.z * v.x - this.x * v.z) + (1.0 - cos) * (double) dot * (double) this.y), (float) ((double) v.z * cos + sin * (double) (this.x * v.y - this.y * v.x) + (1.0 - cos) * (double) dot * (double) this.z));
        return dest;
    }

    public Vector4f transform(Vector4f v) {
        return this.transform(v, v);
    }

    public Vector4f transform(Vector4f v, Vector4f dest) {
        double sin = Math.sin(this.angle);
        double cos = Math.cosFromSin(sin, this.angle);
        float dot = this.x * v.x + this.y * v.y + this.z * v.z;
        dest.set((float) ((double) v.x * cos + sin * (double) (this.y * v.z - this.z * v.y) + (1.0 - cos) * (double) dot * (double) this.x), (float) ((double) v.y * cos + sin * (double) (this.z * v.x - this.x * v.z) + (1.0 - cos) * (double) dot * (double) this.y), (float) ((double) v.z * cos + sin * (double) (this.x * v.y - this.y * v.x) + (1.0 - cos) * (double) dot * (double) this.z), dest.w);
        return dest;
    }

    public String toString() {
        return "(" + x + "," + y + "," + z + " <| " + angle + ")";
    }

    public int hashCode() {
        int result = 1;
        float nangle = (float) (((double) this.angle < 0.0 ? 6.283185307179586 + (double) this.angle % 6.283185307179586 : (double) this.angle) % 6.283185307179586);
        result = 31 * result + Float.floatToIntBits(nangle);
        result = 31 * result + Float.floatToIntBits(this.x);
        result = 31 * result + Float.floatToIntBits(this.y);
        result = 31 * result + Float.floatToIntBits(this.z);
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
            AxisAngle4f other = (AxisAngle4f) obj;
            float nangle = (float) (((double) this.angle < 0.0 ? 6.283185307179586 + (double) this.angle % 6.283185307179586 : (double) this.angle) % 6.283185307179586);
            float nangleOther = (float) (((double) other.angle < 0.0 ? 6.283185307179586 + (double) other.angle % 6.283185307179586 : (double) other.angle) % 6.283185307179586);
            if (Float.floatToIntBits(nangle) != Float.floatToIntBits(nangleOther)) {
                return false;
            } else if (Float.floatToIntBits(this.x) != Float.floatToIntBits(other.x)) {
                return false;
            } else if (Float.floatToIntBits(this.y) != Float.floatToIntBits(other.y)) {
                return false;
            } else {
                return Float.floatToIntBits(this.z) == Float.floatToIntBits(other.z);
            }
        }
    }

}
