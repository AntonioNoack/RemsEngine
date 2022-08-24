package org.joml;

@SuppressWarnings({"unused", "UnusedReturnValue"})
public class Quaterniond {
    
    public double x;
    public double y;
    public double z;
    public double w;

    public Quaterniond() {
        this.w = 1.0;
    }

    public Quaterniond(double x, double y, double z, double w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
    }

    public Quaterniond(Quaterniond source) {
        this.x = source.x;
        this.y = source.y;
        this.z = source.z;
        this.w = source.w;
    }

    public Quaterniond(Quaternionf source) {
        this.x = source.x;
        this.y = source.y;
        this.z = source.z;
        this.w = source.w;
    }

    public Quaterniond(AxisAngle4f axisAngle) {
        double s = Math.sin((double) axisAngle.angle * 0.5);
        this.x = (double) axisAngle.x * s;
        this.y = (double) axisAngle.y * s;
        this.z = (double) axisAngle.z * s;
        this.w = Math.cosFromSin(s, (double) axisAngle.angle * 0.5);
    }

    public Quaterniond(AxisAngle4d axisAngle) {
        double s = Math.sin(axisAngle.angle * 0.5);
        this.x = axisAngle.x * s;
        this.y = axisAngle.y * s;
        this.z = axisAngle.z * s;
        this.w = Math.cosFromSin(s, axisAngle.angle * 0.5);
    }

    public Quaterniond normalize() {
        double invNorm = Math.invsqrt(this.lengthSquared());
        this.x *= invNorm;
        this.y *= invNorm;
        this.z *= invNorm;
        this.w *= invNorm;
        return this;
    }

    public Quaterniond normalize(Quaterniond dest) {
        double invNorm = Math.invsqrt(this.lengthSquared());
        dest.x = this.x * invNorm;
        dest.y = this.y * invNorm;
        dest.z = this.z * invNorm;
        dest.w = this.w * invNorm;
        return dest;
    }

    public Quaterniond add(double x, double y, double z, double w) {
        return this.add(x, y, z, w, this);
    }

    public Quaterniond add(double x, double y, double z, double w, Quaterniond dest) {
        dest.x = this.x + x;
        dest.y = this.y + y;
        dest.z = this.z + z;
        dest.w = this.w + w;
        return dest;
    }

    public Quaterniond add(Quaterniond q2) {
        this.x += q2.x;
        this.y += q2.y;
        this.z += q2.z;
        this.w += q2.w;
        return this;
    }

    public Quaterniond add(Quaterniond q2, Quaterniond dest) {
        dest.x = this.x + q2.x;
        dest.y = this.y + q2.y;
        dest.z = this.z + q2.z;
        dest.w = this.w + q2.w;
        return dest;
    }

    public double dot(Quaterniond otherQuat) {
        return this.x * otherQuat.x + this.y * otherQuat.y + this.z * otherQuat.z + this.w * otherQuat.w;
    }

    public double angle() {
        return 2.0 * Math.safeAcos(this.w);
    }

    public Matrix3d get(Matrix3d dest) {
        return dest.set(this);
    }

    public Matrix3f get(Matrix3f dest) {
        return dest.set(this);
    }

    public Matrix4d get(Matrix4d dest) {
        return dest.set(this);
    }

    public Matrix4f get(Matrix4f dest) {
        return dest.set(this);
    }

    public AxisAngle4f get(AxisAngle4f dest) {
        double x = this.x;
        double y = this.y;
        double z = this.z;
        double w = this.w;
        double s;
        if (w > 1.0) {
            s = Math.invsqrt(this.lengthSquared());
            x *= s;
            y *= s;
            z *= s;
            w *= s;
        }

        dest.angle = (float) (2.0 * Math.acos(w));
        s = Math.sqrt(1.0 - w * w);
        if (s < 0.001) {
            dest.x = (float) x;
            dest.y = (float) y;
            dest.z = (float) z;
        } else {
            s = 1.0 / s;
            dest.x = (float) (x * s);
            dest.y = (float) (y * s);
            dest.z = (float) (z * s);
        }

        return dest;
    }

    public AxisAngle4d get(AxisAngle4d dest) {
        double x = this.x;
        double y = this.y;
        double z = this.z;
        double w = this.w;
        double s;
        if (w > 1.0) {
            s = Math.invsqrt(this.lengthSquared());
            x *= s;
            y *= s;
            z *= s;
            w *= s;
        }

        dest.angle = 2.0 * Math.acos(w);
        s = Math.sqrt(1.0 - w * w);
        if (s < 0.001) {
            dest.x = x;
            dest.y = y;
            dest.z = z;
        } else {
            s = 1.0 / s;
            dest.x = x * s;
            dest.y = y * s;
            dest.z = z * s;
        }

        return dest;
    }

    public Quaterniond get(Quaterniond dest) {
        return dest.set(this);
    }

    public Quaternionf get(Quaternionf dest) {
        return dest.set(this);
    }

    public Quaterniond set(double x, double y, double z, double w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
        return this;
    }

    public Quaterniond set(Quaterniond q) {
        this.x = q.x;
        this.y = q.y;
        this.z = q.z;
        this.w = q.w;
        return this;
    }

    public Quaterniond set(Quaternionf q) {
        this.x = q.x;
        this.y = q.y;
        this.z = q.z;
        this.w = q.w;
        return this;
    }

    public Quaterniond set(AxisAngle4f axisAngle) {
        return this.setAngleAxis(axisAngle.angle, axisAngle.x, axisAngle.y, axisAngle.z);
    }

    public Quaterniond set(AxisAngle4d axisAngle) {
        return this.setAngleAxis(axisAngle.angle, axisAngle.x, axisAngle.y, axisAngle.z);
    }

    public Quaterniond setAngleAxis(double angle, double x, double y, double z) {
        double s = Math.sin(angle * 0.5);
        this.x = x * s;
        this.y = y * s;
        this.z = z * s;
        this.w = Math.cosFromSin(s, angle * 0.5);
        return this;
    }

    public Quaterniond setAngleAxis(double angle, Vector3d axis) {
        return this.setAngleAxis(angle, axis.x, axis.y, axis.z);
    }

    private void setFromUnnormalized(double m00, double m01, double m02, double m10, double m11, double m12, double m20, double m21, double m22) {
        double lenX = Math.invsqrt(m00 * m00 + m01 * m01 + m02 * m02);
        double lenY = Math.invsqrt(m10 * m10 + m11 * m11 + m12 * m12);
        double lenZ = Math.invsqrt(m20 * m20 + m21 * m21 + m22 * m22);
        double nm00 = m00 * lenX;
        double nm01 = m01 * lenX;
        double nm02 = m02 * lenX;
        double nm10 = m10 * lenY;
        double nm11 = m11 * lenY;
        double nm12 = m12 * lenY;
        double nm20 = m20 * lenZ;
        double nm21 = m21 * lenZ;
        double nm22 = m22 * lenZ;
        this.setFromNormalized(nm00, nm01, nm02, nm10, nm11, nm12, nm20, nm21, nm22);
    }

    private void setFromNormalized(double m00, double m01, double m02, double m10, double m11, double m12, double m20, double m21, double m22) {
        double tr = m00 + m11 + m22;
        double t;
        if (tr >= 0.0) {
            t = Math.sqrt(tr + 1.0);
            this.w = t * 0.5;
            t = 0.5 / t;
            this.x = (m12 - m21) * t;
            this.y = (m20 - m02) * t;
            this.z = (m01 - m10) * t;
        } else if (m00 >= m11 && m00 >= m22) {
            t = Math.sqrt(m00 - (m11 + m22) + 1.0);
            this.x = t * 0.5;
            t = 0.5 / t;
            this.y = (m10 + m01) * t;
            this.z = (m02 + m20) * t;
            this.w = (m12 - m21) * t;
        } else if (m11 > m22) {
            t = Math.sqrt(m11 - (m22 + m00) + 1.0);
            this.y = t * 0.5;
            t = 0.5 / t;
            this.z = (m21 + m12) * t;
            this.x = (m10 + m01) * t;
            this.w = (m20 - m02) * t;
        } else {
            t = Math.sqrt(m22 - (m00 + m11) + 1.0);
            this.z = t * 0.5;
            t = 0.5 / t;
            this.x = (m02 + m20) * t;
            this.y = (m21 + m12) * t;
            this.w = (m01 - m10) * t;
        }

    }

    public Quaterniond setFromUnnormalized(Matrix4f mat) {
        this.setFromUnnormalized(mat.m00, mat.m01, mat.m02, mat.m10, mat.m11, mat.m12, mat.m20, mat.m21, mat.m22);
        return this;
    }

    public Quaterniond setFromUnnormalized(Matrix4x3f mat) {
        this.setFromUnnormalized(mat.m00, mat.m01, mat.m02, mat.m10, mat.m11, mat.m12, mat.m20, mat.m21, mat.m22);
        return this;
    }

    public Quaterniond setFromUnnormalized(Matrix4x3d mat) {
        this.setFromUnnormalized(mat.m00, mat.m01, mat.m02, mat.m10, mat.m11, mat.m12, mat.m20, mat.m21, mat.m22);
        return this;
    }

    public Quaterniond setFromNormalized(Matrix4f mat) {
        this.setFromNormalized(mat.m00, mat.m01, mat.m02, mat.m10, mat.m11, mat.m12, mat.m20, mat.m21, mat.m22);
        return this;
    }

    public Quaterniond setFromNormalized(Matrix4x3f mat) {
        this.setFromNormalized(mat.m00, mat.m01, mat.m02, mat.m10, mat.m11, mat.m12, mat.m20, mat.m21, mat.m22);
        return this;
    }

    public Quaterniond setFromNormalized(Matrix4x3d mat) {
        this.setFromNormalized(mat.m00, mat.m01, mat.m02, mat.m10, mat.m11, mat.m12, mat.m20, mat.m21, mat.m22);
        return this;
    }

    public Quaterniond setFromUnnormalized(Matrix4d mat) {
        this.setFromUnnormalized(mat.m00, mat.m01, mat.m02, mat.m10, mat.m11, mat.m12, mat.m20, mat.m21, mat.m22);
        return this;
    }

    public Quaterniond setFromNormalized(Matrix4d mat) {
        this.setFromNormalized(mat.m00, mat.m01, mat.m02, mat.m10, mat.m11, mat.m12, mat.m20, mat.m21, mat.m22);
        return this;
    }

    public Quaterniond setFromUnnormalized(Matrix3f mat) {
        this.setFromUnnormalized(mat.m00, mat.m01, mat.m02, mat.m10, mat.m11, mat.m12, mat.m20, mat.m21, mat.m22);
        return this;
    }

    public Quaterniond setFromNormalized(Matrix3f mat) {
        this.setFromNormalized(mat.m00, mat.m01, mat.m02, mat.m10, mat.m11, mat.m12, mat.m20, mat.m21, mat.m22);
        return this;
    }

