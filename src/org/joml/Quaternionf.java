package org.joml;

@SuppressWarnings({"unused", "UnusedReturnValue"})
public class Quaternionf {
    
    public float x;
    public float y;
    public float z;
    public float w;

    public Quaternionf() {
        this.w = 1f;
    }

    public Quaternionf(double x, double y, double z, double w) {
        this.x = (float) x;
        this.y = (float) y;
        this.z = (float) z;
        this.w = (float) w;
    }

    public Quaternionf(float x, float y, float z, float w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
    }

    public Quaternionf(Quaternionf source) {
        this.set(source);
    }

    public Quaternionf(Quaterniond source) {
        this.set(source);
    }

    public Quaternionf(AxisAngle4f axisAngle) {
        float sin = Math.sin(axisAngle.angle * 0.5F);
        float cos = Math.cosFromSin(sin, axisAngle.angle * 0.5F);
        this.x = axisAngle.x * sin;
        this.y = axisAngle.y * sin;
        this.z = axisAngle.z * sin;
        this.w = cos;
    }

    public Quaternionf(AxisAngle4d axisAngle) {
        double sin = Math.sin(axisAngle.angle * 0.5);
        double cos = Math.cosFromSin(sin, axisAngle.angle * 0.5);
        this.x = (float) (axisAngle.x * sin);
        this.y = (float) (axisAngle.y * sin);
        this.z = (float) (axisAngle.z * sin);
        this.w = (float) cos;
    }

    public float x() {
        return this.x;
    }

    public float y() {
        return this.y;
    }

    public float z() {
        return this.z;
    }

    public float w() {
        return this.w;
    }

    public Quaternionf normalize() {
        return this.normalize(this);
    }

    public Quaternionf normalize(Quaternionf dest) {
        float invNorm = Math.invsqrt(Math.fma(this.x, this.x, Math.fma(this.y, this.y, Math.fma(this.z, this.z, this.w * this.w))));
        dest.x = this.x * invNorm;
        dest.y = this.y * invNorm;
        dest.z = this.z * invNorm;
        dest.w = this.w * invNorm;
        return dest;
    }

    public Quaternionf add(float x, float y, float z, float w) {
        return this.add(x, y, z, w, this);
    }

    public Quaternionf add(float x, float y, float z, float w, Quaternionf dest) {
        dest.x = this.x + x;
        dest.y = this.y + y;
        dest.z = this.z + z;
        dest.w = this.w + w;
        return dest;
    }

    public Quaternionf add(Quaternionf q2) {
        return this.add(q2, this);
    }

    public Quaternionf add(Quaternionf q2, Quaternionf dest) {
        dest.x = this.x + q2.x;
        dest.y = this.y + q2.y;
        dest.z = this.z + q2.z;
        dest.w = this.w + q2.w();
        return dest;
    }

    public float dot(Quaternionf otherQuat) {
        return this.x * otherQuat.x + this.y * otherQuat.y + this.z * otherQuat.z + this.w * otherQuat.w;
    }

    public float angle() {
        return (float) (2.0 * (double) Math.safeAcos(this.w));
    }

    public Matrix3f get(Matrix3f dest) {
        return dest.set(this);
    }

    public Matrix3d get(Matrix3d dest) {
        return dest.set(this);
    }

    public Matrix4f get(Matrix4f dest) {
        return dest.set(this);
    }

    public Matrix4d get(Matrix4d dest) {
        return dest.set(this);
    }

    public Matrix4x3f get(Matrix4x3f dest) {
        return dest.set(this);
    }

    public Matrix4x3d get(Matrix4x3d dest) {
        return dest.set(this);
    }

    public AxisAngle4f get(AxisAngle4f dest) {
        float x = this.x;
        float y = this.y;
        float z = this.z;
        float w = this.w;
        float s;
        if (w > 1f) {
            s = Math.invsqrt(Math.fma(x, x, Math.fma(y, y, Math.fma(z, z, w * w))));
            x *= s;
            y *= s;
            z *= s;
            w *= s;
        }

        dest.angle = 2f * Math.acos(w);
        s = Math.sqrt(1f - w * w);
        if (s < 0.001F) {
            dest.x = x;
            dest.y = y;
            dest.z = z;
        } else {
            s = 1f / s;
            dest.x = x * s;
            dest.y = y * s;
            dest.z = z * s;
        }

        return dest;
    }