    public Quaterniond setFromUnnormalized(Matrix3d mat) {
        this.setFromUnnormalized(mat.m00, mat.m01, mat.m02, mat.m10, mat.m11, mat.m12, mat.m20, mat.m21, mat.m22);
        return this;
    }

    public Quaterniond setFromNormalized(Matrix3d mat) {
        this.setFromNormalized(mat.m00, mat.m01, mat.m02, mat.m10, mat.m11, mat.m12, mat.m20, mat.m21, mat.m22);
        return this;
    }

    public Quaterniond fromAxisAngleRad(Vector3d axis, double angle) {
        return this.fromAxisAngleRad(axis.x, axis.y, axis.z, angle);
    }

    public Quaterniond fromAxisAngleRad(double axisX, double axisY, double axisZ, double angle) {
        double halfAngle = angle / 2.0;
        double sinAngle = Math.sin(halfAngle);
        double vLength = Math.sqrt(axisX * axisX + axisY * axisY + axisZ * axisZ);
        this.x = axisX / vLength * sinAngle;
        this.y = axisY / vLength * sinAngle;
        this.z = axisZ / vLength * sinAngle;
        this.w = Math.cosFromSin(sinAngle, halfAngle);
        return this;
    }

    public Quaterniond fromAxisAngleDeg(Vector3d axis, double angle) {
        return this.fromAxisAngleRad(axis.x, axis.y, axis.z, Math.toRadians(angle));
    }

    public Quaterniond fromAxisAngleDeg(double axisX, double axisY, double axisZ, double angle) {
        return this.fromAxisAngleRad(axisX, axisY, axisZ, Math.toRadians(angle));
    }

    public Quaterniond mul(Quaterniond q) {
        return this.mul(q, this);
    }

    public Quaterniond mul(Quaterniond q, Quaterniond dest) {
        return this.mul(q.x, q.y, q.z, q.w, dest);
    }

    public Quaterniond mul(double qx, double qy, double qz, double qw) {
        return this.mul(qx, qy, qz, qw, this);
    }

    public Quaterniond mul(double qx, double qy, double qz, double qw, Quaterniond dest) {
        return dest.set(Math.fma(this.w, qx, Math.fma(this.x, qw, Math.fma(this.y, qz, -this.z * qy))), Math.fma(this.w, qy, Math.fma(-this.x, qz, Math.fma(this.y, qw, this.z * qx))), Math.fma(this.w, qz, Math.fma(this.x, qy, Math.fma(-this.y, qx, this.z * qw))), Math.fma(this.w, qw, Math.fma(-this.x, qx, Math.fma(-this.y, qy, -this.z * qz))));
    }

    public Quaterniond premul(Quaterniond q) {
        return this.premul(q, this);
    }

    public Quaterniond premul(Quaterniond q, Quaterniond dest) {
        return this.premul(q.x, q.y, q.z, q.w, dest);
    }

    public Quaterniond premul(double qx, double qy, double qz, double qw) {
        return this.premul(qx, qy, qz, qw, this);
    }

    public Quaterniond premul(double qx, double qy, double qz, double qw, Quaterniond dest) {
        return dest.set(Math.fma(qw, this.x, Math.fma(qx, this.w, Math.fma(qy, this.z, -qz * this.y))), Math.fma(qw, this.y, Math.fma(-qx, this.z, Math.fma(qy, this.w, qz * this.x))), Math.fma(qw, this.z, Math.fma(qx, this.y, Math.fma(-qy, this.x, qz * this.w))), Math.fma(qw, this.w, Math.fma(-qx, this.x, Math.fma(-qy, this.y, -qz * this.z))));
    }

    public Vector3d transform(Vector3d vec) {
        return this.transform(vec.x, vec.y, vec.z, vec);
    }

    public Vector3d transformInverse(Vector3d vec) {
        return this.transformInverse(vec.x, vec.y, vec.z, vec);
    }

    public Vector3d transformUnit(Vector3d vec) {
        return this.transformUnit(vec.x, vec.y, vec.z, vec);
    }

    public Vector3d transformInverseUnit(Vector3d vec) {
        return this.transformInverseUnit(vec.x, vec.y, vec.z, vec);
    }

    public Vector3d transformPositiveX(Vector3d dest) {
        double ww = this.w * this.w;
        double xx = this.x * this.x;
        double yy = this.y * this.y;
        double zz = this.z * this.z;
        double zw = this.z * this.w;
        double xy = this.x * this.y;
        double xz = this.x * this.z;
        double yw = this.y * this.w;
        dest.x = ww + xx - zz - yy;
        dest.y = xy + zw + zw + xy;
        dest.z = xz - yw + xz - yw;
        return dest;
    }

    public Vector4d transformPositiveX(Vector4d dest) {
        double ww = this.w * this.w;
        double xx = this.x * this.x;
        double yy = this.y * this.y;
        double zz = this.z * this.z;
        double zw = this.z * this.w;
        double xy = this.x * this.y;
        double xz = this.x * this.z;
        double yw = this.y * this.w;
        dest.x = ww + xx - zz - yy;
        dest.y = xy + zw + zw + xy;
        dest.z = xz - yw + xz - yw;
        return dest;
    }

    public Vector3d transformUnitPositiveX(Vector3d dest) {
        double yy = this.y * this.y;
        double zz = this.z * this.z;
        double xy = this.x * this.y;
        double xz = this.x * this.z;
        double yw = this.y * this.w;
        double zw = this.z * this.w;
        dest.x = 1.0 - yy - yy - zz - zz;
        dest.y = xy + zw + xy + zw;
        dest.z = xz - yw + xz - yw;
        return dest;
    }

    public Vector4d transformUnitPositiveX(Vector4d dest) {
        double yy = this.y * this.y;
        double zz = this.z * this.z;
        double xy = this.x * this.y;
        double xz = this.x * this.z;
        double yw = this.y * this.w;
        double zw = this.z * this.w;
        dest.x = 1.0 - yy - yy - zz - zz;
        dest.y = xy + zw + xy + zw;
        dest.z = xz - yw + xz - yw;
        return dest;
    }

    public Vector3d transformPositiveY(Vector3d dest) {
        double ww = this.w * this.w;
        double xx = this.x * this.x;
        double yy = this.y * this.y;
        double zz = this.z * this.z;
        double zw = this.z * this.w;
        double xy = this.x * this.y;
        double yz = this.y * this.z;
        double xw = this.x * this.w;
        dest.x = -zw + xy - zw + xy;
        dest.y = yy - zz + ww - xx;
        dest.z = yz + yz + xw + xw;
        return dest;
    }

    public Vector4d transformPositiveY(Vector4d dest) {
        double ww = this.w * this.w;
        double xx = this.x * this.x;
        double yy = this.y * this.y;
        double zz = this.z * this.z;
        double zw = this.z * this.w;
        double xy = this.x * this.y;
        double yz = this.y * this.z;
        double xw = this.x * this.w;
        dest.x = -zw + xy - zw + xy;
        dest.y = yy - zz + ww - xx;
        dest.z = yz + yz + xw + xw;
        return dest;
    }

    public Vector4d transformUnitPositiveY(Vector4d dest) {
        double xx = this.x * this.x;
        double zz = this.z * this.z;
        double xy = this.x * this.y;
        double yz = this.y * this.z;
        double xw = this.x * this.w;
        double zw = this.z * this.w;
        dest.x = xy - zw + xy - zw;
        dest.y = 1.0 - xx - xx - zz - zz;
        dest.z = yz + yz + xw + xw;
        return dest;
    }

    public Vector3d transformUnitPositiveY(Vector3d dest) {
        double xx = this.x * this.x;
        double zz = this.z * this.z;
        double xy = this.x * this.y;
        double yz = this.y * this.z;
        double xw = this.x * this.w;
        double zw = this.z * this.w;
        dest.x = xy - zw + xy - zw;
        dest.y = 1.0 - xx - xx - zz - zz;
        dest.z = yz + yz + xw + xw;
        return dest;
    }

    public Vector3d transformPositiveZ(Vector3d dest) {
        double ww = this.w * this.w;
        double xx = this.x * this.x;
        double yy = this.y * this.y;
        double zz = this.z * this.z;
        double xz = this.x * this.z;
        double yw = this.y * this.w;
        double yz = this.y * this.z;
        double xw = this.x * this.w;
        dest.x = yw + xz + xz + yw;
        dest.y = yz + yz - xw - xw;
        dest.z = zz - yy - xx + ww;
        return dest;
    }

    public Vector4d transformPositiveZ(Vector4d dest) {
        double ww = this.w * this.w;
        double xx = this.x * this.x;
        double yy = this.y * this.y;
        double zz = this.z * this.z;
        double xz = this.x * this.z;
        double yw = this.y * this.w;
        double yz = this.y * this.z;
        double xw = this.x * this.w;
        dest.x = yw + xz + xz + yw;
        dest.y = yz + yz - xw - xw;
        dest.z = zz - yy - xx + ww;
        return dest;
    }

    public Vector4d transformUnitPositiveZ(Vector4d dest) {
        double xx = this.x * this.x;
        double yy = this.y * this.y;
        double xz = this.x * this.z;
        double yz = this.y * this.z;
        double xw = this.x * this.w;
        double yw = this.y * this.w;
        dest.x = xz + yw + xz + yw;
        dest.y = yz + yz - xw - xw;
        dest.z = 1.0 - xx - xx - yy - yy;
        return dest;
    }

    public Vector3d transformUnitPositiveZ(Vector3d dest) {
        double xx = this.x * this.x;
        double yy = this.y * this.y;
        double xz = this.x * this.z;
        double yz = this.y * this.z;
        double xw = this.x * this.w;
        double yw = this.y * this.w;
        dest.x = xz + yw + xz + yw;
        dest.y = yz + yz - xw - xw;
        dest.z = 1.0 - xx - xx - yy - yy;
        return dest;
    }

    public Vector4d transform(Vector4d vec) {
        return this.transform( vec, vec);
    }

    public Vector4d transformInverse(Vector4d vec) {
        return this.transformInverse( vec, vec);
    }

    public Vector3d transform(Vector3d vec, Vector3d dest) {
        return this.transform(vec.x, vec.y, vec.z, dest);
    }

    public Vector3d transformInverse(Vector3d vec, Vector3d dest) {
        return this.transformInverse(vec.x, vec.y, vec.z, dest);
    }

    public Vector3d transform(double x, double y, double z, Vector3d dest) {
        double xx = this.x * this.x;
        double yy = this.y * this.y;
        double zz = this.z * this.z;
        double ww = this.w * this.w;
        double xy = this.x * this.y;
        double xz = this.x * this.z;
        double yz = this.y * this.z;
        double xw = this.x * this.w;
        double zw = this.z * this.w;
        double yw = this.y * this.w;
        double k = 1.0 / (xx + yy + zz + ww);
        return dest.set(Math.fma((xx - yy - zz + ww) * k, x, Math.fma(2.0 * (xy - zw) * k, y, 2.0 * (xz + yw) * k * z)), Math.fma(2.0 * (xy + zw) * k, x, Math.fma((yy - xx - zz + ww) * k, y, 2.0 * (yz - xw) * k * z)), Math.fma(2.0 * (xz - yw) * k, x, Math.fma(2.0 * (yz + xw) * k, y, (zz - xx - yy + ww) * k * z)));
    }

    public Vector3d transformInverse(double x, double y, double z, Vector3d dest) {
        double n = 1.0 / Math.fma(this.x, this.x, Math.fma(this.y, this.y, Math.fma(this.z, this.z, this.w * this.w)));
        double qx = this.x * n;
        double qy = this.y * n;
        double qz = this.z * n;
        double qw = this.w * n;
        double xx = qx * qx;
        double yy = qy * qy;
        double zz = qz * qz;
        double ww = qw * qw;
        double xy = qx * qy;
        double xz = qx * qz;
        double yz = qy * qz;
        double xw = qx * qw;
        double zw = qz * qw;
        double yw = qy * qw;
        double k = 1.0 / (xx + yy + zz + ww);
        return dest.set(Math.fma((xx - yy - zz + ww) * k, x, Math.fma(2.0 * (xy + zw) * k, y, 2.0 * (xz - yw) * k * z)), Math.fma(2.0 * (xy - zw) * k, x, Math.fma((yy - xx - zz + ww) * k, y, 2.0 * (yz + xw) * k * z)), Math.fma(2.0 * (xz + yw) * k, x, Math.fma(2.0 * (yz - xw) * k, y, (zz - xx - yy + ww) * k * z)));
    }

    public Vector4d transform(Vector4d vec, Vector4d dest) {
        return this.transform(vec.x, vec.y, vec.z, dest);
    }

    public Vector4d transformInverse(Vector4d vec, Vector4d dest) {
        return this.transformInverse(vec.x, vec.y, vec.z, dest);
    }

    public Vector4d transform(double x, double y, double z, Vector4d dest) {
        double xx = this.x * this.x;
        double yy = this.y * this.y;
        double zz = this.z * this.z;
        double ww = this.w * this.w;
        double xy = this.x * this.y;
        double xz = this.x * this.z;
        double yz = this.y * this.z;
        double xw = this.x * this.w;
        double zw = this.z * this.w;
        double yw = this.y * this.w;
        double k = 1.0 / (xx + yy + zz + ww);
        return dest.set(Math.fma((xx - yy - zz + ww) * k, x, Math.fma(2.0 * (xy - zw) * k, y, 2.0 * (xz + yw) * k * z)), Math.fma(2.0 * (xy + zw) * k, x, Math.fma((yy - xx - zz + ww) * k, y, 2.0 * (yz - xw) * k * z)), Math.fma(2.0 * (xz - yw) * k, x, Math.fma(2.0 * (yz + xw) * k, y, (zz - xx - yy + ww) * k * z)), dest.w);
    }

    public Vector4d transformInverse(double x, double y, double z, Vector4d dest) {
        double n = 1.0 / Math.fma(this.x, this.x, Math.fma(this.y, this.y, Math.fma(this.z, this.z, this.w * this.w)));
        double qx = this.x * n;
        double qy = this.y * n;
        double qz = this.z * n;
        double qw = this.w * n;
        double xx = qx * qx;
        double yy = qy * qy;
        double zz = qz * qz;
        double ww = qw * qw;
        double xy = qx * qy;
        double xz = qx * qz;
        double yz = qy * qz;
        double xw = qx * qw;
        double zw = qz * qw;
        double yw = qy * qw;
        double k = 1.0 / (xx + yy + zz + ww);
        return dest.set(Math.fma((xx - yy - zz + ww) * k, x, Math.fma(2.0 * (xy + zw) * k, y, 2.0 * (xz - yw) * k * z)), Math.fma(2.0 * (xy - zw) * k, x, Math.fma((yy - xx - zz + ww) * k, y, 2.0 * (yz + xw) * k * z)), Math.fma(2.0 * (xz + yw) * k, x, Math.fma(2.0 * (yz - xw) * k, y, (zz - xx - yy + ww) * k * z)));
    }

    public Vector3f transform(Vector3f vec) {
        return this.transform(vec.x, vec.y, vec.z, vec);
    }

    public Vector3f transformInverse(Vector3f vec) {
        return this.transformInverse(vec.x, vec.y, vec.z, vec);
    }

    public Vector4d transformUnit(Vector4d vec) {
        return this.transformUnit(vec, vec);
    }

    public Vector4d transformInverseUnit(Vector4d vec) {
        return this.transformInverseUnit( vec, vec);
    }

    public Vector3d transformUnit(Vector3d vec, Vector3d dest) {
        return this.transformUnit(vec.x, vec.y, vec.z, dest);
    }

    public Vector3d transformInverseUnit(Vector3d vec, Vector3d dest) {
        return this.transformInverseUnit(vec.x, vec.y, vec.z, dest);
    }

    public Vector3d transformUnit(double x, double y, double z, Vector3d dest) {
        double xx = this.x * this.x;
        double xy = this.x * this.y;
        double xz = this.x * this.z;
        double xw = this.x * this.w;
        double yy = this.y * this.y;
        double yz = this.y * this.z;
        double yw = this.y * this.w;
        double zz = this.z * this.z;
        double zw = this.z * this.w;
        return dest.set(Math.fma(Math.fma(-2.0, yy + zz, 1.0), x, Math.fma(2.0 * (xy - zw), y, 2.0 * (xz + yw) * z)), Math.fma(2.0 * (xy + zw), x, Math.fma(Math.fma(-2.0, xx + zz, 1.0), y, 2.0 * (yz - xw) * z)), Math.fma(2.0 * (xz - yw), x, Math.fma(2.0 * (yz + xw), y, Math.fma(-2.0, xx + yy, 1.0) * z)));
    }

    public Vector3d transformInverseUnit(double x, double y, double z, Vector3d dest) {
        double xx = this.x * this.x;
        double xy = this.x * this.y;
        double xz = this.x * this.z;
        double xw = this.x * this.w;
        double yy = this.y * this.y;
        double yz = this.y * this.z;
        double yw = this.y * this.w;
        double zz = this.z * this.z;
        double zw = this.z * this.w;
        return dest.set(Math.fma(Math.fma(-2.0, yy + zz, 1.0), x, Math.fma(2.0 * (xy + zw), y, 2.0 * (xz - yw) * z)), Math.fma(2.0 * (xy - zw), x, Math.fma(Math.fma(-2.0, xx + zz, 1.0), y, 2.0 * (yz + xw) * z)), Math.fma(2.0 * (xz + yw), x, Math.fma(2.0 * (yz - xw), y, Math.fma(-2.0, xx + yy, 1.0) * z)));
    }

    public Vector4d transformUnit(Vector4d vec, Vector4d dest) {
        return this.transformUnit(vec.x, vec.y, vec.z, dest);
    }

    public Vector4d transformInverseUnit(Vector4d vec, Vector4d dest) {
        return this.transformInverseUnit(vec.x, vec.y, vec.z, dest);
    }

    public Vector4d transformUnit(double x, double y, double z, Vector4d dest) {
        double xx = this.x * this.x;
        double xy = this.x * this.y;
        double xz = this.x * this.z;
        double xw = this.x * this.w;
        double yy = this.y * this.y;
        double yz = this.y * this.z;
        double yw = this.y * this.w;
        double zz = this.z * this.z;
        double zw = this.z * this.w;
        return dest.set(Math.fma(Math.fma(-2.0, yy + zz, 1.0), x, Math.fma(2.0 * (xy - zw), y, 2.0 * (xz + yw) * z)), Math.fma(2.0 * (xy + zw), x, Math.fma(Math.fma(-2.0, xx + zz, 1.0), y, 2.0 * (yz - xw) * z)), Math.fma(2.0 * (xz - yw), x, Math.fma(2.0 * (yz + xw), y, Math.fma(-2.0, xx + yy, 1.0) * z)), dest.w);
    }

    public Vector4d transformInverseUnit(double x, double y, double z, Vector4d dest) {
        double xx = this.x * this.x;
        double xy = this.x * this.y;
        double xz = this.x * this.z;
        double xw = this.x * this.w;
        double yy = this.y * this.y;
        double yz = this.y * this.z;
        double yw = this.y * this.w;
        double zz = this.z * this.z;
        double zw = this.z * this.w;
        return dest.set(Math.fma(Math.fma(-2.0, yy + zz, 1.0), x, Math.fma(2.0 * (xy + zw), y, 2.0 * (xz - yw) * z)), Math.fma(2.0 * (xy - zw), x, Math.fma(Math.fma(-2.0, xx + zz, 1.0), y, 2.0 * (yz + xw) * z)), Math.fma(2.0 * (xz + yw), x, Math.fma(2.0 * (yz - xw), y, Math.fma(-2.0, xx + yy, 1.0) * z)), dest.w);
    }

    public Vector3f transformUnit(Vector3f vec) {
        return this.transformUnit(vec.x, vec.y, vec.z, vec);
    }

    public Vector3f transformInverseUnit(Vector3f vec) {
        return this.transformInverseUnit(vec.x, vec.y, vec.z, vec);
    }

    public Vector3f transformPositiveX(Vector3f dest) {
        double ww = this.w * this.w;
        double xx = this.x * this.x;
        double yy = this.y * this.y;
        double zz = this.z * this.z;
        double zw = this.z * this.w;
        double xy = this.x * this.y;
        double xz = this.x * this.z;
        double yw = this.y * this.w;
        dest.x = (float) (ww + xx - zz - yy);
        dest.y = (float) (xy + zw + zw + xy);
        dest.z = (float) (xz - yw + xz - yw);
        return dest;
    }

    public Vector4f transformPositiveX(Vector4f dest) {
        double ww = this.w * this.w;
        double xx = this.x * this.x;
        double yy = this.y * this.y;
        double zz = this.z * this.z;
        double zw = this.z * this.w;
        double xy = this.x * this.y;
        double xz = this.x * this.z;
        double yw = this.y * this.w;
        dest.x = (float) (ww + xx - zz - yy);
        dest.y = (float) (xy + zw + zw + xy);
        dest.z = (float) (xz - yw + xz - yw);
        return dest;
    }

    public Vector3f transformUnitPositiveX(Vector3f dest) {
        double yy = this.y * this.y;
        double zz = this.z * this.z;
        double xy = this.x * this.y;
        double xz = this.x * this.z;
        double yw = this.y * this.w;
        double zw = this.z * this.w;
        dest.x = (float) (1.0 - yy - yy - zz - zz);
        dest.y = (float) (xy + zw + xy + zw);
        dest.z = (float) (xz - yw + xz - yw);
        return dest;
    }