    public AxisAngle4d get(AxisAngle4d dest) {
        float x = this.x;
        float y = this.y;
        float z = this.z;
        float w = this.w;
        float s;
        if (w > 1f) {
            s = Math.invsqrt(Math.fma(x, x, Math.fma(y, y, Math.fma(z, z, w * w))));
            x *= s;
            y *= s;
            z *= s;
            w *= s;
        }

        dest.angle = 2f * Math.acos(w);
        s = Math.sqrt(1f - w * w);
        if (s < 0.001F) {
            dest.x = x;
            dest.y = y;
            dest.z = z;
        } else {
            s = 1f / s;
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

    public Quaternionf set(float x, float y, float z, float w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
        return this;
    }

    public Quaternionf set(Quaternionf q) {
        this.x = q.x;
        this.y = q.y;
        this.z = q.z;
        this.w = q.w();
        return this;
    }

    public Quaternionf set(Quaterniond q) {
        this.x = (float) q.x;
        this.y = (float) q.y;
        this.z = (float) q.z;
        this.w = (float) q.w;
        return this;
    }

    public Quaternionf set(AxisAngle4f axisAngle) {
        return this.setAngleAxis(axisAngle.angle, axisAngle.x, axisAngle.y, axisAngle.z);
    }

    public Quaternionf set(AxisAngle4d axisAngle) {
        return this.setAngleAxis(axisAngle.angle, axisAngle.x, axisAngle.y, axisAngle.z);
    }

    public Quaternionf setAngleAxis(float angle, float x, float y, float z) {
        float s = Math.sin(angle * 0.5F);
        this.x = x * s;
        this.y = y * s;
        this.z = z * s;
        this.w = Math.cosFromSin(s, angle * 0.5F);
        return this;
    }

    public Quaternionf setAngleAxis(double angle, double x, double y, double z) {
        double s = Math.sin(angle * 0.5);
        this.x = (float) (x * s);
        this.y = (float) (y * s);
        this.z = (float) (z * s);
        this.w = (float) Math.cosFromSin(s, angle * 0.5);
        return this;
    }

    public Quaternionf rotationAxis(AxisAngle4f axisAngle) {
        return this.rotationAxis(axisAngle.angle, axisAngle.x, axisAngle.y, axisAngle.z);
    }

    public Quaternionf rotationAxis(float angle, float axisX, float axisY, float axisZ) {
        float halfAngle = angle / 2f;
        float sinAngle = Math.sin(halfAngle);
        float invVLength = Math.invsqrt(axisX * axisX + axisY * axisY + axisZ * axisZ);
        return this.set(axisX * invVLength * sinAngle, axisY * invVLength * sinAngle, axisZ * invVLength * sinAngle, Math.cosFromSin(sinAngle, halfAngle));
    }

    public Quaternionf rotationAxis(float angle, Vector3f axis) {
        return this.rotationAxis(angle, axis.x, axis.y, axis.z);
    }

    public Quaternionf rotationX(float angle) {
        float sin = Math.sin(angle * 0.5F);
        float cos = Math.cosFromSin(sin, angle * 0.5F);
        return this.set(sin, 0f, 0f, cos);
    }

    public Quaternionf rotationY(float angle) {
        float sin = Math.sin(angle * 0.5F);
        float cos = Math.cosFromSin(sin, angle * 0.5F);
        return this.set(0f, sin, 0f, cos);
    }

    public Quaternionf rotationZ(float angle) {
        float sin = Math.sin(angle * 0.5F);
        float cos = Math.cosFromSin(sin, angle * 0.5F);
        return this.set(0f, 0f, sin, cos);
    }

    private void setFromUnnormalized(float m00, float m01, float m02, float m10, float m11, float m12, float m20, float m21, float m22) {
        float lenX = Math.invsqrt(m00 * m00 + m01 * m01 + m02 * m02);
        float lenY = Math.invsqrt(m10 * m10 + m11 * m11 + m12 * m12);
        float lenZ = Math.invsqrt(m20 * m20 + m21 * m21 + m22 * m22);
        float nm00 = m00 * lenX;
        float nm01 = m01 * lenX;
        float nm02 = m02 * lenX;
        float nm10 = m10 * lenY;
        float nm11 = m11 * lenY;
        float nm12 = m12 * lenY;
        float nm20 = m20 * lenZ;
        float nm21 = m21 * lenZ;
        float nm22 = m22 * lenZ;
        this.setFromNormalized(nm00, nm01, nm02, nm10, nm11, nm12, nm20, nm21, nm22);
    }

    private void setFromNormalized(float m00, float m01, float m02, float m10, float m11, float m12, float m20, float m21, float m22) {
        float tr = m00 + m11 + m22;
        float t;
        if (tr >= 0f) {
            t = Math.sqrt(tr + 1f);
            this.w = t * 0.5F;
            t = 0.5F / t;
            this.x = (m12 - m21) * t;
            this.y = (m20 - m02) * t;
            this.z = (m01 - m10) * t;
        } else if (m00 >= m11 && m00 >= m22) {
            t = Math.sqrt(m00 - (m11 + m22) + 1f);
            this.x = t * 0.5F;
            t = 0.5F / t;
            this.y = (m10 + m01) * t;
            this.z = (m02 + m20) * t;
            this.w = (m12 - m21) * t;
        } else if (m11 > m22) {
            t = Math.sqrt(m11 - (m22 + m00) + 1f);
            this.y = t * 0.5F;
            t = 0.5F / t;
            this.z = (m21 + m12) * t;
            this.x = (m10 + m01) * t;
            this.w = (m20 - m02) * t;
        } else {
            t = Math.sqrt(m22 - (m00 + m11) + 1f);
            this.z = t * 0.5F;
            t = 0.5F / t;
            this.x = (m02 + m20) * t;
            this.y = (m21 + m12) * t;
            this.w = (m01 - m10) * t;
        }

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
            this.w = (float) (t * 0.5);
            t = 0.5 / t;
            this.x = (float) ((m12 - m21) * t);
            this.y = (float) ((m20 - m02) * t);
            this.z = (float) ((m01 - m10) * t);
        } else if (m00 >= m11 && m00 >= m22) {
            t = Math.sqrt(m00 - (m11 + m22) + 1.0);
            this.x = (float) (t * 0.5);
            t = 0.5 / t;
            this.y = (float) ((m10 + m01) * t);
            this.z = (float) ((m02 + m20) * t);
            this.w = (float) ((m12 - m21) * t);
        } else if (m11 > m22) {
            t = Math.sqrt(m11 - (m22 + m00) + 1.0);
            this.y = (float) (t * 0.5);
            t = 0.5 / t;
            this.z = (float) ((m21 + m12) * t);
            this.x = (float) ((m10 + m01) * t);
            this.w = (float) ((m20 - m02) * t);
        } else {
            t = Math.sqrt(m22 - (m00 + m11) + 1.0);
            this.z = (float) (t * 0.5);
            t = 0.5 / t;
            this.x = (float) ((m02 + m20) * t);
            this.y = (float) ((m21 + m12) * t);
            this.w = (float) ((m01 - m10) * t);
        }

    }

    public Quaternionf setFromUnnormalized(Matrix4f mat) {
        this.setFromUnnormalized(mat.m00, mat.m01, mat.m02, mat.m10, mat.m11, mat.m12, mat.m20, mat.m21, mat.m22);
        return this;
    }

    public Quaternionf setFromUnnormalized(Matrix4x3f mat) {
        this.setFromUnnormalized(mat.m00, mat.m01, mat.m02, mat.m10, mat.m11, mat.m12, mat.m20, mat.m21, mat.m22);
        return this;
    }

    public Quaternionf setFromUnnormalized(Matrix4x3d mat) {
        this.setFromUnnormalized(mat.m00, mat.m01, mat.m02, mat.m10, mat.m11, mat.m12, mat.m20, mat.m21, mat.m22);
        return this;
    }

    public Quaternionf setFromNormalized(Matrix4f mat) {
        this.setFromNormalized(mat.m00, mat.m01, mat.m02, mat.m10, mat.m11, mat.m12, mat.m20, mat.m21, mat.m22);
        return this;
    }

    public Quaternionf setFromNormalized(Matrix4x3f mat) {
        this.setFromNormalized(mat.m00, mat.m01, mat.m02, mat.m10, mat.m11, mat.m12, mat.m20, mat.m21, mat.m22);
        return this;
    }

    public Quaternionf setFromNormalized(Matrix4x3d mat) {
        this.setFromNormalized(mat.m00, mat.m01, mat.m02, mat.m10, mat.m11, mat.m12, mat.m20, mat.m21, mat.m22);
        return this;
    }

    public Quaternionf setFromUnnormalized(Matrix4d mat) {
        this.setFromUnnormalized(mat.m00, mat.m01, mat.m02, mat.m10, mat.m11, mat.m12, mat.m20, mat.m21, mat.m22);
        return this;
    }

    public Quaternionf setFromNormalized(Matrix4d mat) {
        this.setFromNormalized(mat.m00, mat.m01, mat.m02, mat.m10, mat.m11, mat.m12, mat.m20, mat.m21, mat.m22);
        return this;
    }

    public Quaternionf setFromUnnormalized(Matrix3f mat) {
        this.setFromUnnormalized(mat.m00, mat.m01, mat.m02, mat.m10, mat.m11, mat.m12, mat.m20, mat.m21, mat.m22);
        return this;
    }

    public Quaternionf setFromNormalized(Matrix3f mat) {
        this.setFromNormalized(mat.m00, mat.m01, mat.m02, mat.m10, mat.m11, mat.m12, mat.m20, mat.m21, mat.m22);
        return this;
    }

    public Quaternionf setFromUnnormalized(Matrix3d mat) {
        this.setFromUnnormalized(mat.m00, mat.m01, mat.m02, mat.m10, mat.m11, mat.m12, mat.m20, mat.m21, mat.m22);
        return this;
    }

    public Quaternionf setFromNormalized(Matrix3d mat) {
        this.setFromNormalized(mat.m00, mat.m01, mat.m02, mat.m10, mat.m11, mat.m12, mat.m20, mat.m21, mat.m22);
        return this;
    }

    public Quaternionf fromAxisAngleRad(Vector3f axis, float angle) {
        return this.fromAxisAngleRad(axis.x, axis.y, axis.z, angle);
    }

    public Quaternionf fromAxisAngleRad(float axisX, float axisY, float axisZ, float angle) {
        float halfAngle = angle / 2f;
        float sinAngle = Math.sin(halfAngle);
        float vLength = Math.sqrt(axisX * axisX + axisY * axisY + axisZ * axisZ);
        this.x = axisX / vLength * sinAngle;
        this.y = axisY / vLength * sinAngle;
        this.z = axisZ / vLength * sinAngle;
        this.w = Math.cosFromSin(sinAngle, halfAngle);
        return this;
    }

    public Quaternionf fromAxisAngleDeg(Vector3f axis, float angle) {
        return this.fromAxisAngleRad(axis.x, axis.y, axis.z, Math.toRadians(angle));
    }

    public Quaternionf fromAxisAngleDeg(float axisX, float axisY, float axisZ, float angle) {
        return this.fromAxisAngleRad(axisX, axisY, axisZ, Math.toRadians(angle));
    }

    public Quaternionf mul(Quaternionf q) {
        return this.mul(q, this);
    }

    public Quaternionf mul(Quaternionf q, Quaternionf dest) {
        return dest.set(Math.fma(this.w, q.x, Math.fma(this.x, q.w(), Math.fma(this.y, q.z, -this.z * q.y))), Math.fma(this.w, q.y, Math.fma(-this.x, q.z, Math.fma(this.y, q.w(), this.z * q.x))), Math.fma(this.w, q.z, Math.fma(this.x, q.y, Math.fma(-this.y, q.x, this.z * q.w()))), Math.fma(this.w, q.w(), Math.fma(-this.x, q.x, Math.fma(-this.y, q.y, -this.z * q.z))));
    }

    public Quaternionf mul(float qx, float qy, float qz, float qw) {
        return this.mul(qx, qy, qz, qw, this);
    }

    public Quaternionf mul(float qx, float qy, float qz, float qw, Quaternionf dest) {
        return dest.set(Math.fma(this.w, qx, Math.fma(this.x, qw, Math.fma(this.y, qz, -this.z * qy))), Math.fma(this.w, qy, Math.fma(-this.x, qz, Math.fma(this.y, qw, this.z * qx))), Math.fma(this.w, qz, Math.fma(this.x, qy, Math.fma(-this.y, qx, this.z * qw))), Math.fma(this.w, qw, Math.fma(-this.x, qx, Math.fma(-this.y, qy, -this.z * qz))));
    }

    public Quaternionf premul(Quaternionf q) {
        return this.premul(q, this);
    }

    public Quaternionf premul(Quaternionf q, Quaternionf dest) {
        return dest.set(Math.fma(q.w(), this.x, Math.fma(q.x, this.w, Math.fma(q.y, this.z, -q.z * this.y))), Math.fma(q.w(), this.y, Math.fma(-q.x, this.z, Math.fma(q.y, this.w, q.z * this.x))), Math.fma(q.w(), this.z, Math.fma(q.x, this.y, Math.fma(-q.y, this.x, q.z * this.w))), Math.fma(q.w(), this.w, Math.fma(-q.x, this.x, Math.fma(-q.y, this.y, -q.z * this.z))));
    }

    public Quaternionf premul(float qx, float qy, float qz, float qw) {
        return this.premul(qx, qy, qz, qw, this);
    }

    public Quaternionf premul(float qx, float qy, float qz, float qw, Quaternionf dest) {
        return dest.set(Math.fma(qw, this.x, Math.fma(qx, this.w, Math.fma(qy, this.z, -qz * this.y))), Math.fma(qw, this.y, Math.fma(-qx, this.z, Math.fma(qy, this.w, qz * this.x))), Math.fma(qw, this.z, Math.fma(qx, this.y, Math.fma(-qy, this.x, qz * this.w))), Math.fma(qw, this.w, Math.fma(-qx, this.x, Math.fma(-qy, this.y, -qz * this.z))));
    }

    public Vector3f transform(Vector3f vec) {
        return this.transform(vec.x, vec.y, vec.z, vec);
    }

    public Vector3f transformInverse(Vector3f vec) {
        return this.transformInverse(vec.x, vec.y, vec.z, vec);
    }

    public Vector3f transformPositiveX(Vector3f dest) {
        float ww = this.w * this.w;
        float xx = this.x * this.x;
        float yy = this.y * this.y;
        float zz = this.z * this.z;
        float zw = this.z * this.w;
        float xy = this.x * this.y;
        float xz = this.x * this.z;
        float yw = this.y * this.w;
        dest.x = ww + xx - zz - yy;
        dest.y = xy + zw + zw + xy;
        dest.z = xz - yw + xz - yw;
        return dest;
    }

    public Vector4f transformPositiveX(Vector4f dest) {
        float ww = this.w * this.w;
        float xx = this.x * this.x;
        float yy = this.y * this.y;
        float zz = this.z * this.z;
        float zw = this.z * this.w;
        float xy = this.x * this.y;
        float xz = this.x * this.z;
        float yw = this.y * this.w;
        dest.x = ww + xx - zz - yy;
        dest.y = xy + zw + zw + xy;
        dest.z = xz - yw + xz - yw;
        return dest;
    }

    public Vector3f transformUnitPositiveX(Vector3f dest) {
        float xy = this.x * this.y;
        float xz = this.x * this.z;
        float yy = this.y * this.y;
        float yw = this.y * this.w;
        float zz = this.z * this.z;
        float zw = this.z * this.w;
        dest.x = 1f - yy - zz - yy - zz;
        dest.y = xy + zw + xy + zw;
        dest.z = xz - yw + xz - yw;
        return dest;
    }

    public Vector4f transformUnitPositiveX(Vector4f dest) {
        float yy = this.y * this.y;
        float zz = this.z * this.z;
        float xy = this.x * this.y;
        float xz = this.x * this.z;
        float yw = this.y * this.w;
        float zw = this.z * this.w;
        dest.x = 1f - yy - yy - zz - zz;
        dest.y = xy + zw + xy + zw;
        dest.z = xz - yw + xz - yw;
        return dest;
    }

    public Vector3f transformPositiveY(Vector3f dest) {
        float ww = this.w * this.w;
        float xx = this.x * this.x;
        float yy = this.y * this.y;
        float zz = this.z * this.z;
        float zw = this.z * this.w;
        float xy = this.x * this.y;
        float yz = this.y * this.z;
        float xw = this.x * this.w;
        dest.x = -zw + xy - zw + xy;
        dest.y = yy - zz + ww - xx;
        dest.z = yz + yz + xw + xw;
        return dest;
    }

    public Vector4f transformPositiveY(Vector4f dest) {
        float ww = this.w * this.w;
        float xx = this.x * this.x;
        float yy = this.y * this.y;
        float zz = this.z * this.z;
        float zw = this.z * this.w;
        float xy = this.x * this.y;
        float yz = this.y * this.z;
        float xw = this.x * this.w;
        dest.x = -zw + xy - zw + xy;
        dest.y = yy - zz + ww - xx;
        dest.z = yz + yz + xw + xw;
        return dest;
    }

    public Vector4f transformUnitPositiveY(Vector4f dest) {
        float xx = this.x * this.x;
        float zz = this.z * this.z;
        float xy = this.x * this.y;
        float yz = this.y * this.z;
        float xw = this.x * this.w;
        float zw = this.z * this.w;
        dest.x = xy - zw + xy - zw;
        dest.y = 1f - xx - xx - zz - zz;
        dest.z = yz + yz + xw + xw;
        return dest;
    }

    public Vector3f transformUnitPositiveY(Vector3f dest) {
        float xx = this.x * this.x;
        float zz = this.z * this.z;
        float xy = this.x * this.y;
        float yz = this.y * this.z;
        float xw = this.x * this.w;
        float zw = this.z * this.w;
        dest.x = xy - zw + xy - zw;
        dest.y = 1f - xx - xx - zz - zz;
        dest.z = yz + yz + xw + xw;
        return dest;
    }

    public Vector3f transformPositiveZ(Vector3f dest) {
        float ww = this.w * this.w;
        float xx = this.x * this.x;
        float yy = this.y * this.y;
        float zz = this.z * this.z;
        float xz = this.x * this.z;
        float yw = this.y * this.w;
        float yz = this.y * this.z;
        float xw = this.x * this.w;
        dest.x = yw + xz + xz + yw;
        dest.y = yz + yz - xw - xw;
        dest.z = zz - yy - xx + ww;
        return dest;
    }

    public Vector4f transformPositiveZ(Vector4f dest) {
        float ww = this.w * this.w;
        float xx = this.x * this.x;
        float yy = this.y * this.y;
        float zz = this.z * this.z;
        float xz = this.x * this.z;
        float yw = this.y * this.w;
        float yz = this.y * this.z;
        float xw = this.x * this.w;
        dest.x = yw + xz + xz + yw;
        dest.y = yz + yz - xw - xw;
        dest.z = zz - yy - xx + ww;
        return dest;
    }

    public Vector4f transformUnitPositiveZ(Vector4f dest) {
        float xx = this.x * this.x;
        float yy = this.y * this.y;
        float xz = this.x * this.z;
        float yz = this.y * this.z;
        float xw = this.x * this.w;
        float yw = this.y * this.w;
        dest.x = xz + yw + xz + yw;
        dest.y = yz + yz - xw - xw;
        dest.z = 1f - xx - xx - yy - yy;
        return dest;
    }

    public Vector3f transformUnitPositiveZ(Vector3f dest) {
        float xx = this.x * this.x;
        float yy = this.y * this.y;
        float xz = this.x * this.z;
        float yz = this.y * this.z;
        float xw = this.x * this.w;
        float yw = this.y * this.w;
        dest.x = xz + yw + xz + yw;
        dest.y = yz + yz - xw - xw;
        dest.z = 1f - xx - xx - yy - yy;
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

    public Vector3f transform(float x, float y, float z, Vector3f dest) {
        float xx = this.x * this.x;
        float yy = this.y * this.y;
        float zz = this.z * this.z;
        float ww = this.w * this.w;
        float xy = this.x * this.y;
        float xz = this.x * this.z;
        float yz = this.y * this.z;
        float xw = this.x * this.w;
        float zw = this.z * this.w;
        float yw = this.y * this.w;
        float k = 1f / (xx + yy + zz + ww);
        return dest.set(Math.fma((xx - yy - zz + ww) * k, x, Math.fma(2f * (xy - zw) * k, y, 2f * (xz + yw) * k * z)), Math.fma(2f * (xy + zw) * k, x, Math.fma((yy - xx - zz + ww) * k, y, 2f * (yz - xw) * k * z)), Math.fma(2f * (xz - yw) * k, x, Math.fma(2f * (yz + xw) * k, y, (zz - xx - yy + ww) * k * z)));
    }

    public Vector3f transformInverse(float x, float y, float z, Vector3f dest) {
        float n = 1f / Math.fma(this.x, this.x, Math.fma(this.y, this.y, Math.fma(this.z, this.z, this.w * this.w)));
        float qx = this.x * n;
        float qy = this.y * n;
        float qz = this.z * n;
        float qw = this.w * n;
        float xx = qx * qx;
        float yy = qy * qy;
        float zz = qz * qz;
        float ww = qw * qw;
        float xy = qx * qy;
        float xz = qx * qz;
        float yz = qy * qz;
        float xw = qx * qw;
        float zw = qz * qw;
        float yw = qy * qw;
        float k = 1f / (xx + yy + zz + ww);
        return dest.set(Math.fma((xx - yy - zz + ww) * k, x, Math.fma(2f * (xy + zw) * k, y, 2f * (xz - yw) * k * z)), Math.fma(2f * (xy - zw) * k, x, Math.fma((yy - xx - zz + ww) * k, y, 2f * (yz + xw) * k * z)), Math.fma(2f * (xz + yw) * k, x, Math.fma(2f * (yz - xw) * k, y, (zz - xx - yy + ww) * k * z)));
    }

    public Vector3f transformUnit(Vector3f vec) {
        return this.transformUnit(vec.x, vec.y, vec.z, vec);
    }

    public Vector3f transformInverseUnit(Vector3f vec) {
        return this.transformInverseUnit(vec.x, vec.y, vec.z, vec);
    }

    public Vector3f transformUnit(Vector3f vec, Vector3f dest) {
        return this.transformUnit(vec.x, vec.y, vec.z, dest);
    }

    public Vector3f transformInverseUnit(Vector3f vec, Vector3f dest) {
        return this.transformInverseUnit(vec.x, vec.y, vec.z, dest);
    }

    public Vector3f transformUnit(float x, float y, float z, Vector3f dest) {
        float xx = this.x * this.x;
        float xy = this.x * this.y;
        float xz = this.x * this.z;
        float xw = this.x * this.w;
        float yy = this.y * this.y;
        float yz = this.y * this.z;
        float yw = this.y * this.w;
        float zz = this.z * this.z;
        float zw = this.z * this.w;
        return dest.set(Math.fma(Math.fma(-2f, yy + zz, 1f), x, Math.fma(2f * (xy - zw), y, 2f * (xz + yw) * z)), Math.fma(2f * (xy + zw), x, Math.fma(Math.fma(-2f, xx + zz, 1f), y, 2f * (yz - xw) * z)), Math.fma(2f * (xz - yw), x, Math.fma(2f * (yz + xw), y, Math.fma(-2f, xx + yy, 1f) * z)));
    }

    public Vector3f transformInverseUnit(float x, float y, float z, Vector3f dest) {
        float xx = this.x * this.x;
        float xy = this.x * this.y;
        float xz = this.x * this.z;
        float xw = this.x * this.w;
        float yy = this.y * this.y;
        float yz = this.y * this.z;
        float yw = this.y * this.w;
        float zz = this.z * this.z;
        float zw = this.z * this.w;
        return dest.set(Math.fma(Math.fma(-2f, yy + zz, 1f), x, Math.fma(2f * (xy + zw), y, 2f * (xz - yw) * z)), Math.fma(2f * (xy - zw), x, Math.fma(Math.fma(-2f, xx + zz, 1f), y, 2f * (yz + xw) * z)), Math.fma(2f * (xz + yw), x, Math.fma(2f * (yz - xw), y, Math.fma(-2f, xx + yy, 1f) * z)));
    }

    public Vector4f transform(Vector4f vec, Vector4f dest) {
        return this.transform(vec.x, vec.y, vec.z, dest);
    }

    public Vector4f transformInverse(Vector4f vec, Vector4f dest) {
        return this.transformInverse(vec.x, vec.y, vec.z, dest);
    }

    public Vector4f transform(float x, float y, float z, Vector4f dest) {
        float xx = this.x * this.x;
        float yy = this.y * this.y;
        float zz = this.z * this.z;
        float ww = this.w * this.w;
        float xy = this.x * this.y;
        float xz = this.x * this.z;
        float yz = this.y * this.z;
        float xw = this.x * this.w;
        float zw = this.z * this.w;
        float yw = this.y * this.w;
        float k = 1f / (xx + yy + zz + ww);
        return dest.set(Math.fma((xx - yy - zz + ww) * k, x, Math.fma(2f * (xy - zw) * k, y, 2f * (xz + yw) * k * z)), Math.fma(2f * (xy + zw) * k, x, Math.fma((yy - xx - zz + ww) * k, y, 2f * (yz - xw) * k * z)), Math.fma(2f * (xz - yw) * k, x, Math.fma(2f * (yz + xw) * k, y, (zz - xx - yy + ww) * k * z)));
    }

    public Vector4f transformInverse(float x, float y, float z, Vector4f dest) {
        float n = 1f / Math.fma(this.x, this.x, Math.fma(this.y, this.y, Math.fma(this.z, this.z, this.w * this.w)));
        float qx = this.x * n;
        float qy = this.y * n;
        float qz = this.z * n;
        float qw = this.w * n;
        float xx = qx * qx;
        float yy = qy * qy;
        float zz = qz * qz;
        float ww = qw * qw;
        float xy = qx * qy;
        float xz = qx * qz;
        float yz = qy * qz;
        float xw = qx * qw;
        float zw = qz * qw;
        float yw = qy * qw;
        float k = 1f / (xx + yy + zz + ww);
        return dest.set(Math.fma((xx - yy - zz + ww) * k, x, Math.fma(2f * (xy + zw) * k, y, 2f * (xz - yw) * k * z)), Math.fma(2f * (xy - zw) * k, x, Math.fma((yy - xx - zz + ww) * k, y, 2f * (yz + xw) * k * z)), Math.fma(2f * (xz + yw) * k, x, Math.fma(2f * (yz - xw) * k, y, (zz - xx - yy + ww) * k * z)));
    }

    public Vector3d transform(Vector3d vec) {
        return this.transform(vec.x, vec.y, vec.z, vec);
    }

    public Vector3d transformInverse(Vector3d vec) {
        return this.transformInverse(vec.x, vec.y, vec.z, vec);
    }

    public Vector4f transformUnit(Vector4f vec) {
        return this.transformUnit(vec.x, vec.y, vec.z, vec);
    }

    public Vector4f transformInverseUnit(Vector4f vec) {
        return this.transformInverseUnit(vec.x, vec.y, vec.z, vec);
    }

    public Vector4f transformUnit(Vector4f vec, Vector4f dest) {
        return this.transformUnit(vec.x, vec.y, vec.z, dest);
    }

    public Vector4f transformInverseUnit(Vector4f vec, Vector4f dest) {
        return this.transformInverseUnit(vec.x, vec.y, vec.z, dest);
    }

    public Vector4f transformUnit(float x, float y, float z, Vector4f dest) {
        float xx = this.x * this.x;
        float xy = this.x * this.y;
        float xz = this.x * this.z;
        float xw = this.x * this.w;
        float yy = this.y * this.y;
        float yz = this.y * this.z;
        float yw = this.y * this.w;
        float zz = this.z * this.z;
        float zw = this.z * this.w;
        return dest.set(Math.fma(Math.fma(-2f, yy + zz, 1f), x, Math.fma(2f * (xy - zw), y, 2f * (xz + yw) * z)), Math.fma(2f * (xy + zw), x, Math.fma(Math.fma(-2f, xx + zz, 1f), y, 2f * (yz - xw) * z)), Math.fma(2f * (xz - yw), x, Math.fma(2f * (yz + xw), y, Math.fma(-2f, xx + yy, 1f) * z)));
    }

    public Vector4f transformInverseUnit(float x, float y, float z, Vector4f dest) {
        float xx = this.x * this.x;
        float xy = this.x * this.y;
        float xz = this.x * this.z;
        float xw = this.x * this.w;
        float yy = this.y * this.y;
        float yz = this.y * this.z;
        float yw = this.y * this.w;
        float zz = this.z * this.z;
        float zw = this.z * this.w;
        return dest.set(Math.fma(Math.fma(-2f, yy + zz, 1f), x, Math.fma(2f * (xy + zw), y, 2f * (xz - yw) * z)), Math.fma(2f * (xy - zw), x, Math.fma(Math.fma(-2f, xx + zz, 1f), y, 2f * (yz + xw) * z)), Math.fma(2f * (xz + yw), x, Math.fma(2f * (yz - xw), y, Math.fma(-2f, xx + yy, 1f) * z)));
    }

    public Vector3d transformPositiveX(Vector3d dest) {
        float ww = this.w * this.w;
        float xx = this.x * this.x;
        float yy = this.y * this.y;
        float zz = this.z * this.z;
        float zw = this.z * this.w;
        float xy = this.x * this.y;
        float xz = this.x * this.z;
        float yw = this.y * this.w;
        dest.x = ww + xx - zz - yy;
        dest.y = xy + zw + zw + xy;
        dest.z = xz - yw + xz - yw;
        return dest;
    }

    public Vector4d transformPositiveX(Vector4d dest) {
        float ww = this.w * this.w;
        float xx = this.x * this.x;
        float yy = this.y * this.y;
        float zz = this.z * this.z;
        float zw = this.z * this.w;
        float xy = this.x * this.y;
        float xz = this.x * this.z;
        float yw = this.y * this.w;
        dest.x = ww + xx - zz - yy;
        dest.y = xy + zw + zw + xy;
        dest.z = xz - yw + xz - yw;
        return dest;
    }

    public Vector3d transformUnitPositiveX(Vector3d dest) {
        float yy = this.y * this.y;
        float zz = this.z * this.z;
        float xy = this.x * this.y;
        float xz = this.x * this.z;
        float yw = this.y * this.w;
        float zw = this.z * this.w;
        dest.x = 1f - yy - yy - zz - zz;
        dest.y = xy + zw + xy + zw;
        dest.z = xz - yw + xz - yw;
        return dest;
    }

    public Vector4d transformUnitPositiveX(Vector4d dest) {
        float yy = this.y * this.y;
        float zz = this.z * this.z;
        float xy = this.x * this.y;
        float xz = this.x * this.z;
        float yw = this.y * this.w;
        float zw = this.z * this.w;
        dest.x = 1f - yy - yy - zz - zz;
        dest.y = xy + zw + xy + zw;
        dest.z = xz - yw + xz - yw;
        return dest;
    }

    public Vector3d transformPositiveY(Vector3d dest) {
        float ww = this.w * this.w;
        float xx = this.x * this.x;
        float yy = this.y * this.y;
        float zz = this.z * this.z;
        float zw = this.z * this.w;
        float xy = this.x * this.y;
        float yz = this.y * this.z;
        float xw = this.x * this.w;
        dest.x = -zw + xy - zw + xy;
        dest.y = yy - zz + ww - xx;
        dest.z = yz + yz + xw + xw;
        return dest;
    }

    public Vector4d transformPositiveY(Vector4d dest) {
        float ww = this.w * this.w;
        float xx = this.x * this.x;
        float yy = this.y * this.y;
        float zz = this.z * this.z;
        float zw = this.z * this.w;
        float xy = this.x * this.y;
        float yz = this.y * this.z;
        float xw = this.x * this.w;
        dest.x = -zw + xy - zw + xy;
        dest.y = yy - zz + ww - xx;
        dest.z = yz + yz + xw + xw;
        return dest;
    }

    public Vector4d transformUnitPositiveY(Vector4d dest) {
        float xx = this.x * this.x;
        float zz = this.z * this.z;
        float xy = this.x * this.y;
        float yz = this.y * this.z;
        float xw = this.x * this.w;
        float zw = this.z * this.w;
        dest.x = xy - zw + xy - zw;
        dest.y = 1f - xx - xx - zz - zz;
        dest.z = yz + yz + xw + xw;
        return dest;
    }

    public Vector3d transformUnitPositiveY(Vector3d dest) {
        float xx = this.x * this.x;
        float zz = this.z * this.z;
        float xy = this.x * this.y;
        float yz = this.y * this.z;
        float xw = this.x * this.w;
        float zw = this.z * this.w;
        dest.x = xy - zw + xy - zw;
        dest.y = 1f - xx - xx - zz - zz;
        dest.z = yz + yz + xw + xw;
        return dest;
    }

    public Vector3d transformPositiveZ(Vector3d dest) {
        float ww = this.w * this.w;
        float xx = this.x * this.x;
        float yy = this.y * this.y;
        float zz = this.z * this.z;
        float xz = this.x * this.z;
        float yw = this.y * this.w;
        float yz = this.y * this.z;
        float xw = this.x * this.w;
        dest.x = yw + xz + xz + yw;
        dest.y = yz + yz - xw - xw;
        dest.z = zz - yy - xx + ww;
        return dest;
    }

    public Vector4d transformPositiveZ(Vector4d dest) {
        float ww = this.w * this.w;
        float xx = this.x * this.x;
        float yy = this.y * this.y;
        float zz = this.z * this.z;
        float xz = this.x * this.z;
        float yw = this.y * this.w;
        float yz = this.y * this.z;
        float xw = this.x * this.w;
        dest.x = yw + xz + xz + yw;
        dest.y = yz + yz - xw - xw;
        dest.z = zz - yy - xx + ww;
        return dest;
    }

    public Vector4d transformUnitPositiveZ(Vector4d dest) {
        float xx = this.x * this.x;
        float yy = this.y * this.y;
        float xz = this.x * this.z;
        float yz = this.y * this.z;
        float xw = this.x * this.w;
        float yw = this.y * this.w;
        dest.x = xz + yw + xz + yw;
        dest.y = yz + yz - xw - xw;
        dest.z = 1f - xx - xx - yy - yy;
        return dest;
    }

    public Vector3d transformUnitPositiveZ(Vector3d dest) {
        float xx = this.x * this.x;
        float yy = this.y * this.y;
        float xz = this.x * this.z;
        float yz = this.y * this.z;
        float xw = this.x * this.w;
        float yw = this.y * this.w;
        dest.x = xz + yw + xz + yw;
        dest.y = yz + yz - xw - xw;
        dest.z = 1f - xx - xx - yy - yy;
        return dest;
    }

    public Vector4d transform(Vector4d vec) {
        return this.transform(vec, vec);
    }

    public Vector4d transformInverse(Vector4d vec) {
        return this.transformInverse(vec, vec);
    }

    public Vector3d transform(Vector3d vec, Vector3d dest) {
        return this.transform(vec.x, vec.y, vec.z, dest);
    }

    public Vector3d transformInverse(Vector3d vec, Vector3d dest) {
        return this.transformInverse(vec.x, vec.y, vec.z, dest);
    }

    public Vector3d transform(float x, float y, float z, Vector3d dest) {
        return this.transform(x, y, (double) z, dest);
    }

    public Vector3d transformInverse(float x, float y, float z, Vector3d dest) {
        return this.transformInverse(x, y, (double) z, dest);
    }

    public Vector3d transform(double x, double y, double z, Vector3d dest) {
        float xx = this.x * this.x;
        float yy = this.y * this.y;
        float zz = this.z * this.z;
        float ww = this.w * this.w;
        float xy = this.x * this.y;
        float xz = this.x * this.z;
        float yz = this.y * this.z;
        float xw = this.x * this.w;
        float zw = this.z * this.w;
        float yw = this.y * this.w;
        float k = 1f / (xx + yy + zz + ww);
        return dest.set(Math.fma((xx - yy - zz + ww) * k, x, Math.fma(2f * (xy - zw) * k, y, (double) (2f * (xz + yw) * k) * z)), Math.fma(2f * (xy + zw) * k, x, Math.fma((yy - xx - zz + ww) * k, y, (double) (2f * (yz - xw) * k) * z)), Math.fma(2f * (xz - yw) * k, x, Math.fma(2f * (yz + xw) * k, y, (double) ((zz - xx - yy + ww) * k) * z)));
    }

    public Vector3d transformInverse(double x, double y, double z, Vector3d dest) {
        float n = 1f / Math.fma(this.x, this.x, Math.fma(this.y, this.y, Math.fma(this.z, this.z, this.w * this.w)));
        float qx = this.x * n;
        float qy = this.y * n;
        float qz = this.z * n;
        float qw = this.w * n;
        float xx = qx * qx;
        float yy = qy * qy;
        float zz = qz * qz;
        float ww = qw * qw;
        float xy = qx * qy;
        float xz = qx * qz;
        float yz = qy * qz;
        float xw = qx * qw;
        float zw = qz * qw;
        float yw = qy * qw;
        float k = 1f / (xx + yy + zz + ww);
        return dest.set(Math.fma((xx - yy - zz + ww) * k, x, Math.fma(2f * (xy + zw) * k, y, (double) (2f * (xz - yw) * k) * z)), Math.fma(2f * (xy - zw) * k, x, Math.fma((yy - xx - zz + ww) * k, y, (double) (2f * (yz + xw) * k) * z)), Math.fma(2f * (xz + yw) * k, x, Math.fma(2f * (yz - xw) * k, y, (double) ((zz - xx - yy + ww) * k) * z)));
    }

    public Vector4d transform(Vector4d vec, Vector4d dest) {
        return this.transform(vec.x, vec.y, vec.z, dest);
    }

    public Vector4d transformInverse(Vector4d vec, Vector4d dest) {
        return this.transformInverse(vec.x, vec.y, vec.z, dest);
    }

    public Vector4d transform(double x, double y, double z, Vector4d dest) {
        float xx = this.x * this.x;
        float yy = this.y * this.y;
        float zz = this.z * this.z;
        float ww = this.w * this.w;
        float xy = this.x * this.y;
        float xz = this.x * this.z;
        float yz = this.y * this.z;
        float xw = this.x * this.w;
        float zw = this.z * this.w;
        float yw = this.y * this.w;
        float k = 1f / (xx + yy + zz + ww);
        return dest.set(Math.fma((xx - yy - zz + ww) * k, x, Math.fma(2f * (xy - zw) * k, y, (double) (2f * (xz + yw) * k) * z)), Math.fma(2f * (xy + zw) * k, x, Math.fma((yy - xx - zz + ww) * k, y, (double) (2f * (yz - xw) * k) * z)), Math.fma(2f * (xz - yw) * k, x, Math.fma(2f * (yz + xw) * k, y, (double) ((zz - xx - yy + ww) * k) * z)));
    }

    public Vector4d transformInverse(double x, double y, double z, Vector4d dest) {
        float n = 1f / Math.fma(this.x, this.x, Math.fma(this.y, this.y, Math.fma(this.z, this.z, this.w * this.w)));
        float qx = this.x * n;
        float qy = this.y * n;
        float qz = this.z * n;
        float qw = this.w * n;
        float xx = qx * qx;
        float yy = qy * qy;
        float zz = qz * qz;
        float ww = qw * qw;
        float xy = qx * qy;
        float xz = qx * qz;
        float yz = qy * qz;
        float xw = qx * qw;
        float zw = qz * qw;
        float yw = qy * qw;
        float k = 1f / (xx + yy + zz + ww);
        return dest.set(Math.fma((xx - yy - zz + ww) * k, x, Math.fma(2f * (xy + zw) * k, y, (double) (2f * (xz - yw) * k) * z)), Math.fma(2f * (xy - zw) * k, x, Math.fma((yy - xx - zz + ww) * k, y, (double) (2f * (yz + xw) * k) * z)), Math.fma(2f * (xz + yw) * k, x, Math.fma(2f * (yz - xw) * k, y, (double) ((zz - xx - yy + ww) * k) * z)));
    }

    public Vector4d transformUnit(Vector4d vec) {
        return this.transformUnit(vec, vec);
    }

    public Vector4d transformInverseUnit(Vector4d vec) {
        return this.transformInverseUnit(vec, vec);
    }

    public Vector3d transformUnit(Vector3d vec, Vector3d dest) {
        return this.transformUnit(vec.x, vec.y, vec.z, dest);
    }

    public Vector3d transformInverseUnit(Vector3d vec, Vector3d dest) {
        return this.transformInverseUnit(vec.x, vec.y, vec.z, dest);
    }

    public Vector3d transformUnit(float x, float y, float z, Vector3d dest) {
        return this.transformUnit(x, y, (double) z, dest);
    }

    public Vector3d transformInverseUnit(float x, float y, float z, Vector3d dest) {
        return this.transformInverseUnit(x, y, (double) z, dest);
    }

    public Vector3d transformUnit(double x, double y, double z, Vector3d dest) {
        float xx = this.x * this.x;
        float xy = this.x * this.y;
        float xz = this.x * this.z;
        float xw = this.x * this.w;
        float yy = this.y * this.y;
        float yz = this.y * this.z;
        float yw = this.y * this.w;
        float zz = this.z * this.z;
        float zw = this.z * this.w;
        return dest.set(Math.fma(Math.fma(-2f, yy + zz, 1f), x, Math.fma(2f * (xy - zw), y, (double) (2f * (xz + yw)) * z)), Math.fma(2f * (xy + zw), x, Math.fma(Math.fma(-2f, xx + zz, 1f), y, (double) (2f * (yz - xw)) * z)), Math.fma(2f * (xz - yw), x, Math.fma(2f * (yz + xw), y, (double) Math.fma(-2f, xx + yy, 1f) * z)));
    }

    public Vector3d transformInverseUnit(double x, double y, double z, Vector3d dest) {
        float xx = this.x * this.x;
        float xy = this.x * this.y;
        float xz = this.x * this.z;
        float xw = this.x * this.w;
        float yy = this.y * this.y;
        float yz = this.y * this.z;
        float yw = this.y * this.w;
        float zz = this.z * this.z;
        float zw = this.z * this.w;
        return dest.set(Math.fma(Math.fma(-2f, yy + zz, 1f), x, Math.fma(2f * (xy + zw), y, (double) (2f * (xz - yw)) * z)), Math.fma(2f * (xy - zw), x, Math.fma(Math.fma(-2f, xx + zz, 1f), y, (double) (2f * (yz + xw)) * z)), Math.fma(2f * (xz + yw), x, Math.fma(2f * (yz - xw), y, (double) Math.fma(-2f, xx + yy, 1f) * z)));
    }

    public Vector4d transformUnit(Vector4d vec, Vector4d dest) {
        return this.transformUnit(vec.x, vec.y, vec.z, dest);
    }

    public Vector4d transformInverseUnit(Vector4d vec, Vector4d dest) {
        return this.transformInverseUnit(vec.x, vec.y, vec.z, dest);
    }

    public Vector4d transformUnit(double x, double y, double z, Vector4d dest) {
        float xx = this.x * this.x;
        float xy = this.x * this.y;
        float xz = this.x * this.z;
        float xw = this.x * this.w;
        float yy = this.y * this.y;
        float yz = this.y * this.z;
        float yw = this.y * this.w;
        float zz = this.z * this.z;
        float zw = this.z * this.w;
        return dest.set(Math.fma(Math.fma(-2f, yy + zz, 1f), x, Math.fma(2f * (xy - zw), y, (double) (2f * (xz + yw)) * z)), Math.fma(2f * (xy + zw), x, Math.fma(Math.fma(-2f, xx + zz, 1f), y, (double) (2f * (yz - xw)) * z)), Math.fma(2f * (xz - yw), x, Math.fma(2f * (yz + xw), y, (double) Math.fma(-2f, xx + yy, 1f) * z)));
    }

    public Vector4d transformInverseUnit(double x, double y, double z, Vector4d dest) {
        float xx = this.x * this.x;
        float xy = this.x * this.y;
        float xz = this.x * this.z;
        float xw = this.x * this.w;
        float yy = this.y * this.y;
        float yz = this.y * this.z;
        float yw = this.y * this.w;
        float zz = this.z * this.z;
        float zw = this.z * this.w;
        return dest.set(Math.fma(Math.fma(-2f, yy + zz, 1f), x, Math.fma(2f * (xy + zw), y, (double) (2f * (xz - yw)) * z)), Math.fma(2f * (xy - zw), x, Math.fma(Math.fma(-2f, xx + zz, 1f), y, (double) (2f * (yz + xw)) * z)), Math.fma(2f * (xz + yw), x, Math.fma(2f * (yz - xw), y, (double) Math.fma(-2f, xx + yy, 1f) * z)));
    }

    public Quaternionf invert(Quaternionf dest) {
        float invNorm = 1f / Math.fma(this.x, this.x, Math.fma(this.y, this.y, Math.fma(this.z, this.z, this.w * this.w)));
        dest.x = -this.x * invNorm;
        dest.y = -this.y * invNorm;
        dest.z = -this.z * invNorm;
        dest.w = this.w * invNorm;
        return dest;
    }

    public Quaternionf invert() {
        return this.invert(this);
    }

    public Quaternionf div(Quaternionf b, Quaternionf dest) {
        float invNorm = 1f / Math.fma(b.x, b.x, Math.fma(b.y, b.y, Math.fma(b.z, b.z, b.w() * b.w())));
        float x = -b.x * invNorm;
        float y = -b.y * invNorm;
        float z = -b.z * invNorm;
        float w = b.w() * invNorm;
        return dest.set(Math.fma(this.w, x, Math.fma(this.x, w, Math.fma(this.y, z, -this.z * y))), Math.fma(this.w, y, Math.fma(-this.x, z, Math.fma(this.y, w, this.z * x))), Math.fma(this.w, z, Math.fma(this.x, y, Math.fma(-this.y, x, this.z * w))), Math.fma(this.w, w, Math.fma(-this.x, x, Math.fma(-this.y, y, -this.z * z))));
    }

    public Quaternionf div(Quaternionf b) {
        return this.div(b, this);
    }

    public Quaternionf conjugate() {
        return this.conjugate(this);
    }

    public Quaternionf conjugate(Quaternionf dest) {
        dest.x = -this.x;
        dest.y = -this.y;
        dest.z = -this.z;
        dest.w = this.w;
        return dest;
    }

    public Quaternionf identity() {
        this.x = 0f;
        this.y = 0f;
        this.z = 0f;
        this.w = 1f;
        return this;
    }

    public Quaternionf rotateXYZ(float angleX, float angleY, float angleZ) {
        return this.rotateXYZ(angleX, angleY, angleZ, this);
    }

    public Quaternionf rotateXYZ(float angleX, float angleY, float angleZ, Quaternionf dest) {
        float sx = Math.sin(angleX * 0.5F);
        float cx = Math.cosFromSin(sx, angleX * 0.5F);
        float sy = Math.sin(angleY * 0.5F);
        float cy = Math.cosFromSin(sy, angleY * 0.5F);
        float sz = Math.sin(angleZ * 0.5F);
        float cz = Math.cosFromSin(sz, angleZ * 0.5F);
        float cycz = cy * cz;
        float sysz = sy * sz;
        float sycz = sy * cz;
        float cysz = cy * sz;
        float w = cx * cycz - sx * sysz;
        float x = sx * cycz + cx * sysz;
        float y = cx * sycz - sx * cysz;
        float z = cx * cysz + sx * sycz;
        return dest.set(Math.fma(this.w, x, Math.fma(this.x, w, Math.fma(this.y, z, -this.z * y))), Math.fma(this.w, y, Math.fma(-this.x, z, Math.fma(this.y, w, this.z * x))), Math.fma(this.w, z, Math.fma(this.x, y, Math.fma(-this.y, x, this.z * w))), Math.fma(this.w, w, Math.fma(-this.x, x, Math.fma(-this.y, y, -this.z * z))));
    }

    public Quaternionf rotateZYX(float angleZ, float angleY, float angleX) {
        return this.rotateZYX(angleZ, angleY, angleX, this);
    }

    public Quaternionf rotateZYX(float angleZ, float angleY, float angleX, Quaternionf dest) {
        float sx = Math.sin(angleX * 0.5F);
        float cx = Math.cosFromSin(sx, angleX * 0.5F);
        float sy = Math.sin(angleY * 0.5F);
        float cy = Math.cosFromSin(sy, angleY * 0.5F);
        float sz = Math.sin(angleZ * 0.5F);
        float cz = Math.cosFromSin(sz, angleZ * 0.5F);
        float cycz = cy * cz;
        float sysz = sy * sz;
        float sycz = sy * cz;
        float cysz = cy * sz;
        float w = cx * cycz + sx * sysz;
        float x = sx * cycz - cx * sysz;
        float y = cx * sycz + sx * cysz;
        float z = cx * cysz - sx * sycz;
        return dest.set(Math.fma(this.w, x, Math.fma(this.x, w, Math.fma(this.y, z, -this.z * y))), Math.fma(this.w, y, Math.fma(-this.x, z, Math.fma(this.y, w, this.z * x))), Math.fma(this.w, z, Math.fma(this.x, y, Math.fma(-this.y, x, this.z * w))), Math.fma(this.w, w, Math.fma(-this.x, x, Math.fma(-this.y, y, -this.z * z))));
    }

    public Quaternionf rotateYXZ(float angleY, float angleX, float angleZ) {
        return this.rotateYXZ(angleY, angleX, angleZ, this);
    }

    public Quaternionf rotateYXZ(float angleY, float angleX, float angleZ, Quaternionf dest) {
        float sx = Math.sin(angleX * 0.5F);
        float cx = Math.cosFromSin(sx, angleX * 0.5F);
        float sy = Math.sin(angleY * 0.5F);
        float cy = Math.cosFromSin(sy, angleY * 0.5F);
        float sz = Math.sin(angleZ * 0.5F);
        float cz = Math.cosFromSin(sz, angleZ * 0.5F);
        float yx = cy * sx;
        float yy = sy * cx;
        float yz = sy * sx;
        float yw = cy * cx;
        float x = yx * cz + yy * sz;
        float y = yy * cz - yx * sz;
        float z = yw * sz - yz * cz;
        float w = yw * cz + yz * sz;
        return dest.set(Math.fma(this.w, x, Math.fma(this.x, w, Math.fma(this.y, z, -this.z * y))), Math.fma(this.w, y, Math.fma(-this.x, z, Math.fma(this.y, w, this.z * x))), Math.fma(this.w, z, Math.fma(this.x, y, Math.fma(-this.y, x, this.z * w))), Math.fma(this.w, w, Math.fma(-this.x, x, Math.fma(-this.y, y, -this.z * z))));
    }

    public Vector3f getEulerAnglesXYZ(Vector3f eulerAngles) {
        eulerAngles.x = Math.atan2(this.x * this.w - this.y * this.z, 0.5F - this.x * this.x - this.y * this.y);
        eulerAngles.y = Math.safeAsin(2f * (this.x * this.z + this.y * this.w));
        eulerAngles.z = Math.atan2(this.z * this.w - this.x * this.y, 0.5F - this.y * this.y - this.z * this.z);
        return eulerAngles;
    }

    public Vector3f getEulerAnglesZYX(Vector3f eulerAngles) {
        eulerAngles.x = Math.atan2(this.y * this.z + this.w * this.x, 0.5F - this.x * this.x + this.y * this.y);
        eulerAngles.y = Math.safeAsin(-2f * (this.x * this.z - this.w * this.y));
        eulerAngles.z = Math.atan2(this.x * this.y + this.w * this.z, 0.5F - this.y * this.y - this.z * this.z);
        return eulerAngles;
    }

    public Vector3f getEulerAnglesZXY(Vector3f eulerAngles) {
        eulerAngles.x = Math.safeAsin(2f * (this.w * this.x + this.y * this.z));
        eulerAngles.y = Math.atan2(this.w * this.y - this.x * this.z, 0.5F - this.y * this.y - this.x * this.x);
        eulerAngles.z = Math.atan2(this.w * this.z - this.x * this.y, 0.5F - this.z * this.z - this.x * this.x);
        return eulerAngles;
    }

    public Vector3f getEulerAnglesYXZ(Vector3f eulerAngles) {
        eulerAngles.x = Math.safeAsin(-2f * (this.y * this.z - this.w * this.x));
        eulerAngles.y = Math.atan2(this.x * this.z + this.y * this.w, 0.5F - this.y * this.y - this.x * this.x);
        eulerAngles.z = Math.atan2(this.y * this.x + this.w * this.z, 0.5F - this.x * this.x - this.z * this.z);
        return eulerAngles;
    }

    public float lengthSquared() {
        return Math.fma(this.x, this.x, Math.fma(this.y, this.y, Math.fma(this.z, this.z, this.w * this.w)));
    }

    public Quaternionf rotationXYZ(float angleX, float angleY, float angleZ) {
        float sx = Math.sin(angleX * 0.5F);
        float cx = Math.cosFromSin(sx, angleX * 0.5F);
        float sy = Math.sin(angleY * 0.5F);
        float cy = Math.cosFromSin(sy, angleY * 0.5F);
        float sz = Math.sin(angleZ * 0.5F);
        float cz = Math.cosFromSin(sz, angleZ * 0.5F);
        float cycz = cy * cz;
        float sysz = sy * sz;
        float sycz = sy * cz;
        float cysz = cy * sz;
        this.w = cx * cycz - sx * sysz;
        this.x = sx * cycz + cx * sysz;
        this.y = cx * sycz - sx * cysz;
        this.z = cx * cysz + sx * sycz;
        return this;
    }

    public Quaternionf rotationZYX(float angleZ, float angleY, float angleX) {
        float sx = Math.sin(angleX * 0.5F);
        float cx = Math.cosFromSin(sx, angleX * 0.5F);
        float sy = Math.sin(angleY * 0.5F);
        float cy = Math.cosFromSin(sy, angleY * 0.5F);
        float sz = Math.sin(angleZ * 0.5F);
        float cz = Math.cosFromSin(sz, angleZ * 0.5F);
        float cycz = cy * cz;
        float sysz = sy * sz;
        float sycz = sy * cz;
        float cysz = cy * sz;
        this.w = cx * cycz + sx * sysz;
        this.x = sx * cycz - cx * sysz;
        this.y = cx * sycz + sx * cysz;
        this.z = cx * cysz - sx * sycz;
        return this;
    }

    public Quaternionf rotationYXZ(float angleY, float angleX, float angleZ) {
        float sx = Math.sin(angleX * 0.5F);
        float cx = Math.cosFromSin(sx, angleX * 0.5F);
        float sy = Math.sin(angleY * 0.5F);
        float cy = Math.cosFromSin(sy, angleY * 0.5F);
        float sz = Math.sin(angleZ * 0.5F);
        float cz = Math.cosFromSin(sz, angleZ * 0.5F);
        float x = cy * sx;
        float y = sy * cx;
        float z = sy * sx;
        float w = cy * cx;
        this.x = x * cz + y * sz;
        this.y = y * cz - x * sz;
        this.z = w * sz - z * cz;
        this.w = w * cz + z * sz;
        return this;
    }

    public Quaternionf slerp(Quaternionf target, float alpha) {
        return this.slerp(target, alpha, this);
    }

    public Quaternionf slerp(Quaternionf target, float alpha, Quaternionf dest) {
        float cosom = Math.fma(this.x, target.x, Math.fma(this.y, target.y, Math.fma(this.z, target.z, this.w * target.w())));
        float absCosom = Math.abs(cosom);
        float scale0;
        float scale1;
        if (1f - absCosom > 1.0E-6F) {
            float sinSqr = 1f - absCosom * absCosom;
            float sinom = Math.invsqrt(sinSqr);
            float omega = Math.atan2(sinSqr * sinom, absCosom);
            scale0 = (float) (Math.sin((1.0 - (double) alpha) * (double) omega) * (double) sinom);
            scale1 = Math.sin(alpha * omega) * sinom;
        } else {
            scale0 = 1f - alpha;
            scale1 = alpha;
        }

        scale1 = cosom >= 0f ? scale1 : -scale1;
        dest.x = Math.fma(scale0, this.x, scale1 * target.x);
        dest.y = Math.fma(scale0, this.y, scale1 * target.y);
        dest.z = Math.fma(scale0, this.z, scale1 * target.z);
        dest.w = Math.fma(scale0, this.w, scale1 * target.w());
        return dest;
    }

    public static Quaternionf slerp(Quaternionf[] qs, float[] weights, Quaternionf dest) {
        dest.set(qs[0]);
        float w = weights[0];

        for (int i = 1; i < qs.length; ++i) {
            float w1 = weights[i];
            float rw1 = w1 / (w + w1);
            w += w1;
            dest.slerp(qs[i], rw1);
        }

        return dest;
    }

    public Quaternionf scale(float factor) {
        return this.scale(factor, this);
    }

    public Quaternionf scale(float factor, Quaternionf dest) {
        float sqrt = Math.sqrt(factor);
        dest.x = sqrt * this.x;
        dest.y = sqrt * this.y;
        dest.z = sqrt * this.z;
        dest.w = sqrt * this.w;
        return dest;
    }

    public Quaternionf scaling(float factor) {
        float sqrt = Math.sqrt(factor);
        this.x = 0f;
        this.y = 0f;
        this.z = 0f;
        this.w = sqrt;
        return this;
    }

    public Quaternionf integrate(float dt, float vx, float vy, float vz) {
        return this.integrate(dt, vx, vy, vz, this);
    }

    public Quaternionf integrate(float dt, float vx, float vy, float vz, Quaternionf dest) {
        float thetaX = dt * vx * 0.5F;
        float thetaY = dt * vy * 0.5F;
        float thetaZ = dt * vz * 0.5F;
        float thetaMagSq = thetaX * thetaX + thetaY * thetaY + thetaZ * thetaZ;
        float s;
        float dqW;
        if (thetaMagSq * thetaMagSq / 24f < 1.0E-8F) {
            dqW = 1f - thetaMagSq * 0.5F;
            s = 1f - thetaMagSq / 6f;
        } else {
            float thetaMag = Math.sqrt(thetaMagSq);
            float sin = Math.sin(thetaMag);
            s = sin / thetaMag;
            dqW = Math.cosFromSin(sin, thetaMag);
        }

        float dqX = thetaX * s;
        float dqY = thetaY * s;
        float dqZ = thetaZ * s;
        return dest.set(Math.fma(dqW, this.x, Math.fma(dqX, this.w, Math.fma(dqY, this.z, -dqZ * this.y))), Math.fma(dqW, this.y, Math.fma(-dqX, this.z, Math.fma(dqY, this.w, dqZ * this.x))), Math.fma(dqW, this.z, Math.fma(dqX, this.y, Math.fma(-dqY, this.x, dqZ * this.w))), Math.fma(dqW, this.w, Math.fma(-dqX, this.x, Math.fma(-dqY, this.y, -dqZ * this.z))));
    }

    public Quaternionf nlerp(Quaternionf q, float factor) {
        return this.nlerp(q, factor, this);
    }

    public Quaternionf nlerp(Quaternionf q, float factor, Quaternionf dest) {
        float cosom = Math.fma(this.x, q.x, Math.fma(this.y, q.y, Math.fma(this.z, q.z, this.w * q.w())));
        float scale0 = 1f - factor;
        float scale1 = cosom >= 0f ? factor : -factor;
        dest.x = Math.fma(scale0, this.x, scale1 * q.x);
        dest.y = Math.fma(scale0, this.y, scale1 * q.y);
        dest.z = Math.fma(scale0, this.z, scale1 * q.z);
        dest.w = Math.fma(scale0, this.w, scale1 * q.w());
        float s = Math.invsqrt(Math.fma(dest.x, dest.x, Math.fma(dest.y, dest.y, Math.fma(dest.z, dest.z, dest.w * dest.w))));
        dest.x *= s;
        dest.y *= s;
        dest.z *= s;
        dest.w *= s;
        return dest;
    }

    public static Quaternionf nlerp(Quaternionf[] qs, float[] weights, Quaternionf dest) {
        dest.set(qs[0]);
        float w = weights[0];

        for (int i = 1; i < qs.length; ++i) {
            float w1 = weights[i];
            float rw1 = w1 / (w + w1);
            w += w1;
            dest.nlerp(qs[i], rw1);
        }

        return dest;
    }

    public Quaternionf nlerpIterative(Quaternionf q, float alpha, float dotThreshold, Quaternionf dest) {
        float q1x = this.x;
        float q1y = this.y;
        float q1z = this.z;
        float q1w = this.w;
        float q2x = q.x;
        float q2y = q.y;
        float q2z = q.z;
        float q2w = q.w();
        float dot = Math.fma(q1x, q2x, Math.fma(q1y, q2y, Math.fma(q1z, q2z, q1w * q2w)));
        float absDot = Math.abs(dot);
        if (0.999999F < absDot) {
            return dest.set(this);
        } else {
            float alphaN;
            float scale0;
            float scale1;
            float s;
            for (alphaN = alpha; absDot < dotThreshold; absDot = Math.abs(dot)) {
                scale0 = 0.5F;
                scale1 = dot >= 0f ? 0.5F : -0.5F;
                if (alphaN < 0.5F) {
                    q2x = Math.fma(scale0, q2x, scale1 * q1x);
                    q2y = Math.fma(scale0, q2y, scale1 * q1y);
                    q2z = Math.fma(scale0, q2z, scale1 * q1z);
                    q2w = Math.fma(scale0, q2w, scale1 * q1w);
                    s = Math.invsqrt(Math.fma(q2x, q2x, Math.fma(q2y, q2y, Math.fma(q2z, q2z, q2w * q2w))));
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
                    s = Math.invsqrt(Math.fma(q1x, q1x, Math.fma(q1y, q1y, Math.fma(q1z, q1z, q1w * q1w))));
                    q1x *= s;
                    q1y *= s;
                    q1z *= s;
                    q1w *= s;
                    alphaN = alphaN + alphaN - 1f;
                }

                dot = Math.fma(q1x, q2x, Math.fma(q1y, q2y, Math.fma(q1z, q2z, q1w * q2w)));
            }

            scale0 = 1f - alphaN;
            scale1 = dot >= 0f ? alphaN : -alphaN;
            s = Math.fma(scale0, q1x, scale1 * q2x);
            float resY = Math.fma(scale0, q1y, scale1 * q2y);
            float resZ = Math.fma(scale0, q1z, scale1 * q2z);
            float resW = Math.fma(scale0, q1w, scale1 * q2w);
            s = Math.invsqrt(Math.fma(s, s, Math.fma(resY, resY, Math.fma(resZ, resZ, resW * resW))));
            dest.x = s * s;
            dest.y = resY * s;
            dest.z = resZ * s;
            dest.w = resW * s;
            return dest;
        }
    }

    public Quaternionf nlerpIterative(Quaternionf q, float alpha, float dotThreshold) {
        return this.nlerpIterative(q, alpha, dotThreshold, this);
    }

    public static Quaternionf nlerpIterative(Quaternionf[] qs, float[] weights, float dotThreshold, Quaternionf dest) {
        dest.set(qs[0]);
        float w = weights[0];

        for (int i = 1; i < qs.length; ++i) {
            float w1 = weights[i];
            float rw1 = w1 / (w + w1);
            w += w1;
            dest.nlerpIterative(qs[i], rw1, dotThreshold);
        }

        return dest;
    }

    public Quaternionf lookAlong(Vector3f dir, Vector3f up) {
        return this.lookAlong(dir.x, dir.y, dir.z, up.x, up.y, up.z, this);
    }

    public Quaternionf lookAlong(Vector3f dir, Vector3f up, Quaternionf dest) {
        return this.lookAlong(dir.x, dir.y, dir.z, up.x, up.y, up.z, dest);
    }

    public Quaternionf lookAlong(float dirX, float dirY, float dirZ, float upX, float upY, float upZ) {
        return this.lookAlong(dirX, dirY, dirZ, upX, upY, upZ, this);
    }

    public Quaternionf lookAlong(float dirX, float dirY, float dirZ, float upX, float upY, float upZ, Quaternionf dest) {
        float invDirLength = Math.invsqrt(dirX * dirX + dirY * dirY + dirZ * dirZ);
        float dirnX = -dirX * invDirLength;
        float dirnY = -dirY * invDirLength;
        float dirnZ = -dirZ * invDirLength;
        float leftX = upY * dirnZ - upZ * dirnY;
        float leftY = upZ * dirnX - upX * dirnZ;
        float leftZ = upX * dirnY - upY * dirnX;
        float invLeftLength = Math.invsqrt(leftX * leftX + leftY * leftY + leftZ * leftZ);
        leftX *= invLeftLength;
        leftY *= invLeftLength;
        leftZ *= invLeftLength;
        float upnX = dirnY * leftZ - dirnZ * leftY;
        float upnY = dirnZ * leftX - dirnX * leftZ;
        float upnZ = dirnX * leftY - dirnY * leftX;
        double tr = leftX + upnY + dirnZ;
        float x;
        float y;
        float z;
        float w;
        double t;
        if (tr >= 0.0) {
            t = Math.sqrt(tr + 1.0);
            w = (float) (t * 0.5);
            t = 0.5 / t;
            x = (float) ((double) (dirnY - upnZ) * t);
            y = (float) ((double) (leftZ - dirnX) * t);
            z = (float) ((double) (upnX - leftY) * t);
        } else if (leftX > upnY && leftX > dirnZ) {
            t = Math.sqrt(1.0 + (double) leftX - (double) upnY - (double) dirnZ);
            x = (float) (t * 0.5);
            t = 0.5 / t;
            y = (float) ((double) (leftY + upnX) * t);
            z = (float) ((double) (dirnX + leftZ) * t);
            w = (float) ((double) (dirnY - upnZ) * t);
        } else if (upnY > dirnZ) {
            t = Math.sqrt(1.0 + (double) upnY - (double) leftX - (double) dirnZ);
            y = (float) (t * 0.5);
            t = 0.5 / t;
            x = (float) ((double) (leftY + upnX) * t);
            z = (float) ((double) (upnZ + dirnY) * t);
            w = (float) ((double) (leftZ - dirnX) * t);
        } else {
            t = Math.sqrt(1.0 + (double) dirnZ - (double) leftX - (double) upnY);
            z = (float) (t * 0.5);
            t = 0.5 / t;
            x = (float) ((double) (dirnX + leftZ) * t);
            y = (float) ((double) (upnZ + dirnY) * t);
            w = (float) ((double) (upnX - leftY) * t);
        }

        return dest.set(Math.fma(this.w, x, Math.fma(this.x, w, Math.fma(this.y, z, -this.z * y))), Math.fma(this.w, y, Math.fma(-this.x, z, Math.fma(this.y, w, this.z * x))), Math.fma(this.w, z, Math.fma(this.x, y, Math.fma(-this.y, x, this.z * w))), Math.fma(this.w, w, Math.fma(-this.x, x, Math.fma(-this.y, y, -this.z * z))));
    }

    public Quaternionf rotationTo(float fromDirX, float fromDirY, float fromDirZ, float toDirX, float toDirY, float toDirZ) {
        float fn = Math.invsqrt(Math.fma(fromDirX, fromDirX, Math.fma(fromDirY, fromDirY, fromDirZ * fromDirZ)));
        float tn = Math.invsqrt(Math.fma(toDirX, toDirX, Math.fma(toDirY, toDirY, toDirZ * toDirZ)));
        float fx = fromDirX * fn;
        float fy = fromDirY * fn;
        float fz = fromDirZ * fn;
        float tx = toDirX * tn;
        float ty = toDirY * tn;
        float tz = toDirZ * tn;
        float dot = fx * tx + fy * ty + fz * tz;
        float x;
        float y;
        float z;
        float w;
        if (dot < -0.999999F) {
            x = fy;
            y = -fx;
            z = 0f;
            if (fy * fy + y * y == 0f) {
                x = 0f;
                y = fz;
                z = -fy;
            }
            this.x = x;
            this.y = y;
            this.z = z;
            this.w = 0f;
        } else {
            float sd2 = Math.sqrt((1f + dot) * 2f);
            float isd2 = 1f / sd2;
            float cx = fy * tz - fz * ty;
            float cy = fz * tx - fx * tz;
            float cz = fx * ty - fy * tx;
            x = cx * isd2;
            y = cy * isd2;
            z = cz * isd2;
            w = sd2 * 0.5F;
            float n2 = Math.invsqrt(Math.fma(x, x, Math.fma(y, y, Math.fma(z, z, w * w))));
            this.x = x * n2;
            this.y = y * n2;
            this.z = z * n2;
            this.w = w * n2;
        }

        return this;
    }

    public Quaternionf rotationTo(Vector3f fromDir, Vector3f toDir) {
        return this.rotationTo(fromDir.x, fromDir.y, fromDir.z, toDir.x, toDir.y, toDir.z);
    }

    public Quaternionf rotateTo(float fromDirX, float fromDirY, float fromDirZ, float toDirX, float toDirY, float toDirZ, Quaternionf dest) {
        float fn = Math.invsqrt(Math.fma(fromDirX, fromDirX, Math.fma(fromDirY, fromDirY, fromDirZ * fromDirZ)));
        float tn = Math.invsqrt(Math.fma(toDirX, toDirX, Math.fma(toDirY, toDirY, toDirZ * toDirZ)));
        float fx = fromDirX * fn;
        float fy = fromDirY * fn;
        float fz = fromDirZ * fn;
        float tx = toDirX * tn;
        float ty = toDirY * tn;
        float tz = toDirZ * tn;
        float dot = fx * tx + fy * ty + fz * tz;
        float x;
        float y;
        float z;
        float w;
        if (dot < -0.999999F) {
            x = fy;
            y = -fx;
            z = 0f;
            w = 0f;
            if (fy * fy + y * y == 0f) {
                x = 0f;
                y = fz;
                z = -fy;
                w = 0f;
            }
        } else {
            float sd2 = Math.sqrt((1f + dot) * 2f);
            float isd2 = 1f / sd2;
            float cx = fy * tz - fz * ty;
            float cy = fz * tx - fx * tz;
            float cz = fx * ty - fy * tx;
            x = cx * isd2;
            y = cy * isd2;
            z = cz * isd2;
            w = sd2 * 0.5F;
            float n2 = Math.invsqrt(Math.fma(x, x, Math.fma(y, y, Math.fma(z, z, w * w))));
            x *= n2;
            y *= n2;
            z *= n2;
            w *= n2;
        }

        return dest.set(Math.fma(this.w, x, Math.fma(this.x, w, Math.fma(this.y, z, -this.z * y))), Math.fma(this.w, y, Math.fma(-this.x, z, Math.fma(this.y, w, this.z * x))), Math.fma(this.w, z, Math.fma(this.x, y, Math.fma(-this.y, x, this.z * w))), Math.fma(this.w, w, Math.fma(-this.x, x, Math.fma(-this.y, y, -this.z * z))));
    }

    public Quaternionf rotateTo(float fromDirX, float fromDirY, float fromDirZ, float toDirX, float toDirY, float toDirZ) {
        return this.rotateTo(fromDirX, fromDirY, fromDirZ, toDirX, toDirY, toDirZ, this);
    }

    public Quaternionf rotateTo(Vector3f fromDir, Vector3f toDir, Quaternionf dest) {
        return this.rotateTo(fromDir.x, fromDir.y, fromDir.z, toDir.x, toDir.y, toDir.z, dest);
    }

    public Quaternionf rotateTo(Vector3f fromDir, Vector3f toDir) {
        return this.rotateTo(fromDir.x, fromDir.y, fromDir.z, toDir.x, toDir.y, toDir.z, this);
    }

    public Quaternionf rotateX(float angle) {
        return this.rotateX(angle, this);
    }

    public Quaternionf rotateX(float angle, Quaternionf dest) {
        float sin = Math.sin(angle * 0.5F);
        float cos = Math.cosFromSin(sin, angle * 0.5F);
        return dest.set(this.w * sin + this.x * cos, this.y * cos + this.z * sin, this.z * cos - this.y * sin, this.w * cos - this.x * sin);
    }

    public Quaternionf rotateY(float angle) {
        return this.rotateY(angle, this);
    }

    public Quaternionf rotateY(float angle, Quaternionf dest) {
        float sin = Math.sin(angle * 0.5F);
        float cos = Math.cosFromSin(sin, angle * 0.5F);
        return dest.set(this.x * cos - this.z * sin, this.w * sin + this.y * cos, this.x * sin + this.z * cos, this.w * cos - this.y * sin);
    }

    public Quaternionf rotateZ(float angle) {
        return this.rotateZ(angle, this);
    }

    public Quaternionf rotateZ(float angle, Quaternionf dest) {
        float sin = Math.sin(angle * 0.5F);
        float cos = Math.cosFromSin(sin, angle * 0.5F);
        return dest.set(this.x * cos + this.y * sin, this.y * cos - this.x * sin, this.w * sin + this.z * cos, this.w * cos - this.z * sin);
    }

    public Quaternionf rotateLocalX(float angle) {
        return this.rotateLocalX(angle, this);
    }

    public Quaternionf rotateLocalX(float angle, Quaternionf dest) {
        float halfAngle = angle * 0.5F;
        float s = Math.sin(halfAngle);
        float c = Math.cosFromSin(s, halfAngle);
        dest.set(c * this.x + s * this.w, c * this.y - s * this.z, c * this.z + s * this.y, c * this.w - s * this.x);
        return dest;
    }

    public Quaternionf rotateLocalY(float angle) {
        return this.rotateLocalY(angle, this);
    }

    public Quaternionf rotateLocalY(float angle, Quaternionf dest) {
        float halfAngle = angle * 0.5F;
        float s = Math.sin(halfAngle);
        float c = Math.cosFromSin(s, halfAngle);
        dest.set(c * this.x + s * this.z, c * this.y + s * this.w, c * this.z - s * this.x, c * this.w - s * this.y);
        return dest;
    }

    public Quaternionf rotateLocalZ(float angle) {
        return this.rotateLocalZ(angle, this);
    }

    public Quaternionf rotateLocalZ(float angle, Quaternionf dest) {
        float halfAngle = angle * 0.5F;
        float s = Math.sin(halfAngle);
        float c = Math.cosFromSin(s, halfAngle);
        dest.set(c * this.x - s * this.y, c * this.y + s * this.x, c * this.z + s * this.w, c * this.w - s * this.z);
        return dest;
    }

    public Quaternionf rotateAxis(float angle, float axisX, float axisY, float axisZ, Quaternionf dest) {
        float halfAngle = angle / 2f;
        float sinAngle = Math.sin(halfAngle);
        float invVLength = Math.invsqrt(Math.fma(axisX, axisX, Math.fma(axisY, axisY, axisZ * axisZ)));
        float rx = axisX * invVLength * sinAngle;
        float ry = axisY * invVLength * sinAngle;
        float rz = axisZ * invVLength * sinAngle;
        float rw = Math.cosFromSin(sinAngle, halfAngle);
        return dest.set(Math.fma(this.w, rx, Math.fma(this.x, rw, Math.fma(this.y, rz, -this.z * ry))), Math.fma(this.w, ry, Math.fma(-this.x, rz, Math.fma(this.y, rw, this.z * rx))), Math.fma(this.w, rz, Math.fma(this.x, ry, Math.fma(-this.y, rx, this.z * rw))), Math.fma(this.w, rw, Math.fma(-this.x, rx, Math.fma(-this.y, ry, -this.z * rz))));
    }

    public Quaternionf rotateAxis(float angle, Vector3f axis, Quaternionf dest) {
        return this.rotateAxis(angle, axis.x, axis.y, axis.z, dest);
    }

    public Quaternionf rotateAxis(float angle, Vector3f axis) {
        return this.rotateAxis(angle, axis.x, axis.y, axis.z, this);
    }

    public Quaternionf rotateAxis(float angle, float axisX, float axisY, float axisZ) {
        return this.rotateAxis(angle, axisX, axisY, axisZ, this);
    }

    public String toString() {
        return "(" + x + "," + y + "," + z + "," + w + ")";
    }

    public int hashCode() {
        int result = 1;
        result = 31 * result + Float.floatToIntBits(this.w);
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
            Quaternionf other = (Quaternionf) obj;
            if (Float.floatToIntBits(this.w) != Float.floatToIntBits(other.w)) {
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

    public Quaternionf difference(Quaternionf other) {
        return this.difference(other, this);
    }

    public Quaternionf difference(Quaternionf other, Quaternionf dest) {
        float invNorm = 1f / this.lengthSquared();
        float x = -this.x * invNorm;
        float y = -this.y * invNorm;
        float z = -this.z * invNorm;
        float w = this.w * invNorm;
        dest.set(Math.fma(w, other.x, Math.fma(x, other.w(), Math.fma(y, other.z, -z * other.y))), Math.fma(w, other.y, Math.fma(-x, other.z, Math.fma(y, other.w(), z * other.x))), Math.fma(w, other.z, Math.fma(x, other.y, Math.fma(-y, other.x, z * other.w()))), Math.fma(w, other.w(), Math.fma(-x, other.x, Math.fma(-y, other.y, -z * other.z))));
        return dest;
    }

    public Vector3f positiveX(Vector3f dir) {
        float invNorm = 1f / this.lengthSquared();
        float nx = -this.x * invNorm;
        float ny = -this.y * invNorm;
        float nz = -this.z * invNorm;
        float nw = this.w * invNorm;
        float dy = ny + ny;
        float dz = nz + nz;
        dir.x = -ny * dy - nz * dz + 1f;
        dir.y = nx * dy + nw * dz;
        dir.z = nx * dz - nw * dy;
        return dir;
    }

    public Vector3f normalizedPositiveX(Vector3f dir) {
        float dy = this.y + this.y;
        float dz = this.z + this.z;
        dir.x = -this.y * dy - this.z * dz + 1f;
        dir.y = this.x * dy - this.w * dz;
        dir.z = this.x * dz + this.w * dy;
        return dir;
    }

    public Vector3f positiveY(Vector3f dir) {
        float invNorm = 1f / this.lengthSquared();
        float nx = -this.x * invNorm;
        float ny = -this.y * invNorm;
        float nz = -this.z * invNorm;
        float nw = this.w * invNorm;
        float dx = nx + nx;
        float dy = ny + ny;
        float dz = nz + nz;
        dir.x = nx * dy - nw * dz;
        dir.y = -nx * dx - nz * dz + 1f;
        dir.z = ny * dz + nw * dx;
        return dir;
    }

    public Vector3f normalizedPositiveY(Vector3f dir) {
        float dx = this.x + this.x;
        float dy = this.y + this.y;
        float dz = this.z + this.z;
        dir.x = this.x * dy + this.w * dz;
        dir.y = -this.x * dx - this.z * dz + 1f;
        dir.z = this.y * dz - this.w * dx;
        return dir;
    }

    public Vector3f positiveZ(Vector3f dir) {
        float invNorm = 1f / this.lengthSquared();
        float nx = -this.x * invNorm;
        float ny = -this.y * invNorm;
        float nz = -this.z * invNorm;
        float nw = this.w * invNorm;
        float dx = nx + nx;
        float dy = ny + ny;
        float dz = nz + nz;
        dir.x = nx * dz + nw * dy;
        dir.y = ny * dz - nw * dx;
        dir.z = -nx * dx - ny * dy + 1f;
        return dir;
    }

    public Vector3f normalizedPositiveZ(Vector3f dir) {
        float dx = this.x + this.x;
        float dy = this.y + this.y;
        float dz = this.z + this.z;
        dir.x = this.x * dz - this.w * dy;
        dir.y = this.y * dz + this.w * dx;
        dir.z = -this.x * dx - this.y * dy + 1f;
        return dir;
    }

    public Quaternionf conjugateBy(Quaternionf q) {
        return this.conjugateBy(q, this);
    }

    public Quaternionf conjugateBy(Quaternionf q, Quaternionf dest) {
        float invNorm = 1f / q.lengthSquared();
        float qix = -q.x * invNorm;
        float qiy = -q.y * invNorm;
        float qiz = -q.z * invNorm;
        float qiw = q.w() * invNorm;
        float qpx = Math.fma(q.w(), this.x, Math.fma(q.x, this.w, Math.fma(q.y, this.z, -q.z * this.y)));
        float qpy = Math.fma(q.w(), this.y, Math.fma(-q.x, this.z, Math.fma(q.y, this.w, q.z * this.x)));
        float qpz = Math.fma(q.w(), this.z, Math.fma(q.x, this.y, Math.fma(-q.y, this.x, q.z * this.w)));
        float qpw = Math.fma(q.w(), this.w, Math.fma(-q.x, this.x, Math.fma(-q.y, this.y, -q.z * this.z)));
        return dest.set(Math.fma(qpw, qix, Math.fma(qpx, qiw, Math.fma(qpy, qiz, -qpz * qiy))), Math.fma(qpw, qiy, Math.fma(-qpx, qiz, Math.fma(qpy, qiw, qpz * qix))), Math.fma(qpw, qiz, Math.fma(qpx, qiy, Math.fma(-qpy, qix, qpz * qiw))), Math.fma(qpw, qiw, Math.fma(-qpx, qix, Math.fma(-qpy, qiy, -qpz * qiz))));
    }

    public boolean isFinite() {
        return Math.isFinite(this.x) && Math.isFinite(this.y) && Math.isFinite(this.z) && Math.isFinite(this.w);
    }

    public boolean equals(Quaternionf q, float delta) {
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
            return Runtime.equals(this.w, q.w(), delta);
        }
    }

    public boolean equals(float x, float y, float z, float w) {
        if (Float.floatToIntBits(this.x) != Float.floatToIntBits(x)) {
            return false;
        } else if (Float.floatToIntBits(this.y) != Float.floatToIntBits(y)) {
            return false;
        } else if (Float.floatToIntBits(this.z) != Float.floatToIntBits(z)) {
            return false;
        } else {
            return Float.floatToIntBits(this.w) == Float.floatToIntBits(w);
        }
    }

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