    public Vector4f transformUnitPositiveX(Vector4f dest) {
        double yy = this.y * this.y;
        double zz = this.z * this.z;
        double xy = this.x * this.y;
        double xz = this.x * this.z;
        double yw = this.y * this.w;
        double zw = this.z * this.w;
        dest.x = (float) (1.0 - yy - yy - zz - zz);
        dest.y = (float) (xy + zw + xy + zw);
        dest.z = (float) (xz - yw + xz - yw);
        return dest;
    }

    public Vector3f transformPositiveY(Vector3f dest) {
        double ww = this.w * this.w;
        double xx = this.x * this.x;
        double yy = this.y * this.y;
        double zz = this.z * this.z;
        double zw = this.z * this.w;
        double xy = this.x * this.y;
        double yz = this.y * this.z;
        double xw = this.x * this.w;
        dest.x = (float) (-zw + xy - zw + xy);
        dest.y = (float) (yy - zz + ww - xx);
        dest.z = (float) (yz + yz + xw + xw);
        return dest;
    }

    public Vector4f transformPositiveY(Vector4f dest) {
        double ww = this.w * this.w;
        double xx = this.x * this.x;
        double yy = this.y * this.y;
        double zz = this.z * this.z;
        double zw = this.z * this.w;
        double xy = this.x * this.y;
        double yz = this.y * this.z;
        double xw = this.x * this.w;
        dest.x = (float) (-zw + xy - zw + xy);
        dest.y = (float) (yy - zz + ww - xx);
        dest.z = (float) (yz + yz + xw + xw);
        return dest;
    }

    public Vector4f transformUnitPositiveY(Vector4f dest) {
        double xx = this.x * this.x;
        double zz = this.z * this.z;
        double xy = this.x * this.y;
        double yz = this.y * this.z;
        double xw = this.x * this.w;
        double zw = this.z * this.w;
        dest.x = (float) (xy - zw + xy - zw);
        dest.y = (float) (1.0 - xx - xx - zz - zz);
        dest.z = (float) (yz + yz + xw + xw);
        return dest;
    }

    public Vector3f transformUnitPositiveY(Vector3f dest) {
        double xx = this.x * this.x;
        double zz = this.z * this.z;
        double xy = this.x * this.y;
        double yz = this.y * this.z;
        double xw = this.x * this.w;
        double zw = this.z * this.w;
        dest.x = (float) (xy - zw + xy - zw);
        dest.y = (float) (1.0 - xx - xx - zz - zz);
        dest.z = (float) (yz + yz + xw + xw);
        return dest;
    }

    public Vector3f transformPositiveZ(Vector3f dest) {
        double ww = this.w * this.w;
        double xx = this.x * this.x;
        double yy = this.y * this.y;
        double zz = this.z * this.z;
        double xz = this.x * this.z;
        double yw = this.y * this.w;
        double yz = this.y * this.z;
        double xw = this.x * this.w;
        dest.x = (float) (yw + xz + xz + yw);
        dest.y = (float) (yz + yz - xw - xw);
        dest.z = (float) (zz - yy - xx + ww);
        return dest;
    }

    public Vector4f transformPositiveZ(Vector4f dest) {
        double ww = this.w * this.w;
        double xx = this.x * this.x;
        double yy = this.y * this.y;
        double zz = this.z * this.z;
        double xz = this.x * this.z;
        double yw = this.y * this.w;
        double yz = this.y * this.z;
        double xw = this.x * this.w;
        dest.x = (float) (yw + xz + xz + yw);
        dest.y = (float) (yz + yz - xw - xw);
        dest.z = (float) (zz - yy - xx + ww);
        return dest;
    }

    public Vector4f transformUnitPositiveZ(Vector4f dest) {
        double xx = this.x * this.x;
        double yy = this.y * this.y;
        double xz = this.x * this.z;
        double yz = this.y * this.z;
        double xw = this.x * this.w;
        double yw = this.y * this.w;
        dest.x = (float) (xz + yw + xz + yw);
        dest.y = (float) (yz + yz - xw - xw);
        dest.z = (float) (1.0 - xx - xx - yy - yy);
        return dest;
    }

    public Vector3f transformUnitPositiveZ(Vector3f dest) {
        double xx = this.x * this.x;
        double yy = this.y * this.y;
        double xz = this.x * this.z;
        double yz = this.y * this.z;
        double xw = this.x * this.w;
        double yw = this.y * this.w;
        dest.x = (float) (xz + yw + xz + yw);
        dest.y = (float) (yz + yz - xw - xw);
        dest.z = (float) (1.0 - xx - xx - yy - yy);
        return dest;
    }

    public Vector4f transform(Vector4f vec) {
        return this.transform(vec, vec);
    }

    public Vector4f transformInverse(Vector4f vec) {
        return this.transformInverse(vec, vec);
    }

    public Vector3f transform(Vector3f vec, Vector3f dest) {
        return this.transform(vec.x, vec.y, vec.z, dest);
    }

    public Vector3f transformInverse(Vector3f vec, Vector3f dest) {
        return this.transformInverse(vec.x, vec.y, vec.z, dest);
    }

    public Vector3f transform(double x, double y, double z, Vector3f dest) {
        double xx = this.x * this.x;
        double yy = this.y * this.y;
        double zz = this.z * this.z;
        double ww = this.w * this.w;
        double xy = this.x * this.y;
        double xz = this.x * this.z;
        double yz = this.y * this.z;
        double xw = this.x * this.w;
        double zw = this.z * this.w;
        double yw = this.y * this.w;
        double k = 1.0 / (xx + yy + zz + ww);
        return dest.set(Math.fma((xx - yy - zz + ww) * k, x, Math.fma(2.0 * (xy - zw) * k, y, 2.0 * (xz + yw) * k * z)), Math.fma(2.0 * (xy + zw) * k, x, Math.fma((yy - xx - zz + ww) * k, y, 2.0 * (yz - xw) * k * z)), Math.fma(2.0 * (xz - yw) * k, x, Math.fma(2.0 * (yz + xw) * k, y, (zz - xx - yy + ww) * k * z)));
    }

    public Vector3f transformInverse(double x, double y, double z, Vector3f dest) {
        double n = 1.0 / Math.fma(this.x, this.x, Math.fma(this.y, this.y, Math.fma(this.z, this.z, this.w * this.w)));
        double qx = this.x * n;
        double qy = this.y * n;
        double qz = this.z * n;
        double qw = this.w * n;
        double xx = qx * qx;
        double yy = qy * qy;
        double zz = qz * qz;
        double ww = qw * qw;
        double xy = qx * qy;
        double xz = qx * qz;
        double yz = qy * qz;
        double xw = qx * qw;
        double zw = qz * qw;
        double yw = qy * qw;
        double k = 1.0 / (xx + yy + zz + ww);
        return dest.set(Math.fma((xx - yy - zz + ww) * k, x, Math.fma(2.0 * (xy + zw) * k, y, 2.0 * (xz - yw) * k * z)), Math.fma(2.0 * (xy - zw) * k, x, Math.fma((yy - xx - zz + ww) * k, y, 2.0 * (yz + xw) * k * z)), Math.fma(2.0 * (xz + yw) * k, x, Math.fma(2.0 * (yz - xw) * k, y, (zz - xx - yy + ww) * k * z)));
    }

    public Vector4f transform(Vector4f vec, Vector4f dest) {
        return this.transform(vec.x, vec.y, vec.z, dest);
    }

    public Vector4f transformInverse(Vector4f vec, Vector4f dest) {
        return this.transformInverse(vec.x, vec.y, vec.z, dest);
    }

    public Vector4f transform(double x, double y, double z, Vector4f dest) {
        double xx = this.x * this.x;
        double yy = this.y * this.y;
        double zz = this.z * this.z;
        double ww = this.w * this.w;
        double xy = this.x * this.y;
        double xz = this.x * this.z;
        double yz = this.y * this.z;
        double xw = this.x * this.w;
        double zw = this.z * this.w;
        double yw = this.y * this.w;
        double k = 1.0 / (xx + yy + zz + ww);
        return dest.set((float) Math.fma((xx - yy - zz + ww) * k, x, Math.fma(2.0 * (xy - zw) * k, y, 2.0 * (xz + yw) * k * z)), (float) Math.fma(2.0 * (xy + zw) * k, x, Math.fma((yy - xx - zz + ww) * k, y, 2.0 * (yz - xw) * k * z)), (float) Math.fma(2.0 * (xz - yw) * k, x, Math.fma(2.0 * (yz + xw) * k, y, (zz - xx - yy + ww) * k * z)), dest.w);
    }

    public Vector4f transformInverse(double x, double y, double z, Vector4f dest) {
        double n = 1.0 / Math.fma(this.x, this.x, Math.fma(this.y, this.y, Math.fma(this.z, this.z, this.w * this.w)));
        double qx = this.x * n;
        double qy = this.y * n;
        double qz = this.z * n;
        double qw = this.w * n;
        double xx = qx * qx;
        double yy = qy * qy;
        double zz = qz * qz;
        double ww = qw * qw;
        double xy = qx * qy;
        double xz = qx * qz;
        double yz = qy * qz;
        double xw = qx * qw;
        double zw = qz * qw;
        double yw = qy * qw;
        double k = 1.0 / (xx + yy + zz + ww);
        return dest.set(Math.fma((xx - yy - zz + ww) * k, x, Math.fma(2.0 * (xy + zw) * k, y, 2.0 * (xz - yw) * k * z)), Math.fma(2.0 * (xy - zw) * k, x, Math.fma((yy - xx - zz + ww) * k, y, 2.0 * (yz + xw) * k * z)), Math.fma(2.0 * (xz + yw) * k, x, Math.fma(2.0 * (yz - xw) * k, y, (zz - xx - yy + ww) * k * z)), dest.w);
    }

    public Vector4f transformUnit(Vector4f vec) {
        return this.transformUnit(vec, vec);
    }

    public Vector4f transformInverseUnit(Vector4f vec) {
        return this.transformInverseUnit(vec, vec);
    }

    public Vector3f transformUnit(Vector3f vec, Vector3f dest) {
        return this.transformUnit(vec.x, vec.y, vec.z, dest);
    }

    public Vector3f transformInverseUnit(Vector3f vec, Vector3f dest) {
        return this.transformInverseUnit(vec.x, vec.y, vec.z, dest);
    }

    public Vector3f transformUnit(double x, double y, double z, Vector3f dest) {
        double xx = this.x * this.x;
        double xy = this.x * this.y;
        double xz = this.x * this.z;
        double xw = this.x * this.w;
        double yy = this.y * this.y;
        double yz = this.y * this.z;
        double yw = this.y * this.w;
        double zz = this.z * this.z;
        double zw = this.z * this.w;
        return dest.set((float) Math.fma(Math.fma(-2.0, yy + zz, 1.0), x, Math.fma(2.0 * (xy - zw), y, 2.0 * (xz + yw) * z)), (float) Math.fma(2.0 * (xy + zw), x, Math.fma(Math.fma(-2.0, xx + zz, 1.0), y, 2.0 * (yz - xw) * z)), (float) Math.fma(2.0 * (xz - yw), x, Math.fma(2.0 * (yz + xw), y, Math.fma(-2.0, xx + yy, 1.0) * z)));
    }

    public Vector3f transformInverseUnit(double x, double y, double z, Vector3f dest) {
        double xx = this.x * this.x;
        double xy = this.x * this.y;
        double xz = this.x * this.z;
        double xw = this.x * this.w;
        double yy = this.y * this.y;
        double yz = this.y * this.z;
        double yw = this.y * this.w;
        double zz = this.z * this.z;
        double zw = this.z * this.w;
        return dest.set((float) Math.fma(Math.fma(-2.0, yy + zz, 1.0), x, Math.fma(2.0 * (xy + zw), y, 2.0 * (xz - yw) * z)), (float) Math.fma(2.0 * (xy - zw), x, Math.fma(Math.fma(-2.0, xx + zz, 1.0), y, 2.0 * (yz + xw) * z)), (float) Math.fma(2.0 * (xz + yw), x, Math.fma(2.0 * (yz - xw), y, Math.fma(-2.0, xx + yy, 1.0) * z)));
    }

    public Vector4f transformUnit(Vector4f vec, Vector4f dest) {
        return this.transformUnit(vec.x, vec.y, vec.z, dest);
    }

    public Vector4f transformInverseUnit(Vector4f vec, Vector4f dest) {
        return this.transformInverseUnit(vec.x, vec.y, vec.z, dest);
    }

    public Vector4f transformUnit(double x, double y, double z, Vector4f dest) {
        double xx = this.x * this.x;
        double xy = this.x * this.y;
        double xz = this.x * this.z;
        double xw = this.x * this.w;
        double yy = this.y * this.y;
        double yz = this.y * this.z;
        double yw = this.y * this.w;
        double zz = this.z * this.z;
        double zw = this.z * this.w;
        return dest.set((float) Math.fma(Math.fma(-2.0, yy + zz, 1.0), x, Math.fma(2.0 * (xy - zw), y, 2.0 * (xz + yw) * z)), (float) Math.fma(2.0 * (xy + zw), x, Math.fma(Math.fma(-2.0, xx + zz, 1.0), y, 2.0 * (yz - xw) * z)), (float) Math.fma(2.0 * (xz - yw), x, Math.fma(2.0 * (yz + xw), y, Math.fma(-2.0, xx + yy, 1.0) * z)));
    }

    public Vector4f transformInverseUnit(double x, double y, double z, Vector4f dest) {
        double xx = this.x * this.x;
        double xy = this.x * this.y;
        double xz = this.x * this.z;
        double xw = this.x * this.w;
        double yy = this.y * this.y;
        double yz = this.y * this.z;
        double yw = this.y * this.w;
        double zz = this.z * this.z;
        double zw = this.z * this.w;
        return dest.set((float) Math.fma(Math.fma(-2.0, yy + zz, 1.0), x, Math.fma(2.0 * (xy + zw), y, 2.0 * (xz - yw) * z)), (float) Math.fma(2.0 * (xy - zw), x, Math.fma(Math.fma(-2.0, xx + zz, 1.0), y, 2.0 * (yz + xw) * z)), (float) Math.fma(2.0 * (xz + yw), x, Math.fma(2.0 * (yz - xw), y, Math.fma(-2.0, xx + yy, 1.0) * z)));
    }

    public Quaterniond invert(Quaterniond dest) {
        double invNorm = 1.0 / this.lengthSquared();
        dest.x = -this.x * invNorm;
        dest.y = -this.y * invNorm;
        dest.z = -this.z * invNorm;
        dest.w = this.w * invNorm;
        return dest;
    }

    public Quaterniond invert() {
        return this.invert(this);
    }

    public Quaterniond div(Quaterniond b, Quaterniond dest) {
        double invNorm = 1.0 / Math.fma(b.x, b.x, Math.fma(b.y, b.y, Math.fma(b.z, b.z, b.w * b.w)));
        double x = -b.x * invNorm;
        double y = -b.y * invNorm;
        double z = -b.z * invNorm;
        double w = b.w * invNorm;
        return dest.set(Math.fma(this.w, x, Math.fma(this.x, w, Math.fma(this.y, z, -this.z * y))), Math.fma(this.w, y, Math.fma(-this.x, z, Math.fma(this.y, w, this.z * x))), Math.fma(this.w, z, Math.fma(this.x, y, Math.fma(-this.y, x, this.z * w))), Math.fma(this.w, w, Math.fma(-this.x, x, Math.fma(-this.y, y, -this.z * z))));
    }

    public Quaterniond div(Quaterniond b) {
        return this.div(b, this);
    }

    public Quaterniond conjugate() {
        this.x = -this.x;
        this.y = -this.y;
        this.z = -this.z;
        return this;
    }

    public Quaterniond conjugate(Quaterniond dest) {
        dest.x = -this.x;
        dest.y = -this.y;
        dest.z = -this.z;
        dest.w = this.w;
        return dest;
    }

    public Quaterniond identity() {
        this.x = 0.0;
        this.y = 0.0;
        this.z = 0.0;
        this.w = 1.0;
        return this;
    }

    public double lengthSquared() {
        return Math.fma(this.x, this.x, Math.fma(this.y, this.y, Math.fma(this.z, this.z, this.w * this.w)));
    }

    public Quaterniond rotationXYZ(double angleX, double angleY, double angleZ) {
        double sx = Math.sin(angleX * 0.5);
        double cx = Math.cosFromSin(sx, angleX * 0.5);
        double sy = Math.sin(angleY * 0.5);
        double cy = Math.cosFromSin(sy, angleY * 0.5);
        double sz = Math.sin(angleZ * 0.5);
        double cz = Math.cosFromSin(sz, angleZ * 0.5);
        double cycz = cy * cz;
        double sysz = sy * sz;
        double sycz = sy * cz;
        double cysz = cy * sz;
        this.w = cx * cycz - sx * sysz;
        this.x = sx * cycz + cx * sysz;
        this.y = cx * sycz - sx * cysz;
        this.z = cx * cysz + sx * sycz;
        return this;
    }

    public Quaterniond rotationZYX(double angleZ, double angleY, double angleX) {
        double sx = Math.sin(angleX * 0.5);
        double cx = Math.cosFromSin(sx, angleX * 0.5);
        double sy = Math.sin(angleY * 0.5);
        double cy = Math.cosFromSin(sy, angleY * 0.5);
        double sz = Math.sin(angleZ * 0.5);
        double cz = Math.cosFromSin(sz, angleZ * 0.5);
        double cycz = cy * cz;
        double sysz = sy * sz;
        double sycz = sy * cz;
        double cysz = cy * sz;
        this.w = cx * cycz + sx * sysz;
        this.x = sx * cycz - cx * sysz;
        this.y = cx * sycz + sx * cysz;
        this.z = cx * cysz - sx * sycz;
        return this;
    }

    public Quaterniond rotationYXZ(double angleY, double angleX, double angleZ) {
        double sx = Math.sin(angleX * 0.5);
        double cx = Math.cosFromSin(sx, angleX * 0.5);
        double sy = Math.sin(angleY * 0.5);
        double cy = Math.cosFromSin(sy, angleY * 0.5);
        double sz = Math.sin(angleZ * 0.5);
        double cz = Math.cosFromSin(sz, angleZ * 0.5);
        double x = cy * sx;
        double y = sy * cx;
        double z = sy * sx;
        double w = cy * cx;
        this.x = x * cz + y * sz;
        this.y = y * cz - x * sz;
        this.z = w * sz - z * cz;
        this.w = w * cz + z * sz;
        return this;
    }

    public Quaterniond slerp(Quaterniond target, double alpha) {
        return this.slerp(target, alpha, this);
    }

    public Quaterniond slerp(Quaterniond target, double alpha, Quaterniond dest) {
        double cosom = Math.fma(this.x, target.x, Math.fma(this.y, target.y, Math.fma(this.z, target.z, this.w * target.w)));
        double absCosom = Math.abs(cosom);
        double scale0;
        double scale1;
        if (1.0 - absCosom > 1.0E-6) {
            double sinSqr = 1.0 - absCosom * absCosom;
            double sinom = Math.invsqrt(sinSqr);
            double omega = Math.atan2(sinSqr * sinom, absCosom);
            scale0 = Math.sin((1.0 - alpha) * omega) * sinom;
            scale1 = Math.sin(alpha * omega) * sinom;
        } else {
            scale0 = 1.0 - alpha;
            scale1 = alpha;
        }

        scale1 = cosom >= 0.0 ? scale1 : -scale1;
        dest.x = Math.fma(scale0, this.x, scale1 * target.x);
        dest.y = Math.fma(scale0, this.y, scale1 * target.y);
        dest.z = Math.fma(scale0, this.z, scale1 * target.z);
        dest.w = Math.fma(scale0, this.w, scale1 * target.w);
        return dest;
    }

    public static Quaterniond slerp(Quaterniond[] qs, double[] weights, Quaterniond dest) {
        dest.set(qs[0]);
        double w = weights[0];

        for (int i = 1; i < qs.length; ++i) {
            double w1 = weights[i];
            double rw1 = w1 / (w + w1);
            w += w1;
            dest.slerp(qs[i], rw1);
        }

        return dest;
    }

    public Quaterniond scale(double factor) {
        return this.scale(factor, this);
    }

    public Quaterniond scale(double factor, Quaterniond dest) {
        double sqrt = Math.sqrt(factor);
        dest.x = sqrt * this.x;
        dest.y = sqrt * this.y;
        dest.z = sqrt * this.z;
        dest.w = sqrt * this.w;
        return dest;
    }

    public Quaterniond scaling(double factor) {
        double sqrt = Math.sqrt(factor);
        this.x = 0.0;
        this.y = 0.0;
        this.z = 0.0;
        this.w = sqrt;
        return this;
    }

    public Quaterniond integrate(double dt, double vx, double vy, double vz) {
        return this.integrate(dt, vx, vy, vz, this);
    }

    public Quaterniond integrate(double dt, double vx, double vy, double vz, Quaterniond dest) {
        double thetaX = dt * vx * 0.5;
        double thetaY = dt * vy * 0.5;
        double thetaZ = dt * vz * 0.5;
        double thetaMagSq = thetaX * thetaX + thetaY * thetaY + thetaZ * thetaZ;
        double s;
        double dqW;
        if (thetaMagSq * thetaMagSq / 24.0 < 1.0E-8) {
            dqW = 1.0 - thetaMagSq * 0.5;
            s = 1.0 - thetaMagSq / 6.0;
        } else {
            double thetaMag = Math.sqrt(thetaMagSq);
            double sin = Math.sin(thetaMag);
            s = sin / thetaMag;
            dqW = Math.cosFromSin(sin, thetaMag);
        }

        double dqX = thetaX * s;
        double dqY = thetaY * s;
        double dqZ = thetaZ * s;
        return dest.set(Math.fma(dqW, this.x, Math.fma(dqX, this.w, Math.fma(dqY, this.z, -dqZ * this.y))), Math.fma(dqW, this.y, Math.fma(-dqX, this.z, Math.fma(dqY, this.w, dqZ * this.x))), Math.fma(dqW, this.z, Math.fma(dqX, this.y, Math.fma(-dqY, this.x, dqZ * this.w))), Math.fma(dqW, this.w, Math.fma(-dqX, this.x, Math.fma(-dqY, this.y, -dqZ * this.z))));
    }

    public Quaterniond nlerp(Quaterniond q, double factor) {
        return this.nlerp(q, factor, this);
    }

    public Quaterniond nlerp(Quaterniond q, double factor, Quaterniond dest) {
        double cosom = Math.fma(this.x, q.x, Math.fma(this.y, q.y, Math.fma(this.z, q.z, this.w * q.w)));
        double scale0 = 1.0 - factor;
        double scale1 = cosom >= 0.0 ? factor : -factor;
        dest.x = Math.fma(scale0, this.x, scale1 * q.x);
        dest.y = Math.fma(scale0, this.y, scale1 * q.y);
        dest.z = Math.fma(scale0, this.z, scale1 * q.z);
        dest.w = Math.fma(scale0, this.w, scale1 * q.w);
        double s = Math.invsqrt(Math.fma(dest.x, dest.x, Math.fma(dest.y, dest.y, Math.fma(dest.z, dest.z, dest.w * dest.w))));
        dest.x *= s;
        dest.y *= s;
        dest.z *= s;
        dest.w *= s;
        return dest;
    }

    public static Quaterniond nlerp(Quaterniond[] qs, double[] weights, Quaterniond dest) {
        dest.set(qs[0]);
        double w = weights[0];

        for (int i = 1; i < qs.length; ++i) {
            double w1 = weights[i];
            double rw1 = w1 / (w + w1);
            w += w1;
            dest.nlerp(qs[i], rw1);
        }

        return dest;
    }

    public Quaterniond nlerpIterative(Quaterniond q, double alpha, double dotThreshold, Quaterniond dest) {
        double q1x = this.x;
        double q1y = this.y;
        double q1z = this.z;
        double q1w = this.w;
        double q2x = q.x;
        double q2y = q.y;
        double q2z = q.z;
        double q2w = q.w;
        double dot = Math.fma(q1x, q2x, Math.fma(q1y, q2y, Math.fma(q1z, q2z, q1w * q2w)));
        double absDot = Math.abs(dot);
        if (0.999999 < absDot) {
            return dest.set(this);
        } else {
            double alphaN;
            double scale0;
            double scale1;
            for (alphaN = alpha; absDot < dotThreshold; absDot = Math.abs(dot)) {
                scale0 = 0.5;
                scale1 = dot >= 0.0 ? 0.5 : -0.5;
                float s;
                if (alphaN < 0.5) {
                    q2x = Math.fma(scale0, q2x, scale1 * q1x);
                    q2y = Math.fma(scale0, q2y, scale1 * q1y);
                    q2z = Math.fma(scale0, q2z, scale1 * q1z);
                    q2w = Math.fma(scale0, q2w, scale1 * q1w);
                    s = (float) Math.invsqrt(Math.fma(q2x, q2x, Math.fma(q2y, q2y, Math.fma(q2z, q2z, q2w * q2w))));
                    q2x *= s;
                    q2y *= s;
                    q2z *= s;
                    q2w *= s;
                    alphaN += alphaN;
                } else {
                    q1x = Math.fma(scale0, q1x, scale1 * q2x);
                    q1y = Math.fma(scale0, q1y, scale1 * q2y);
                    q1z = Math.fma(scale0, q1z, scale1 * q2z);
                    q1w = Math.fma(scale0, q1w, scale1 * q2w);
                    s = (float) Math.invsqrt(Math.fma(q1x, q1x, Math.fma(q1y, q1y, Math.fma(q1z, q1z, q1w * q1w))));
                    q1x *= s;
                    q1y *= s;
                    q1z *= s;
                    q1w *= s;
                    alphaN = alphaN + alphaN - 1.0;
                }

                dot = Math.fma(q1x, q2x, Math.fma(q1y, q2y, Math.fma(q1z, q2z, q1w * q2w)));
            }

            scale0 = 1.0 - alphaN;
            scale1 = dot >= 0.0 ? alphaN : -alphaN;
            double resX = Math.fma(scale0, q1x, scale1 * q2x);
            double resY = Math.fma(scale0, q1y, scale1 * q2y);
            double resZ = Math.fma(scale0, q1z, scale1 * q2z);
            double resW = Math.fma(scale0, q1w, scale1 * q2w);
            double s = Math.invsqrt(Math.fma(resX, resX, Math.fma(resY, resY, Math.fma(resZ, resZ, resW * resW))));
            dest.x = resX * s;
            dest.y = resY * s;
            dest.z = resZ * s;
            dest.w = resW * s;
            return dest;
        }
    }

    public Quaterniond nlerpIterative(Quaterniond q, double alpha, double dotThreshold) {
        return this.nlerpIterative(q, alpha, dotThreshold, this);
    }

    public static Quaterniond nlerpIterative(Quaterniond[] qs, double[] weights, double dotThreshold, Quaterniond dest) {
        dest.set(qs[0]);
        double w = weights[0];

        for (int i = 1; i < qs.length; ++i) {
            double w1 = weights[i];
            double rw1 = w1 / (w + w1);
            w += w1;
            dest.nlerpIterative(qs[i], rw1, dotThreshold);
        }

        return dest;
    }

    public Quaterniond lookAlong(Vector3d dir, Vector3d up) {
        return this.lookAlong(dir.x, dir.y, dir.z, up.x, up.y, up.z, this);
    }

    public Quaterniond lookAlong(Vector3d dir, Vector3d up, Quaterniond dest) {
        return this.lookAlong(dir.x, dir.y, dir.z, up.x, up.y, up.z, dest);
    }

    public Quaterniond lookAlong(double dirX, double dirY, double dirZ, double upX, double upY, double upZ) {
        return this.lookAlong(dirX, dirY, dirZ, upX, upY, upZ, this);
    }

    public Quaterniond lookAlong(double dirX, double dirY, double dirZ, double upX, double upY, double upZ, Quaterniond dest) {
        double invDirLength = Math.invsqrt(dirX * dirX + dirY * dirY + dirZ * dirZ);
        double dirnX = -dirX * invDirLength;
        double dirnY = -dirY * invDirLength;
        double dirnZ = -dirZ * invDirLength;
        double leftX = upY * dirnZ - upZ * dirnY;
        double leftY = upZ * dirnX - upX * dirnZ;
        double leftZ = upX * dirnY - upY * dirnX;
        double invLeftLength = Math.invsqrt(leftX * leftX + leftY * leftY + leftZ * leftZ);
        leftX *= invLeftLength;
        leftY *= invLeftLength;
        leftZ *= invLeftLength;
        double upnX = dirnY * leftZ - dirnZ * leftY;
        double upnY = dirnZ * leftX - dirnX * leftZ;
        double upnZ = dirnX * leftY - dirnY * leftX;
        double tr = leftX + upnY + dirnZ;
        double x;
        double y;
        double z;
        double w;
        double t;
        if (tr >= 0.0) {
            t = Math.sqrt(tr + 1.0);
            w = t * 0.5;
            t = 0.5 / t;
            x = (dirnY - upnZ) * t;
            y = (leftZ - dirnX) * t;
            z = (upnX - leftY) * t;
        } else if (leftX > upnY && leftX > dirnZ) {
            t = Math.sqrt(1.0 + leftX - upnY - dirnZ);
            x = t * 0.5;
            t = 0.5 / t;
            y = (leftY + upnX) * t;
            z = (dirnX + leftZ) * t;
            w = (dirnY - upnZ) * t;
        } else if (upnY > dirnZ) {
            t = Math.sqrt(1.0 + upnY - leftX - dirnZ);
            y = t * 0.5;
            t = 0.5 / t;
            x = (leftY + upnX) * t;
            z = (upnZ + dirnY) * t;
            w = (leftZ - dirnX) * t;
        } else {
            t = Math.sqrt(1.0 + dirnZ - leftX - upnY);
            z = t * 0.5;
            t = 0.5 / t;
            x = (dirnX + leftZ) * t;
            y = (upnZ + dirnY) * t;
            w = (upnX - leftY) * t;
        }

        return dest.set(Math.fma(this.w, x, Math.fma(this.x, w, Math.fma(this.y, z, -this.z * y))), Math.fma(this.w, y, Math.fma(-this.x, z, Math.fma(this.y, w, this.z * x))), Math.fma(this.w, z, Math.fma(this.x, y, Math.fma(-this.y, x, this.z * w))), Math.fma(this.w, w, Math.fma(-this.x, x, Math.fma(-this.y, y, -this.z * z))));
    }

    public String toString() {
        return "(" + x + "," + y + "," + z + "," + w + ")";
    }

    public int hashCode() {
        int result = 1;
        long temp = Double.doubleToLongBits(this.w);
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
            Quaterniond other = (Quaterniond) obj;
            if (Double.doubleToLongBits(this.w) != Double.doubleToLongBits(other.w)) {
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

    public Quaterniond difference(Quaterniond other) {
        return this.difference(other, this);
    }

    public Quaterniond difference(Quaterniond other, Quaterniond dest) {
        double invNorm = 1.0 / this.lengthSquared();
        double x = -this.x * invNorm;
        double y = -this.y * invNorm;
        double z = -this.z * invNorm;
        double w = this.w * invNorm;
        dest.set(Math.fma(w, other.x, Math.fma(x, other.w, Math.fma(y, other.z, -z * other.y))), Math.fma(w, other.y, Math.fma(-x, other.z, Math.fma(y, other.w, z * other.x))), Math.fma(w, other.z, Math.fma(x, other.y, Math.fma(-y, other.x, z * other.w))), Math.fma(w, other.w, Math.fma(-x, other.x, Math.fma(-y, other.y, -z * other.z))));
        return dest;
    }

    public Quaterniond rotationTo(double fromDirX, double fromDirY, double fromDirZ, double toDirX, double toDirY, double toDirZ) {
        double fn = Math.invsqrt(Math.fma(fromDirX, fromDirX, Math.fma(fromDirY, fromDirY, fromDirZ * fromDirZ)));
        double tn = Math.invsqrt(Math.fma(toDirX, toDirX, Math.fma(toDirY, toDirY, toDirZ * toDirZ)));
        double fx = fromDirX * fn;
        double fy = fromDirY * fn;
        double fz = fromDirZ * fn;
        double tx = toDirX * tn;
        double ty = toDirY * tn;
        double tz = toDirZ * tn;
        double dot = fx * tx + fy * ty + fz * tz;
        double x;
        double y;
        double z;
        double w;
        if (dot < -0.999999) {
            x = fy;
            y = -fx;
            z = 0.0;
            if (fy * fy + y * y == 0.0) {
                x = 0.0;
                y = fz;
                z = -fy;
            }
            this.x = x;
            this.y = y;
            this.z = z;
            this.w = 0.0;
        } else {
            double sd2 = Math.sqrt((1.0 + dot) * 2.0);
            double isd2 = 1.0 / sd2;
            double cx = fy * tz - fz * ty;
            double cy = fz * tx - fx * tz;
            double cz = fx * ty - fy * tx;
            x = cx * isd2;
            y = cy * isd2;
            z = cz * isd2;
            w = sd2 * 0.5;
            double n2 = Math.invsqrt(Math.fma(x, x, Math.fma(y, y, Math.fma(z, z, w * w))));
            this.x = x * n2;
            this.y = y * n2;
            this.z = z * n2;
            this.w = w * n2;
        }

        return this;
    }

    public Quaterniond rotationTo(Vector3d fromDir, Vector3d toDir) {
        return this.rotationTo(fromDir.x, fromDir.y, fromDir.z, toDir.x, toDir.y, toDir.z);
    }

    public Quaterniond rotateTo(double fromDirX, double fromDirY, double fromDirZ, double toDirX, double toDirY, double toDirZ, Quaterniond dest) {
        double fn = Math.invsqrt(Math.fma(fromDirX, fromDirX, Math.fma(fromDirY, fromDirY, fromDirZ * fromDirZ)));
        double tn = Math.invsqrt(Math.fma(toDirX, toDirX, Math.fma(toDirY, toDirY, toDirZ * toDirZ)));
        double fx = fromDirX * fn;
        double fy = fromDirY * fn;
        double fz = fromDirZ * fn;
        double tx = toDirX * tn;
        double ty = toDirY * tn;
        double tz = toDirZ * tn;
        double dot = fx * tx + fy * ty + fz * tz;
        double x;
        double y;
        double z;
        double w;
        if (dot < -0.999999) {
            x = fy;
            y = -fx;
            z = 0.0;
            w = 0.0;
            if (fy * fy + y * y == 0.0) {
                x = 0.0;
                y = fz;
                z = -fy;
                w = 0.0;
            }
        } else {
            double sd2 = Math.sqrt((1.0 + dot) * 2.0);
            double isd2 = 1.0 / sd2;
            double cx = fy * tz - fz * ty;
            double cy = fz * tx - fx * tz;
            double cz = fx * ty - fy * tx;
            x = cx * isd2;
            y = cy * isd2;
            z = cz * isd2;
            w = sd2 * 0.5;
            double n2 = Math.invsqrt(Math.fma(x, x, Math.fma(y, y, Math.fma(z, z, w * w))));
            x *= n2;
            y *= n2;
            z *= n2;
            w *= n2;
        }

        return dest.set(Math.fma(this.w, x, Math.fma(this.x, w, Math.fma(this.y, z, -this.z * y))), Math.fma(this.w, y, Math.fma(-this.x, z, Math.fma(this.y, w, this.z * x))), Math.fma(this.w, z, Math.fma(this.x, y, Math.fma(-this.y, x, this.z * w))), Math.fma(this.w, w, Math.fma(-this.x, x, Math.fma(-this.y, y, -this.z * z))));
    }

    public Quaterniond rotationAxis(AxisAngle4f axisAngle) {
        return this.rotationAxis(axisAngle.angle, axisAngle.x, axisAngle.y, axisAngle.z);
    }

    public Quaterniond rotationAxis(double angle, double axisX, double axisY, double axisZ) {
        double halfAngle = angle / 2.0;
        double sinAngle = Math.sin(halfAngle);
        double invVLength = Math.invsqrt(axisX * axisX + axisY * axisY + axisZ * axisZ);
        return this.set(axisX * invVLength * sinAngle, axisY * invVLength * sinAngle, axisZ * invVLength * sinAngle, Math.cosFromSin(sinAngle, halfAngle));
    }

    public Quaterniond rotationX(double angle) {
        double sin = Math.sin(angle * 0.5);
        double cos = Math.cosFromSin(sin, angle * 0.5);
        return this.set(sin, 0.0, cos, 0.0);
    }

    public Quaterniond rotationY(double angle) {
        double sin = Math.sin(angle * 0.5);
        double cos = Math.cosFromSin(sin, angle * 0.5);
        return this.set(0.0, sin, 0.0, cos);
    }

    public Quaterniond rotationZ(double angle) {
        double sin = Math.sin(angle * 0.5);
        double cos = Math.cosFromSin(sin, angle * 0.5);
        return this.set(0.0, 0.0, sin, cos);
    }

    public Quaterniond rotateTo(double fromDirX, double fromDirY, double fromDirZ, double toDirX, double toDirY, double toDirZ) {
        return this.rotateTo(fromDirX, fromDirY, fromDirZ, toDirX, toDirY, toDirZ, this);
    }

    public Quaterniond rotateTo(Vector3d fromDir, Vector3d toDir, Quaterniond dest) {
        return this.rotateTo(fromDir.x, fromDir.y, fromDir.z, toDir.x, toDir.y, toDir.z, dest);
    }

    public Quaterniond rotateTo(Vector3d fromDir, Vector3d toDir) {
        return this.rotateTo(fromDir.x, fromDir.y, fromDir.z, toDir.x, toDir.y, toDir.z, this);
    }

    public Quaterniond rotateX(double angle) {
        return this.rotateX(angle, this);
    }

    public Quaterniond rotateX(double angle, Quaterniond dest) {
        double sin = Math.sin(angle * 0.5);
        double cos = Math.cosFromSin(sin, angle * 0.5);
        return dest.set(this.w * sin + this.x * cos, this.y * cos + this.z * sin, this.z * cos - this.y * sin, this.w * cos - this.x * sin);
    }

    public Quaterniond rotateY(double angle) {
        return this.rotateY(angle, this);
    }

    public Quaterniond rotateY(double angle, Quaterniond dest) {
        double sin = Math.sin(angle * 0.5);
        double cos = Math.cosFromSin(sin, angle * 0.5);
        return dest.set(this.x * cos - this.z * sin, this.w * sin + this.y * cos, this.x * sin + this.z * cos, this.w * cos - this.y * sin);
    }

    public Quaterniond rotateZ(double angle) {
        return this.rotateZ(angle, this);
    }

    public Quaterniond rotateZ(double angle, Quaterniond dest) {
        double sin = Math.sin(angle * 0.5);
        double cos = Math.cosFromSin(sin, angle * 0.5);
        return dest.set(this.x * cos + this.y * sin, this.y * cos - this.x * sin, this.w * sin + this.z * cos, this.w * cos - this.z * sin);
    }

    public Quaterniond rotateLocalX(double angle) {
        return this.rotateLocalX(angle, this);
    }

    public Quaterniond rotateLocalX(double angle, Quaterniond dest) {
        double halfAngle = angle * 0.5;
        double s = Math.sin(halfAngle);
        double c = Math.cosFromSin(s, halfAngle);
        dest.set(c * this.x + s * this.w, c * this.y - s * this.z, c * this.z + s * this.y, c * this.w - s * this.x);
        return dest;
    }

    public Quaterniond rotateLocalY(double angle) {
        return this.rotateLocalY(angle, this);
    }

    public Quaterniond rotateLocalY(double angle, Quaterniond dest) {
        double halfAngle = angle * 0.5;
        double s = Math.sin(halfAngle);
        double c = Math.cosFromSin(s, halfAngle);
        dest.set(c * this.x + s * this.z, c * this.y + s * this.w, c * this.z - s * this.x, c * this.w - s * this.y);
        return dest;
    }

    public Quaterniond rotateLocalZ(double angle) {
        return this.rotateLocalZ(angle, this);
    }

    public Quaterniond rotateLocalZ(double angle, Quaterniond dest) {
        double halfAngle = angle * 0.5;
        double s = Math.sin(halfAngle);
        double c = Math.cosFromSin(s, halfAngle);
        dest.set(c * this.x - s * this.y, c * this.y + s * this.x, c * this.z + s * this.w, c * this.w - s * this.z);
        return dest;
    }

    public Quaterniond rotateXYZ(double angleX, double angleY, double angleZ) {
        return this.rotateXYZ(angleX, angleY, angleZ, this);
    }

    public Quaterniond rotateXYZ(double angleX, double angleY, double angleZ, Quaterniond dest) {
        double sx = Math.sin(angleX * 0.5);
        double cx = Math.cosFromSin(sx, angleX * 0.5);
        double sy = Math.sin(angleY * 0.5);
        double cy = Math.cosFromSin(sy, angleY * 0.5);
        double sz = Math.sin(angleZ * 0.5);
        double cz = Math.cosFromSin(sz, angleZ * 0.5);
        double cycz = cy * cz;
        double sysz = sy * sz;
        double sycz = sy * cz;
        double cysz = cy * sz;
        double w = cx * cycz - sx * sysz;
        double x = sx * cycz + cx * sysz;
        double y = cx * sycz - sx * cysz;
        double z = cx * cysz + sx * sycz;
        return dest.set(Math.fma(this.w, x, Math.fma(this.x, w, Math.fma(this.y, z, -this.z * y))), Math.fma(this.w, y, Math.fma(-this.x, z, Math.fma(this.y, w, this.z * x))), Math.fma(this.w, z, Math.fma(this.x, y, Math.fma(-this.y, x, this.z * w))), Math.fma(this.w, w, Math.fma(-this.x, x, Math.fma(-this.y, y, -this.z * z))));
    }

    public Quaterniond rotateZYX(double angleZ, double angleY, double angleX) {
        return this.rotateZYX(angleZ, angleY, angleX, this);
    }

    public Quaterniond rotateZYX(double angleZ, double angleY, double angleX, Quaterniond dest) {
        double sx = Math.sin(angleX * 0.5);
        double cx = Math.cosFromSin(sx, angleX * 0.5);
        double sy = Math.sin(angleY * 0.5);
        double cy = Math.cosFromSin(sy, angleY * 0.5);
        double sz = Math.sin(angleZ * 0.5);
        double cz = Math.cosFromSin(sz, angleZ * 0.5);
        double cycz = cy * cz;
        double sysz = sy * sz;
        double sycz = sy * cz;
        double cysz = cy * sz;
        double w = cx * cycz + sx * sysz;
        double x = sx * cycz - cx * sysz;
        double y = cx * sycz + sx * cysz;
        double z = cx * cysz - sx * sycz;
        return dest.set(Math.fma(this.w, x, Math.fma(this.x, w, Math.fma(this.y, z, -this.z * y))), Math.fma(this.w, y, Math.fma(-this.x, z, Math.fma(this.y, w, this.z * x))), Math.fma(this.w, z, Math.fma(this.x, y, Math.fma(-this.y, x, this.z * w))), Math.fma(this.w, w, Math.fma(-this.x, x, Math.fma(-this.y, y, -this.z * z))));
    }

    public Quaterniond rotateYXZ(double angleY, double angleX, double angleZ) {
        return this.rotateYXZ(angleY, angleX, angleZ, this);
    }

    public Quaterniond rotateYXZ(double angleY, double angleX, double angleZ, Quaterniond dest) {
        double sx = Math.sin(angleX * 0.5);
        double cx = Math.cosFromSin(sx, angleX * 0.5);
        double sy = Math.sin(angleY * 0.5);
        double cy = Math.cosFromSin(sy, angleY * 0.5);
        double sz = Math.sin(angleZ * 0.5);
        double cz = Math.cosFromSin(sz, angleZ * 0.5);
        double yx = cy * sx;
        double yy = sy * cx;
        double yz = sy * sx;
        double yw = cy * cx;
        double x = yx * cz + yy * sz;
        double y = yy * cz - yx * sz;
        double z = yw * sz - yz * cz;
        double w = yw * cz + yz * sz;
        return dest.set(Math.fma(this.w, x, Math.fma(this.x, w, Math.fma(this.y, z, -this.z * y))), Math.fma(this.w, y, Math.fma(-this.x, z, Math.fma(this.y, w, this.z * x))), Math.fma(this.w, z, Math.fma(this.x, y, Math.fma(-this.y, x, this.z * w))), Math.fma(this.w, w, Math.fma(-this.x, x, Math.fma(-this.y, y, -this.z * z))));
    }

    public Vector3d getEulerAnglesXYZ(Vector3d eulerAngles) {
        eulerAngles.x = Math.atan2(this.x * this.w - this.y * this.z, 0.5 - this.x * this.x - this.y * this.y);
        eulerAngles.y = Math.safeAsin(2.0 * (this.x * this.z + this.y * this.w));
        eulerAngles.z = Math.atan2(this.z * this.w - this.x * this.y, 0.5 - this.y * this.y - this.z * this.z);
        return eulerAngles;
    }

    public Vector3d getEulerAnglesZYX(Vector3d eulerAngles) {
        eulerAngles.x = Math.atan2(this.y * this.z + this.w * this.x, 0.5 - this.x * this.x + this.y * this.y);
        eulerAngles.y = Math.safeAsin(-2.0 * (this.x * this.z - this.w * this.y));
        eulerAngles.z = Math.atan2(this.x * this.y + this.w * this.z, 0.5 - this.y * this.y - this.z * this.z);
        return eulerAngles;
    }

    public Vector3d getEulerAnglesZXY(Vector3d eulerAngles) {
        eulerAngles.x = Math.safeAsin(2.0 * (this.w * this.x + this.y * this.z));
        eulerAngles.y = Math.atan2(this.w * this.y - this.x * this.z, 0.5 - this.y * this.y - this.x * this.x);
        eulerAngles.z = Math.atan2(this.w * this.z - this.x * this.y, 0.5 - this.z * this.z - this.x * this.x);
        return eulerAngles;
    }

    public Vector3d getEulerAnglesYXZ(Vector3d eulerAngles) {
        eulerAngles.x = Math.safeAsin(-2.0 * (this.y * this.z - this.w * this.x));
        eulerAngles.y = Math.atan2(this.x * this.z + this.y * this.w, 0.5 - this.y * this.y - this.x * this.x);
        eulerAngles.z = Math.atan2(this.y * this.x + this.w * this.z, 0.5 - this.x * this.x - this.z * this.z);
        return eulerAngles;
    }

    public Quaterniond rotateAxis(double angle, double axisX, double axisY, double axisZ, Quaterniond dest) {
        double halfAngle = angle / 2.0;
        double sinAngle = Math.sin(halfAngle);
        double invVLength = Math.invsqrt(Math.fma(axisX, axisX, Math.fma(axisY, axisY, axisZ * axisZ)));
        double rx = axisX * invVLength * sinAngle;
        double ry = axisY * invVLength * sinAngle;
        double rz = axisZ * invVLength * sinAngle;
        double rw = Math.cosFromSin(sinAngle, halfAngle);
        return dest.set(Math.fma(this.w, rx, Math.fma(this.x, rw, Math.fma(this.y, rz, -this.z * ry))), Math.fma(this.w, ry, Math.fma(-this.x, rz, Math.fma(this.y, rw, this.z * rx))), Math.fma(this.w, rz, Math.fma(this.x, ry, Math.fma(-this.y, rx, this.z * rw))), Math.fma(this.w, rw, Math.fma(-this.x, rx, Math.fma(-this.y, ry, -this.z * rz))));
    }

    public Quaterniond rotateAxis(double angle, Vector3d axis, Quaterniond dest) {
        return this.rotateAxis(angle, axis.x, axis.y, axis.z, dest);
    }

    public Quaterniond rotateAxis(double angle, Vector3d axis) {
        return this.rotateAxis(angle, axis.x, axis.y, axis.z, this);
    }

    public Quaterniond rotateAxis(double angle, double axisX, double axisY, double axisZ) {
        return this.rotateAxis(angle, axisX, axisY, axisZ, this);
    }

    public Vector3d positiveX(Vector3d dir) {
        double invNorm = 1.0 / this.lengthSquared();
        double nx = -this.x * invNorm;
        double ny = -this.y * invNorm;
        double nz = -this.z * invNorm;
        double nw = this.w * invNorm;
        double dy = ny + ny;
        double dz = nz + nz;
        dir.x = -ny * dy - nz * dz + 1.0;
        dir.y = nx * dy + nw * dz;
        dir.z = nx * dz - nw * dy;
        return dir;
    }

    public Vector3d normalizedPositiveX(Vector3d dir) {
        double dy = this.y + this.y;
        double dz = this.z + this.z;
        dir.x = -this.y * dy - this.z * dz + 1.0;
        dir.y = this.x * dy - this.w * dz;
        dir.z = this.x * dz + this.w * dy;
        return dir;
    }

    public Vector3d positiveY(Vector3d dir) {
        double invNorm = 1.0 / this.lengthSquared();
        double nx = -this.x * invNorm;
        double ny = -this.y * invNorm;
        double nz = -this.z * invNorm;
        double nw = this.w * invNorm;
        double dx = nx + nx;
        double dy = ny + ny;
        double dz = nz + nz;
        dir.x = nx * dy - nw * dz;
        dir.y = -nx * dx - nz * dz + 1.0;
        dir.z = ny * dz + nw * dx;
        return dir;
    }

    public Vector3d normalizedPositiveY(Vector3d dir) {
        double dx = this.x + this.x;
        double dy = this.y + this.y;
        double dz = this.z + this.z;
        dir.x = this.x * dy + this.w * dz;
        dir.y = -this.x * dx - this.z * dz + 1.0;
        dir.z = this.y * dz - this.w * dx;
        return dir;
    }

    public Vector3d positiveZ(Vector3d dir) {
        double invNorm = 1.0 / this.lengthSquared();
        double nx = -this.x * invNorm;
        double ny = -this.y * invNorm;
        double nz = -this.z * invNorm;
        double nw = this.w * invNorm;
        double dx = nx + nx;
        double dy = ny + ny;
        double dz = nz + nz;
        dir.x = nx * dz + nw * dy;
        dir.y = ny * dz - nw * dx;
        dir.z = -nx * dx - ny * dy + 1.0;
        return dir;
    }

    public Vector3d normalizedPositiveZ(Vector3d dir) {
        double dx = this.x + this.x;
        double dy = this.y + this.y;
        double dz = this.z + this.z;
        dir.x = this.x * dz - this.w * dy;
        dir.y = this.y * dz + this.w * dx;
        dir.z = -this.x * dx - this.y * dy + 1.0;
        return dir;
    }

    public Quaterniond conjugateBy(Quaterniond q) {
        return this.conjugateBy(q, this);
    }

    public Quaterniond conjugateBy(Quaterniond q, Quaterniond dest) {
        double invNorm = 1.0 / q.lengthSquared();
        double qix = -q.x * invNorm;
        double qiy = -q.y * invNorm;
        double qiz = -q.z * invNorm;
        double qiw = q.w * invNorm;
        double qpx = Math.fma(q.w, this.x, Math.fma(q.x, this.w, Math.fma(q.y, this.z, -q.z * this.y)));
        double qpy = Math.fma(q.w, this.y, Math.fma(-q.x, this.z, Math.fma(q.y, this.w, q.z * this.x)));
        double qpz = Math.fma(q.w, this.z, Math.fma(q.x, this.y, Math.fma(-q.y, this.x, q.z * this.w)));
        double qpw = Math.fma(q.w, this.w, Math.fma(-q.x, this.x, Math.fma(-q.y, this.y, -q.z * this.z)));
        return dest.set(Math.fma(qpw, qix, Math.fma(qpx, qiw, Math.fma(qpy, qiz, -qpz * qiy))), Math.fma(qpw, qiy, Math.fma(-qpx, qiz, Math.fma(qpy, qiw, qpz * qix))), Math.fma(qpw, qiz, Math.fma(qpx, qiy, Math.fma(-qpy, qix, qpz * qiw))), Math.fma(qpw, qiw, Math.fma(-qpx, qix, Math.fma(-qpy, qiy, -qpz * qiz))));
    }

    public boolean isFinite() {
        return Math.isFinite(this.x) && Math.isFinite(this.y) && Math.isFinite(this.z) && Math.isFinite(this.w);
    }

    public boolean equals(Quaterniond q, double delta) {
        if (this == q) {
            return true;
        } else if (q == null) {
            return false;
        } else if (!Runtime.equals(this.x, q.x, delta)) {
            return false;
        } else if (!Runtime.equals(this.y, q.y, delta)) {
            return false;
        } else if (!Runtime.equals(this.z, q.z, delta)) {
            return false;
        } else {
            return Runtime.equals(this.w, q.w, delta);
        }
    }

    public boolean equals(double x, double y, double z, double w) {
        if (Double.doubleToLongBits(this.x) != Double.doubleToLongBits(x)) {
            return false;
        } else if (Double.doubleToLongBits(this.y) != Double.doubleToLongBits(y)) {
            return false;
        } else if (Double.doubleToLongBits(this.z) != Double.doubleToLongBits(z)) {
            return false;
        } else {
            return Double.doubleToLongBits(this.w) == Double.doubleToLongBits(w);
        }
    }

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
