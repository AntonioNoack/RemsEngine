package org.joml;

@SuppressWarnings("unused")
public class Vector3d {
    public double x;
    public double y;
    public double z;

    public Vector3d() {
    }

    public Vector3d(double d) {
        this.x = d;
        this.y = d;
        this.z = d;
    }

    public Vector3d(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Vector3d(Vector3f v) {
        this.x = v.x;
        this.y = v.y;
        this.z = v.z;
    }

    public Vector3d(Vector3i v) {
        this.x = v.x;
        this.y = v.y;
        this.z = v.z;
    }

    public Vector3d(Vector2f v, double z) {
        this.x = v.x;
        this.y = v.y;
        this.z = z;
    }
    public Vector3d(Vector2i v, double z) {
        this.x = v.x;
        this.y = v.y;
        this.z = z;
    }
    public Vector3d(Vector3d v) {
        this.x = v.x;
        this.y = v.y;
        this.z = v.z;
    }
    public Vector3d(Vector2d v, double z) {
        this.x = v.x;
        this.y = v.y;
        this.z = z;
    }

    public Vector3d(double[] xyz) {
        this.x = xyz[0];
        this.y = xyz[1];
        this.z = xyz[2];
    }

    public Vector3d(float[] xyz) {
        this.x = xyz[0];
        this.y = xyz[1];
        this.z = xyz[2];
    }

    public Vector3d set(Vector3d v) {
        this.x = v.x;
        this.y = v.y;
        this.z = v.z;
        return this;
    }

    public Vector3d set(Vector3i v) {
        this.x = v.x;
        this.y = v.y;
        this.z = v.z;
        return this;
    }

    public Vector3d set(Vector2d v, double z) {
        this.x = v.x;
        this.y = v.y;
        this.z = z;
        return this;
    }

    public Vector3d set(Vector2i v, double z) {
        this.x = v.x;
        this.y = v.y;
        this.z = z;
        return this;
    }

    public Vector3d set(Vector3f v) {
        this.x = v.x;
        this.y = v.y;
        this.z = v.z;
        return this;
    }

    public Vector3d set(Vector2f v, double z) {
        this.x = v.x;
        this.y = v.y;
        this.z = z;
        return this;
    }

    public Vector3d set(double d) {
        this.x = d;
        this.y = d;
        this.z = d;
        return this;
    }

    public Vector3d set(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
        return this;
    }

    public Vector3d set(double[] xyz) {
        this.x = xyz[0];
        this.y = xyz[1];
        this.z = xyz[2];
        return this;
    }

    public Vector3d set(float[] xyz) {
        this.x = xyz[0];
        this.y = xyz[1];
        this.z = xyz[2];
        return this;
    }

    public Vector3d setComponent(int component, double value) throws IllegalArgumentException {
        switch (component) {
            case 0:
                this.x = value;
                break;
            case 1:
                this.y = value;
                break;
            case 2:
                this.z = value;
                break;
            default:
                throw new IllegalArgumentException();
        }

        return this;
    }

    public Vector3d sub(Vector3d v) {
        this.x -= v.x;
        this.y -= v.y;
        this.z -= v.z;
        return this;
    }

    public Vector3d sub(Vector3d v, Vector3d dest) {
        dest.x = this.x - v.x;
        dest.y = this.y - v.y;
        dest.z = this.z - v.z;
        return dest;
    }

    public Vector3d sub(Vector3f v) {
        this.x -= v.x;
        this.y -= v.y;
        this.z -= v.z;
        return this;
    }

    public Vector3d sub(Vector3f v, Vector3d dest) {
        dest.x = this.x - (double) v.x;
        dest.y = this.y - (double) v.y;
        dest.z = this.z - (double) v.z;
        return dest;
    }

    public Vector3d sub(double x, double y, double z) {
        this.x -= x;
        this.y -= y;
        this.z -= z;
        return this;
    }

    public Vector3d sub(double x, double y, double z, Vector3d dest) {
        dest.x = this.x - x;
        dest.y = this.y - y;
        dest.z = this.z - z;
        return dest;
    }

    public Vector3d add(Vector3d v) {
        this.x += v.x;
        this.y += v.y;
        this.z += v.z;
        return this;
    }

    public Vector3d add(Vector3d v, Vector3d dest) {
        dest.x = this.x + v.x;
        dest.y = this.y + v.y;
        dest.z = this.z + v.z;
        return dest;
    }

    public Vector3d add(Vector3f v) {
        this.x += v.x;
        this.y += v.y;
        this.z += v.z;
        return this;
    }

    public Vector3d add(Vector3f v, Vector3d dest) {
        dest.x = this.x + (double) v.x;
        dest.y = this.y + (double) v.y;
        dest.z = this.z + (double) v.z;
        return dest;
    }

    public Vector3d add(double x, double y, double z) {
        this.x += x;
        this.y += y;
        this.z += z;
        return this;
    }

    public Vector3d add(double x, double y, double z, Vector3d dest) {
        dest.x = this.x + x;
        dest.y = this.y + y;
        dest.z = this.z + z;
        return dest;
    }

    public Vector3d fma(Vector3d a, Vector3d b) {
        this.x = Math.fma(a.x, b.x, this.x);
        this.y = Math.fma(a.y, b.y, this.y);
        this.z = Math.fma(a.z, b.z, this.z);
        return this;
    }

    public Vector3d fma(double a, Vector3d b) {
        this.x = Math.fma(a, b.x, this.x);
        this.y = Math.fma(a, b.y, this.y);
        this.z = Math.fma(a, b.z, this.z);
        return this;
    }

    public Vector3d fma(Vector3f a, Vector3f b) {
        this.x = Math.fma(a.x, b.x, this.x);
        this.y = Math.fma(a.y, b.y, this.y);
        this.z = Math.fma(a.z, b.z, this.z);
        return this;
    }

    public Vector3d fma(Vector3f a, Vector3f b, Vector3d dest) {
        dest.x = Math.fma(a.x, b.x, this.x);
        dest.y = Math.fma(a.y, b.y, this.y);
        dest.z = Math.fma(a.z, b.z, this.z);
        return dest;
    }

    public Vector3d fma(double a, Vector3f b) {
        this.x = Math.fma(a, b.x, this.x);
        this.y = Math.fma(a, b.y, this.y);
        this.z = Math.fma(a, b.z, this.z);
        return this;
    }

    public Vector3d fma(Vector3d a, Vector3d b, Vector3d dest) {
        dest.x = Math.fma(a.x, b.x, this.x);
        dest.y = Math.fma(a.y, b.y, this.y);
        dest.z = Math.fma(a.z, b.z, this.z);
        return dest;
    }

    public Vector3d fma(double a, Vector3d b, Vector3d dest) {
        dest.x = Math.fma(a, b.x, this.x);
        dest.y = Math.fma(a, b.y, this.y);
        dest.z = Math.fma(a, b.z, this.z);
        return dest;
    }

    public Vector3d fma(Vector3d a, Vector3f b, Vector3d dest) {
        dest.x = Math.fma(a.x, b.x, this.x);
        dest.y = Math.fma(a.y, b.y, this.y);
        dest.z = Math.fma(a.z, b.z, this.z);
        return dest;
    }

    public Vector3d fma(double a, Vector3f b, Vector3d dest) {
        dest.x = Math.fma(a, b.x, this.x);
        dest.y = Math.fma(a, b.y, this.y);
        dest.z = Math.fma(a, b.z, this.z);
        return dest;
    }

    public Vector3d mulAdd(Vector3d a, Vector3d b) {
        this.x = Math.fma(this.x, a.x, b.x);
        this.y = Math.fma(this.y, a.y, b.y);
        this.z = Math.fma(this.z, a.z, b.z);
        return this;
    }

    public Vector3d mulAdd(double a, Vector3d b) {
        this.x = Math.fma(this.x, a, b.x);
        this.y = Math.fma(this.y, a, b.y);
        this.z = Math.fma(this.z, a, b.z);
        return this;
    }

    public Vector3d mulAdd(Vector3d a, Vector3d b, Vector3d dest) {
        dest.x = Math.fma(this.x, a.x, b.x);
        dest.y = Math.fma(this.y, a.y, b.y);
        dest.z = Math.fma(this.z, a.z, b.z);
        return dest;
    }

    public Vector3d mulAdd(double a, Vector3d b, Vector3d dest) {
        dest.x = Math.fma(this.x, a, b.x);
        dest.y = Math.fma(this.y, a, b.y);
        dest.z = Math.fma(this.z, a, b.z);
        return dest;
    }

    public Vector3d mulAdd(Vector3f a, Vector3d b, Vector3d dest) {
        dest.x = Math.fma(this.x, a.x, b.x);
        dest.y = Math.fma(this.y, a.y, b.y);
        dest.z = Math.fma(this.z, a.z, b.z);
        return dest;
    }

    public Vector3d mul(Vector3d v) {
        this.x *= v.x;
        this.y *= v.y;
        this.z *= v.z;
        return this;
    }

    public Vector3d mul(Vector3f v) {
        this.x *= v.x;
        this.y *= v.y;
        this.z *= v.z;
        return this;
    }

    public Vector3d mul(Vector3f v, Vector3d dest) {
        dest.x = this.x * (double) v.x;
        dest.y = this.y * (double) v.y;
        dest.z = this.z * (double) v.z;
        return dest;
    }

    public Vector3d mul(Vector3d v, Vector3d dest) {
        dest.x = this.x * v.x;
        dest.y = this.y * v.y;
        dest.z = this.z * v.z;
        return dest;
    }

    public Vector3d div(Vector3d v) {
        this.x /= v.x;
        this.y /= v.y;
        this.z /= v.z;
        return this;
    }

    public Vector3d div(Vector3f v) {
        this.x /= v.x;
        this.y /= v.y;
        this.z /= v.z;
        return this;
    }

    public Vector3d div(Vector3f v, Vector3d dest) {
        dest.x = this.x / (double) v.x;
        dest.y = this.y / (double) v.y;
        dest.z = this.z / (double) v.z;
        return dest;
    }

    public Vector3d div(Vector3d v, Vector3d dest) {
        dest.x = this.x / v.x;
        dest.y = this.y / v.y;
        dest.z = this.z / v.z;
        return dest;
    }

    public Vector3d mulProject(Matrix4d mat, double w, Vector3d dest) {
        double invW = 1.0 / Math.fma(mat.m03, this.x, Math.fma(mat.m13, this.y, Math.fma(mat.m23, this.z, mat.m33 * w)));
        double rx = Math.fma(mat.m00, this.x, Math.fma(mat.m10, this.y, Math.fma(mat.m20, this.z, mat.m30 * w))) * invW;
        double ry = Math.fma(mat.m01, this.x, Math.fma(mat.m11, this.y, Math.fma(mat.m21, this.z, mat.m31 * w))) * invW;
        double rz = Math.fma(mat.m02, this.x, Math.fma(mat.m12, this.y, Math.fma(mat.m22, this.z, mat.m32 * w))) * invW;
        dest.x = rx;
        dest.y = ry;
        dest.z = rz;
        return dest;
    }

    public Vector3d mulProject(Matrix4d mat, Vector3d dest) {
        double invW = 1.0 / Math.fma(mat.m03, this.x, Math.fma(mat.m13, this.y, Math.fma(mat.m23, this.z, mat.m33)));
        double rx = Math.fma(mat.m00, this.x, Math.fma(mat.m10, this.y, Math.fma(mat.m20, this.z, mat.m30))) * invW;
        double ry = Math.fma(mat.m01, this.x, Math.fma(mat.m11, this.y, Math.fma(mat.m21, this.z, mat.m31))) * invW;
        double rz = Math.fma(mat.m02, this.x, Math.fma(mat.m12, this.y, Math.fma(mat.m22, this.z, mat.m32))) * invW;
        dest.x = rx;
        dest.y = ry;
        dest.z = rz;
        return dest;
    }

    public Vector3d mulProject(Matrix4d mat) {
        double invW = 1.0 / Math.fma(mat.m03, this.x, Math.fma(mat.m13, this.y, Math.fma(mat.m23, this.z, mat.m33)));
        double rx = Math.fma(mat.m00, this.x, Math.fma(mat.m10, this.y, Math.fma(mat.m20, this.z, mat.m30))) * invW;
        double ry = Math.fma(mat.m01, this.x, Math.fma(mat.m11, this.y, Math.fma(mat.m21, this.z, mat.m31))) * invW;
        double rz = Math.fma(mat.m02, this.x, Math.fma(mat.m12, this.y, Math.fma(mat.m22, this.z, mat.m32))) * invW;
        this.x = rx;
        this.y = ry;
        this.z = rz;
        return this;
    }

    public Vector3d mulProject(Matrix4f mat, Vector3d dest) {
        double invW = 1.0 / Math.fma(mat.m03, this.x, Math.fma(mat.m13, this.y, Math.fma(mat.m23, this.z, mat.m33)));
        double rx = ((double) mat.m00 * this.x + (double) mat.m10 * this.y + (double) mat.m20 * this.z + (double) mat.m30) * invW;
        double ry = ((double) mat.m01 * this.x + (double) mat.m11 * this.y + (double) mat.m21 * this.z + (double) mat.m31) * invW;
        double rz = ((double) mat.m02 * this.x + (double) mat.m12 * this.y + (double) mat.m22 * this.z + (double) mat.m32) * invW;
        dest.x = rx;
        dest.y = ry;
        dest.z = rz;
        return dest;
    }

    public Vector3d mulProject(Matrix4f mat) {
        double invW = 1.0 / Math.fma(mat.m03, this.x, Math.fma(mat.m13, this.y, Math.fma(mat.m23, this.z, mat.m33)));
        double rx = ((double) mat.m00 * this.x + (double) mat.m10 * this.y + (double) mat.m20 * this.z + (double) mat.m30) * invW;
        double ry = ((double) mat.m01 * this.x + (double) mat.m11 * this.y + (double) mat.m21 * this.z + (double) mat.m31) * invW;
        double rz = ((double) mat.m02 * this.x + (double) mat.m12 * this.y + (double) mat.m22 * this.z + (double) mat.m32) * invW;
        this.x = rx;
        this.y = ry;
        this.z = rz;
        return this;
    }

    public Vector3d mul(Matrix3f mat) {
        double rx = Math.fma(mat.m00, this.x, Math.fma(mat.m10, this.y, (double) mat.m20 * this.z));
        double ry = Math.fma(mat.m01, this.x, Math.fma(mat.m11, this.y, (double) mat.m21 * this.z));
        double rz = Math.fma(mat.m02, this.x, Math.fma(mat.m12, this.y, (double) mat.m22 * this.z));
        this.x = rx;
        this.y = ry;
        this.z = rz;
        return this;
    }

    public Vector3d mul(Matrix3d mat) {
        double rx = Math.fma(mat.m00, this.x, Math.fma(mat.m10, this.y, mat.m20 * this.z));
        double ry = Math.fma(mat.m01, this.x, Math.fma(mat.m11, this.y, mat.m21 * this.z));
        double rz = Math.fma(mat.m02, this.x, Math.fma(mat.m12, this.y, mat.m22 * this.z));
        this.x = rx;
        this.y = ry;
        this.z = rz;
        return this;
    }

    public Vector3d mul(Matrix3d mat, Vector3d dest) {
        double rx = Math.fma(mat.m00, this.x, Math.fma(mat.m10, this.y, mat.m20 * this.z));
        double ry = Math.fma(mat.m01, this.x, Math.fma(mat.m11, this.y, mat.m21 * this.z));
        double rz = Math.fma(mat.m02, this.x, Math.fma(mat.m12, this.y, mat.m22 * this.z));
        dest.x = rx;
        dest.y = ry;
        dest.z = rz;
        return dest;
    }

    public Vector3f mul(Matrix3d mat, Vector3f dest) {
        double rx = Math.fma(mat.m00, this.x, Math.fma(mat.m10, this.y, mat.m20 * this.z));
        double ry = Math.fma(mat.m01, this.x, Math.fma(mat.m11, this.y, mat.m21 * this.z));
        double rz = Math.fma(mat.m02, this.x, Math.fma(mat.m12, this.y, mat.m22 * this.z));
        dest.x = (float) rx;
        dest.y = (float) ry;
        dest.z = (float) rz;
        return dest;
    }

    public Vector3d mul(Matrix3f mat, Vector3d dest) {
        double rx = Math.fma(mat.m00, this.x, Math.fma(mat.m10, this.y, (double) mat.m20 * this.z));
        double ry = Math.fma(mat.m01, this.x, Math.fma(mat.m11, this.y, (double) mat.m21 * this.z));
        double rz = Math.fma(mat.m02, this.x, Math.fma(mat.m12, this.y, (double) mat.m22 * this.z));
        dest.x = rx;
        dest.y = ry;
        dest.z = rz;
        return dest;
    }

    public Vector3d mul(Matrix3x2d mat) {
        double rx = Math.fma(mat.m00, this.x, Math.fma(mat.m10, this.y, mat.m20 * this.z));
        double ry = Math.fma(mat.m01, this.x, Math.fma(mat.m11, this.y, mat.m21 * this.z));
        this.x = rx;
        this.y = ry;
        return this;
    }

    public Vector3d mul(Matrix3x2d mat, Vector3d dest) {
        double rx = Math.fma(mat.m00, this.x, Math.fma(mat.m10, this.y, mat.m20 * this.z));
        double ry = Math.fma(mat.m01, this.x, Math.fma(mat.m11, this.y, mat.m21 * this.z));
        dest.x = rx;
        dest.y = ry;
        dest.z = this.z;
        return dest;
    }

    public Vector3d mul(Matrix3x2f mat) {
        double rx = Math.fma(mat.m00, this.x, Math.fma(mat.m10, this.y, (double) mat.m20 * this.z));
        double ry = Math.fma(mat.m01, this.x, Math.fma(mat.m11, this.y, (double) mat.m21 * this.z));
        this.x = rx;
        this.y = ry;
        return this;
    }

    public Vector3d mul(Matrix3x2f mat, Vector3d dest) {
        double rx = Math.fma(mat.m00, this.x, Math.fma(mat.m10, this.y, (double) mat.m20 * this.z));
        double ry = Math.fma(mat.m01, this.x, Math.fma(mat.m11, this.y, (double) mat.m21 * this.z));
        dest.x = rx;
        dest.y = ry;
        dest.z = this.z;
        return dest;
    }

    public Vector3d mulTranspose(Matrix3d mat) {
        double rx = Math.fma(mat.m00, this.x, Math.fma(mat.m01, this.y, mat.m02 * this.z));
        double ry = Math.fma(mat.m10, this.x, Math.fma(mat.m11, this.y, mat.m12 * this.z));
        double rz = Math.fma(mat.m20, this.x, Math.fma(mat.m21, this.y, mat.m22 * this.z));
        this.x = rx;
        this.y = ry;
        this.z = rz;
        return this;
    }

    public Vector3d mulTranspose(Matrix3d mat, Vector3d dest) {
        double rx = Math.fma(mat.m00, this.x, Math.fma(mat.m01, this.y, mat.m02 * this.z));
        double ry = Math.fma(mat.m10, this.x, Math.fma(mat.m11, this.y, mat.m12 * this.z));
        double rz = Math.fma(mat.m20, this.x, Math.fma(mat.m21, this.y, mat.m22 * this.z));
        dest.x = rx;
        dest.y = ry;
        dest.z = rz;
        return dest;
    }

    public Vector3d mulTranspose(Matrix3f mat) {
        double rx = Math.fma(mat.m00, this.x, Math.fma(mat.m01, this.y, (double) mat.m02 * this.z));
        double ry = Math.fma(mat.m10, this.x, Math.fma(mat.m11, this.y, (double) mat.m12 * this.z));
        double rz = Math.fma(mat.m20, this.x, Math.fma(mat.m21, this.y, (double) mat.m22 * this.z));
        this.x = rx;
        this.y = ry;
        this.z = rz;
        return this;
    }

    public Vector3d mulTranspose(Matrix3f mat, Vector3d dest) {
        double rx = Math.fma(mat.m00, this.x, Math.fma(mat.m01, this.y, (double) mat.m02 * this.z));
        double ry = Math.fma(mat.m10, this.x, Math.fma(mat.m11, this.y, (double) mat.m12 * this.z));
        double rz = Math.fma(mat.m20, this.x, Math.fma(mat.m21, this.y, (double) mat.m22 * this.z));
        dest.x = rx;
        dest.y = ry;
        dest.z = rz;
        return dest;
    }

    public Vector3d mulPosition(Matrix4f mat) {
        double rx = Math.fma(mat.m00, this.x, Math.fma(mat.m10, this.y, Math.fma(mat.m20, this.z, mat.m30)));
        double ry = Math.fma(mat.m01, this.x, Math.fma(mat.m11, this.y, Math.fma(mat.m21, this.z, mat.m31)));
        double rz = Math.fma(mat.m02, this.x, Math.fma(mat.m12, this.y, Math.fma(mat.m22, this.z, mat.m32)));
        this.x = rx;
        this.y = ry;
        this.z = rz;
        return this;
    }

    public Vector3d mulPosition(Matrix4d mat) {
        double rx = Math.fma(mat.m00, this.x, Math.fma(mat.m10, this.y, Math.fma(mat.m20, this.z, mat.m30)));
        double ry = Math.fma(mat.m01, this.x, Math.fma(mat.m11, this.y, Math.fma(mat.m21, this.z, mat.m31)));
        double rz = Math.fma(mat.m02, this.x, Math.fma(mat.m12, this.y, Math.fma(mat.m22, this.z, mat.m32)));
        this.x = rx;
        this.y = ry;
        this.z = rz;
        return this;
    }

    public Vector3d mulPosition(Matrix4x3d mat) {
        double rx = Math.fma(mat.m00, this.x, Math.fma(mat.m10, this.y, Math.fma(mat.m20, this.z, mat.m30)));
        double ry = Math.fma(mat.m01, this.x, Math.fma(mat.m11, this.y, Math.fma(mat.m21, this.z, mat.m31)));
        double rz = Math.fma(mat.m02, this.x, Math.fma(mat.m12, this.y, Math.fma(mat.m22, this.z, mat.m32)));
        this.x = rx;
        this.y = ry;
        this.z = rz;
        return this;
    }

    public Vector3d mulPosition(Matrix4x3f mat) {
        double rx = Math.fma(mat.m00, this.x, Math.fma(mat.m10, this.y, Math.fma(mat.m20, this.z, mat.m30)));
        double ry = Math.fma(mat.m01, this.x, Math.fma(mat.m11, this.y, Math.fma(mat.m21, this.z, mat.m31)));
        double rz = Math.fma(mat.m02, this.x, Math.fma(mat.m12, this.y, Math.fma(mat.m22, this.z, mat.m32)));
        this.x = rx;
        this.y = ry;
        this.z = rz;
        return this;
    }

    public Vector3d mulPosition(Matrix4d mat, Vector3d dest) {
        double rx = Math.fma(mat.m00, this.x, Math.fma(mat.m10, this.y, Math.fma(mat.m20, this.z, mat.m30)));
        double ry = Math.fma(mat.m01, this.x, Math.fma(mat.m11, this.y, Math.fma(mat.m21, this.z, mat.m31)));
        double rz = Math.fma(mat.m02, this.x, Math.fma(mat.m12, this.y, Math.fma(mat.m22, this.z, mat.m32)));
        dest.x = rx;
        dest.y = ry;
        dest.z = rz;
        return dest;
    }

    public Vector3d mulPosition(Matrix4f mat, Vector3d dest) {
        double rx = Math.fma(mat.m00, this.x, Math.fma(mat.m10, this.y, Math.fma(mat.m20, this.z, mat.m30)));
        double ry = Math.fma(mat.m01, this.x, Math.fma(mat.m11, this.y, Math.fma(mat.m21, this.z, mat.m31)));
        double rz = Math.fma(mat.m02, this.x, Math.fma(mat.m12, this.y, Math.fma(mat.m22, this.z, mat.m32)));
        dest.x = rx;
        dest.y = ry;
        dest.z = rz;
        return dest;
    }

    public Vector3d mulPosition(Matrix4x3d mat, Vector3d dest) {
        double rx = Math.fma(mat.m00, this.x, Math.fma(mat.m10, this.y, Math.fma(mat.m20, this.z, mat.m30)));
        double ry = Math.fma(mat.m01, this.x, Math.fma(mat.m11, this.y, Math.fma(mat.m21, this.z, mat.m31)));
        double rz = Math.fma(mat.m02, this.x, Math.fma(mat.m12, this.y, Math.fma(mat.m22, this.z, mat.m32)));
        dest.x = rx;
        dest.y = ry;
        dest.z = rz;
        return dest;
    }

    public Vector3d mulPosition(Matrix4x3f mat, Vector3d dest) {
        double rx = Math.fma(mat.m00, this.x, Math.fma(mat.m10, this.y, Math.fma(mat.m20, this.z, mat.m30)));
        double ry = Math.fma(mat.m01, this.x, Math.fma(mat.m11, this.y, Math.fma(mat.m21, this.z, mat.m31)));
        double rz = Math.fma(mat.m02, this.x, Math.fma(mat.m12, this.y, Math.fma(mat.m22, this.z, mat.m32)));
        dest.x = rx;
        dest.y = ry;
        dest.z = rz;
        return dest;
    }

    public Vector3d mulTransposePosition(Matrix4d mat) {
        double rx = Math.fma(mat.m00, this.x, Math.fma(mat.m01, this.y, Math.fma(mat.m02, this.z, mat.m03)));
        double ry = Math.fma(mat.m10, this.x, Math.fma(mat.m11, this.y, Math.fma(mat.m12, this.z, mat.m13)));
        double rz = Math.fma(mat.m20, this.x, Math.fma(mat.m21, this.y, Math.fma(mat.m22, this.z, mat.m23)));
        this.x = rx;
        this.y = ry;
        this.z = rz;
        return this;
    }

    public Vector3d mulTransposePosition(Matrix4d mat, Vector3d dest) {
        double rx = Math.fma(mat.m00, this.x, Math.fma(mat.m01, this.y, Math.fma(mat.m02, this.z, mat.m03)));
        double ry = Math.fma(mat.m10, this.x, Math.fma(mat.m11, this.y, Math.fma(mat.m12, this.z, mat.m13)));
        double rz = Math.fma(mat.m20, this.x, Math.fma(mat.m21, this.y, Math.fma(mat.m22, this.z, mat.m23)));
        dest.x = rx;
        dest.y = ry;
        dest.z = rz;
        return dest;
    }

    public Vector3d mulTransposePosition(Matrix4f mat) {
        double rx = Math.fma(mat.m00, this.x, Math.fma(mat.m01, this.y, Math.fma(mat.m02, this.z, mat.m03)));
        double ry = Math.fma(mat.m10, this.x, Math.fma(mat.m11, this.y, Math.fma(mat.m12, this.z, mat.m13)));
        double rz = Math.fma(mat.m20, this.x, Math.fma(mat.m21, this.y, Math.fma(mat.m22, this.z, mat.m23)));
        this.x = rx;
        this.y = ry;
        this.z = rz;
        return this;
    }

    public Vector3d mulTransposePosition(Matrix4f mat, Vector3d dest) {
        double rx = Math.fma(mat.m00, this.x, Math.fma(mat.m01, this.y, Math.fma(mat.m02, this.z, mat.m03)));
        double ry = Math.fma(mat.m10, this.x, Math.fma(mat.m11, this.y, Math.fma(mat.m12, this.z, mat.m13)));
        double rz = Math.fma(mat.m20, this.x, Math.fma(mat.m21, this.y, Math.fma(mat.m22, this.z, mat.m23)));
        dest.x = rx;
        dest.y = ry;
        dest.z = rz;
        return dest;
    }

    public double mulPositionW(Matrix4f mat) {
        double w = Math.fma(mat.m03, this.x, Math.fma(mat.m13, this.y, Math.fma(mat.m23, this.z, mat.m33)));
        double rx = Math.fma(mat.m00, this.x, Math.fma(mat.m10, this.y, Math.fma(mat.m20, this.z, mat.m30)));
        double ry = Math.fma(mat.m01, this.x, Math.fma(mat.m11, this.y, Math.fma(mat.m21, this.z, mat.m31)));
        double rz = Math.fma(mat.m02, this.x, Math.fma(mat.m12, this.y, Math.fma(mat.m22, this.z, mat.m32)));
        this.x = rx;
        this.y = ry;
        this.z = rz;
        return w;
    }

    public double mulPositionW(Matrix4f mat, Vector3d dest) {
        double w = Math.fma(mat.m03, this.x, Math.fma(mat.m13, this.y, Math.fma(mat.m23, this.z, mat.m33)));
        double rx = Math.fma(mat.m00, this.x, Math.fma(mat.m10, this.y, Math.fma(mat.m20, this.z, mat.m30)));
        double ry = Math.fma(mat.m01, this.x, Math.fma(mat.m11, this.y, Math.fma(mat.m21, this.z, mat.m31)));
        double rz = Math.fma(mat.m02, this.x, Math.fma(mat.m12, this.y, Math.fma(mat.m22, this.z, mat.m32)));
        dest.x = rx;
        dest.y = ry;
        dest.z = rz;
        return w;
    }

    public double mulPositionW(Matrix4d mat) {
        double w = Math.fma(mat.m03, this.x, Math.fma(mat.m13, this.y, Math.fma(mat.m23, this.z, mat.m33)));
        double rx = Math.fma(mat.m00, this.x, Math.fma(mat.m10, this.y, Math.fma(mat.m20, this.z, mat.m30)));
        double ry = Math.fma(mat.m01, this.x, Math.fma(mat.m11, this.y, Math.fma(mat.m21, this.z, mat.m31)));
        double rz = Math.fma(mat.m02, this.x, Math.fma(mat.m12, this.y, Math.fma(mat.m22, this.z, mat.m32)));
        this.x = rx;
        this.y = ry;
        this.z = rz;
        return w;
    }

    public double mulPositionW(Matrix4d mat, Vector3d dest) {
        double w = Math.fma(mat.m03, this.x, Math.fma(mat.m13, this.y, Math.fma(mat.m23, this.z, mat.m33)));
        double rx = Math.fma(mat.m00, this.x, Math.fma(mat.m10, this.y, Math.fma(mat.m20, this.z, mat.m30)));
        double ry = Math.fma(mat.m01, this.x, Math.fma(mat.m11, this.y, Math.fma(mat.m21, this.z, mat.m31)));
        double rz = Math.fma(mat.m02, this.x, Math.fma(mat.m12, this.y, Math.fma(mat.m22, this.z, mat.m32)));
        dest.x = rx;
        dest.y = ry;
        dest.z = rz;
        return w;
    }

    public Vector3d mulDirection(Matrix4f mat) {
        double rx = Math.fma(mat.m00, this.x, Math.fma(mat.m10, this.y, (double) mat.m20 * this.z));
        double ry = Math.fma(mat.m01, this.x, Math.fma(mat.m11, this.y, (double) mat.m21 * this.z));
        double rz = Math.fma(mat.m02, this.x, Math.fma(mat.m12, this.y, (double) mat.m22 * this.z));
        this.x = rx;
        this.y = ry;
        this.z = rz;
        return this;
    }

    public Vector3d mulDirection(Matrix4d mat) {
        double rx = Math.fma(mat.m00, this.x, Math.fma(mat.m10, this.y, mat.m20 * this.z));
        double ry = Math.fma(mat.m01, this.x, Math.fma(mat.m11, this.y, mat.m21 * this.z));
        double rz = Math.fma(mat.m02, this.x, Math.fma(mat.m12, this.y, mat.m22 * this.z));
        this.x = rx;
        this.y = ry;
        this.z = rz;
        return this;
    }

    public Vector3d mulDirection(Matrix4x3d mat) {
        double rx = Math.fma(mat.m00, this.x, Math.fma(mat.m10, this.y, mat.m20 * this.z));
        double ry = Math.fma(mat.m01, this.x, Math.fma(mat.m11, this.y, mat.m21 * this.z));
        double rz = Math.fma(mat.m02, this.x, Math.fma(mat.m12, this.y, mat.m22 * this.z));
        this.x = rx;
        this.y = ry;
        this.z = rz;
        return this;
    }

    public Vector3d mulDirection(Matrix4x3f mat) {
        double rx = Math.fma(mat.m00, this.x, Math.fma(mat.m10, this.y, (double) mat.m20 * this.z));
        double ry = Math.fma(mat.m01, this.x, Math.fma(mat.m11, this.y, (double) mat.m21 * this.z));
        double rz = Math.fma(mat.m02, this.x, Math.fma(mat.m12, this.y, (double) mat.m22 * this.z));
        this.x = rx;
        this.y = ry;
        this.z = rz;
        return this;
    }

    public Vector3d mulDirection(Matrix4d mat, Vector3d dest) {
        double rx = Math.fma(mat.m00, this.x, Math.fma(mat.m10, this.y, mat.m20 * this.z));
        double ry = Math.fma(mat.m01, this.x, Math.fma(mat.m11, this.y, mat.m21 * this.z));
        double rz = Math.fma(mat.m02, this.x, Math.fma(mat.m12, this.y, mat.m22 * this.z));
        dest.x = rx;
        dest.y = ry;
        dest.z = rz;
        return dest;
    }

    public Vector3d mulDirection(Matrix4f mat, Vector3d dest) {
        double rx = Math.fma(mat.m00, this.x, Math.fma(mat.m10, this.y, (double) mat.m20 * this.z));
        double ry = Math.fma(mat.m01, this.x, Math.fma(mat.m11, this.y, (double) mat.m21 * this.z));
        double rz = Math.fma(mat.m02, this.x, Math.fma(mat.m12, this.y, (double) mat.m22 * this.z));
        dest.x = rx;
        dest.y = ry;
        dest.z = rz;
        return dest;
    }

    public Vector3d mulDirection(Matrix4x3d mat, Vector3d dest) {
        double rx = Math.fma(mat.m00, this.x, Math.fma(mat.m10, this.y, mat.m20 * this.z));
        double ry = Math.fma(mat.m01, this.x, Math.fma(mat.m11, this.y, mat.m21 * this.z));
        double rz = Math.fma(mat.m02, this.x, Math.fma(mat.m12, this.y, mat.m22 * this.z));
        dest.x = rx;
        dest.y = ry;
        dest.z = rz;
        return dest;
    }

    public Vector3d mulDirection(Matrix4x3f mat, Vector3d dest) {
        double rx = Math.fma(mat.m00, this.x, Math.fma(mat.m10, this.y, (double) mat.m20 * this.z));
        double ry = Math.fma(mat.m01, this.x, Math.fma(mat.m11, this.y, (double) mat.m21 * this.z));
        double rz = Math.fma(mat.m02, this.x, Math.fma(mat.m12, this.y, (double) mat.m22 * this.z));
        dest.x = rx;
        dest.y = ry;
        dest.z = rz;
        return dest;
    }

    public Vector3d mulTransposeDirection(Matrix4d mat) {
        double rx = Math.fma(mat.m00, this.x, Math.fma(mat.m01, this.y, mat.m02 * this.z));
        double ry = Math.fma(mat.m10, this.x, Math.fma(mat.m11, this.y, mat.m12 * this.z));
        double rz = Math.fma(mat.m20, this.x, Math.fma(mat.m21, this.y, mat.m22 * this.z));
        this.x = rx;
        this.y = ry;
        this.z = rz;
        return this;
    }

    public Vector3d mulTransposeDirection(Matrix4d mat, Vector3d dest) {
        double rx = Math.fma(mat.m00, this.x, Math.fma(mat.m01, this.y, mat.m02 * this.z));
        double ry = Math.fma(mat.m10, this.x, Math.fma(mat.m11, this.y, mat.m12 * this.z));
        double rz = Math.fma(mat.m20, this.x, Math.fma(mat.m21, this.y, mat.m22 * this.z));
        dest.x = rx;
        dest.y = ry;
        dest.z = rz;
        return dest;
    }

    public Vector3d mulTransposeDirection(Matrix4f mat) {
        double rx = Math.fma(mat.m00, this.x, Math.fma(mat.m01, this.y, (double) mat.m02 * this.z));
        double ry = Math.fma(mat.m10, this.x, Math.fma(mat.m11, this.y, (double) mat.m12 * this.z));
        double rz = Math.fma(mat.m20, this.x, Math.fma(mat.m21, this.y, (double) mat.m22 * this.z));
        this.x = rx;
        this.y = ry;
        this.z = rz;
        return this;
    }

    public Vector3d mulTransposeDirection(Matrix4f mat, Vector3d dest) {
        double rx = Math.fma(mat.m00, this.x, Math.fma(mat.m01, this.y, (double) mat.m02 * this.z));
        double ry = Math.fma(mat.m10, this.x, Math.fma(mat.m11, this.y, (double) mat.m12 * this.z));
        double rz = Math.fma(mat.m20, this.x, Math.fma(mat.m21, this.y, (double) mat.m22 * this.z));
        dest.x = rx;
        dest.y = ry;
        dest.z = rz;
        return dest;
    }

    public Vector3d mul(double scalar) {
        this.x *= scalar;
        this.y *= scalar;
        this.z *= scalar;
        return this;
    }

    public Vector3d mul(double scalar, Vector3d dest) {
        dest.x = this.x * scalar;
        dest.y = this.y * scalar;
        dest.z = this.z * scalar;
        return dest;
    }

    public Vector3d mul(double x, double y, double z) {
        this.x *= x;
        this.y *= y;
        this.z *= z;
        return this;
    }

    public Vector3d mul(double x, double y, double z, Vector3d dest) {
        dest.x = this.x * x;
        dest.y = this.y * y;
        dest.z = this.z * z;
        return dest;
    }

    public Vector3d rotate(Quaterniond quat) {
        return quat.transform(this, this);
    }

    public Vector3d rotate(Quaterniond quat, Vector3d dest) {
        return quat.transform(this, dest);
    }

    public Quaterniond rotationTo(Vector3d toDir, Quaterniond dest) {
        return dest.rotationTo(this, toDir);
    }

    public Quaterniond rotationTo(double toDirX, double toDirY, double toDirZ, Quaterniond dest) {
        return dest.rotationTo(this.x, this.y, this.z, toDirX, toDirY, toDirZ);
    }

    public Vector3d rotateAxis(double angle, double x, double y, double z) {
        if (y == 0.0 && z == 0.0 && Math.absEqualsOne(x)) {
            return this.rotateX(x * angle, this);
        } else if (x == 0.0 && z == 0.0 && Math.absEqualsOne(y)) {
            return this.rotateY(y * angle, this);
        } else {
            return x == 0.0 && y == 0.0 && Math.absEqualsOne(z) ? this.rotateZ(z * angle, this) : this.rotateAxisInternal(angle, x, y, z, this);
        }
    }

    public Vector3d rotateAxis(double angle, double aX, double aY, double aZ, Vector3d dest) {
        if (aY == 0.0 && aZ == 0.0 && Math.absEqualsOne(aX)) {
            return this.rotateX(aX * angle, dest);
        } else if (aX == 0.0 && aZ == 0.0 && Math.absEqualsOne(aY)) {
            return this.rotateY(aY * angle, dest);
        } else {
            return aX == 0.0 && aY == 0.0 && Math.absEqualsOne(aZ) ? this.rotateZ(aZ * angle, dest) : this.rotateAxisInternal(angle, aX, aY, aZ, dest);
        }
    }

    private Vector3d rotateAxisInternal(double angle, double aX, double aY, double aZ, Vector3d dest) {
        double halfAngle = angle * 0.5;
        double sinAngle = Math.sin(halfAngle);
        double qx = aX * sinAngle;
        double qy = aY * sinAngle;
        double qz = aZ * sinAngle;
        double qw = Math.cosFromSin(sinAngle, halfAngle);
        double w2 = qw * qw;
        double x2 = qx * qx;
        double y2 = qy * qy;
        double z2 = qz * qz;
        double zw = qz * qw;
        double xy = qx * qy;
        double xz = qx * qz;
        double yw = qy * qw;
        double yz = qy * qz;
        double xw = qx * qw;
        double nx = (w2 + x2 - z2 - y2) * this.x + (-zw + xy - zw + xy) * this.y + (yw + xz + xz + yw) * this.z;
        double ny = (xy + zw + zw + xy) * this.x + (y2 - z2 + w2 - x2) * this.y + (yz + yz - xw - xw) * this.z;
        double nz = (xz - yw + xz - yw) * this.x + (yz + yz + xw + xw) * this.y + (z2 - y2 - x2 + w2) * this.z;
        dest.x = nx;
        dest.y = ny;
        dest.z = nz;
        return dest;
    }

    public Vector3d rotateX(double angle) {
        double sin = Math.sin(angle);
        double cos = Math.cosFromSin(sin, angle);
        double y = this.y * cos - this.z * sin;
        double z = this.y * sin + this.z * cos;
        this.y = y;
        this.z = z;
        return this;
    }

    public Vector3d rotateX(double angle, Vector3d dest) {
        double sin = Math.sin(angle);
        double cos = Math.cosFromSin(sin, angle);
        double y = this.y * cos - this.z * sin;
        double z = this.y * sin + this.z * cos;
        dest.x = this.x;
        dest.y = y;
        dest.z = z;
        return dest;
    }

    public Vector3d rotateY(double angle) {
        double sin = Math.sin(angle);
        double cos = Math.cosFromSin(sin, angle);
        double x = this.x * cos + this.z * sin;
        double z = -this.x * sin + this.z * cos;
        this.x = x;
        this.z = z;
        return this;
    }

    public Vector3d rotateY(double angle, Vector3d dest) {
        double sin = Math.sin(angle);
        double cos = Math.cosFromSin(sin, angle);
        double x = this.x * cos + this.z * sin;
        double z = -this.x * sin + this.z * cos;
        dest.x = x;
        dest.y = this.y;
        dest.z = z;
        return dest;
    }

    public Vector3d rotateZ(double angle) {
        double sin = Math.sin(angle);
        double cos = Math.cosFromSin(sin, angle);
        double x = this.x * cos - this.y * sin;
        double y = this.x * sin + this.y * cos;
        this.x = x;
        this.y = y;
        return this;
    }

    public Vector3d rotateZ(double angle, Vector3d dest) {
        double sin = Math.sin(angle);
        double cos = Math.cosFromSin(sin, angle);
        double x = this.x * cos - this.y * sin;
        double y = this.x * sin + this.y * cos;
        dest.x = x;
        dest.y = y;
        dest.z = this.z;
        return dest;
    }

    public Vector3d div(double scalar) {
        double inv = 1.0 / scalar;
        this.x *= inv;
        this.y *= inv;
        this.z *= inv;
        return this;
    }

    public Vector3d div(double scalar, Vector3d dest) {
        double inv = 1.0 / scalar;
        dest.x = this.x * inv;
        dest.y = this.y * inv;
        dest.z = this.z * inv;
        return dest;
    }

    public Vector3d div(double x, double y, double z) {
        this.x /= x;
        this.y /= y;
        this.z /= z;
        return this;
    }

    public Vector3d div(double x, double y, double z, Vector3d dest) {
        dest.x = this.x / x;
        dest.y = this.y / y;
        dest.z = this.z / z;
        return dest;
    }

    public double lengthSquared() {
        return Math.fma(this.x, this.x, Math.fma(this.y, this.y, this.z * this.z));
    }

    public static double lengthSquared(double x, double y, double z) {
        return Math.fma(x, x, Math.fma(y, y, z * z));
    }

    public double length() {
        return Math.sqrt(Math.fma(this.x, this.x, Math.fma(this.y, this.y, this.z * this.z)));
    }

    public static double length(double x, double y, double z) {
        return Math.sqrt(Math.fma(x, x, Math.fma(y, y, z * z)));
    }

    public Vector3d normalize() {
        double invLength = Math.invsqrt(Math.fma(this.x, this.x, Math.fma(this.y, this.y, this.z * this.z)));
        this.x *= invLength;
        this.y *= invLength;
        this.z *= invLength;
        return this;
    }

    public Vector3d normalize(Vector3d dest) {
        double invLength = Math.invsqrt(Math.fma(this.x, this.x, Math.fma(this.y, this.y, this.z * this.z)));
        dest.x = this.x * invLength;
        dest.y = this.y * invLength;
        dest.z = this.z * invLength;
        return dest;
    }

    public Vector3d normalize(double length) {
        double invLength = Math.invsqrt(Math.fma(this.x, this.x, Math.fma(this.y, this.y, this.z * this.z))) * length;
        this.x *= invLength;
        this.y *= invLength;
        this.z *= invLength;
        return this;
    }

    public Vector3d normalize(double length, Vector3d dest) {
        double invLength = Math.invsqrt(Math.fma(this.x, this.x, Math.fma(this.y, this.y, this.z * this.z))) * length;
        dest.x = this.x * invLength;
        dest.y = this.y * invLength;
        dest.z = this.z * invLength;
        return dest;
    }

    public Vector3d cross(Vector3d v) {
        double rx = Math.fma(this.y, v.z, -this.z * v.y);
        double ry = Math.fma(this.z, v.x, -this.x * v.z);
        double rz = Math.fma(this.x, v.y, -this.y * v.x);
        this.x = rx;
        this.y = ry;
        this.z = rz;
        return this;
    }

    public Vector3d cross(double x, double y, double z) {
        double rx = Math.fma(this.y, z, -this.z * y);
        double ry = Math.fma(this.z, x, -this.x * z);
        double rz = Math.fma(this.x, y, -this.y * x);
        this.x = rx;
        this.y = ry;
        this.z = rz;
        return this;
    }

    public Vector3d cross(Vector3d v, Vector3d dest) {
        double rx = Math.fma(this.y, v.z, -this.z * v.y);
        double ry = Math.fma(this.z, v.x, -this.x * v.z);
        double rz = Math.fma(this.x, v.y, -this.y * v.x);
        dest.x = rx;
        dest.y = ry;
        dest.z = rz;
        return dest;
    }

    public Vector3d cross(double x, double y, double z, Vector3d dest) {
        double rx = Math.fma(this.y, z, -this.z * y);
        double ry = Math.fma(this.z, x, -this.x * z);
        double rz = Math.fma(this.x, y, -this.y * x);
        dest.x = rx;
        dest.y = ry;
        dest.z = rz;
        return dest;
    }

    public double distance(Vector3d v) {
        double dx = this.x - v.x;
        double dy = this.y - v.y;
        double dz = this.z - v.z;
        return Math.sqrt(Math.fma(dx, dx, Math.fma(dy, dy, dz * dz)));
    }

    public double distance(double x, double y, double z) {
        double dx = this.x - x;
        double dy = this.y - y;
        double dz = this.z - z;
        return Math.sqrt(Math.fma(dx, dx, Math.fma(dy, dy, dz * dz)));
    }

    public double distanceSquared(Vector3d v) {
        double dx = this.x - v.x;
        double dy = this.y - v.y;
        double dz = this.z - v.z;
        return Math.fma(dx, dx, Math.fma(dy, dy, dz * dz));
    }

    public double distanceSquared(double x, double y, double z) {
        double dx = this.x - x;
        double dy = this.y - y;
        double dz = this.z - z;
        return Math.fma(dx, dx, Math.fma(dy, dy, dz * dz));
    }

    public static double distance(double x1, double y1, double z1, double x2, double y2, double z2) {
        return Math.sqrt(distanceSquared(x1, y1, z1, x2, y2, z2));
    }

    public static double distanceSquared(double x1, double y1, double z1, double x2, double y2, double z2) {
        double dx = x1 - x2;
        double dy = y1 - y2;
        double dz = z1 - z2;
        return Math.fma(dx, dx, Math.fma(dy, dy, dz * dz));
    }

    public double dot(Vector3d v) {
        return Math.fma(this.x, v.x, Math.fma(this.y, v.y, this.z * v.z));
    }

    public double dot(double x, double y, double z) {
        return Math.fma(this.x, x, Math.fma(this.y, y, this.z * z));
    }

    public double angleCos(Vector3d v) {
        double length1Squared = Math.fma(this.x, this.x, Math.fma(this.y, this.y, this.z * this.z));
        double length2Squared = Math.fma(v.x, v.x, Math.fma(v.y, v.y, v.z * v.z));
        double dot = Math.fma(this.x, v.x, Math.fma(this.y, v.y, this.z * v.z));
        return dot / Math.sqrt(length1Squared * length2Squared);
    }

    public double angle(Vector3d v) {
        double cos = this.angleCos(v);
        cos = java.lang.Math.min(cos, 1.0);
        cos = java.lang.Math.max(cos, -1.0);
        return Math.acos(cos);
    }

    public double angleSigned(Vector3d v, Vector3d n) {
        double x = v.x;
        double y = v.y;
        double z = v.z;
        return Math.atan2((this.y * z - this.z * y) * n.x + (this.z * x - this.x * z) * n.y + (this.x * y - this.y * x) * n.z, this.x * x + this.y * y + this.z * z);
    }

    public double angleSigned(double x, double y, double z, double nx, double ny, double nz) {
        return Math.atan2((this.y * z - this.z * y) * nx + (this.z * x - this.x * z) * ny + (this.x * y - this.y * x) * nz, this.x * x + this.y * y + this.z * z);
    }

    public Vector3d min(Vector3d v) {
        this.x = java.lang.Math.min(this.x, v.x);
        this.y = java.lang.Math.min(this.y, v.y);
        this.z = java.lang.Math.min(this.z, v.z);
        return this;
    }

    public Vector3d min(Vector3d v, Vector3d dest) {
        dest.x = java.lang.Math.min(this.x, v.x);
        dest.y = java.lang.Math.min(this.y, v.y);
        dest.z = java.lang.Math.min(this.z, v.z);
        return dest;
    }

    public Vector3d max(Vector3d v) {
        this.x = java.lang.Math.max(this.x, v.x);
        this.y = java.lang.Math.max(this.y, v.y);
        this.z = java.lang.Math.max(this.z, v.z);
        return this;
    }

    public Vector3d max(Vector3d v, Vector3d dest) {
        dest.x = java.lang.Math.max(this.x, v.x);
        dest.y = java.lang.Math.max(this.y, v.y);
        dest.z = java.lang.Math.max(this.z, v.z);
        return dest;
    }

    public Vector3d zero() {
        this.x = 0.0;
        this.y = 0.0;
        this.z = 0.0;
        return this;
    }

    public String toString() {
        return "(" + x + "," + y + "," + z + ")";
    }

    public Vector3d negate() {
        this.x = -this.x;
        this.y = -this.y;
        this.z = -this.z;
        return this;
    }

    public Vector3d negate(Vector3d dest) {
        dest.x = -this.x;
        dest.y = -this.y;
        dest.z = -this.z;
        return dest;
    }

    public Vector3d absolute() {
        this.x = Math.abs(this.x);
        this.y = Math.abs(this.y);
        this.z = Math.abs(this.z);
        return this;
    }

    public Vector3d absolute(Vector3d dest) {
        dest.x = Math.abs(this.x);
        dest.y = Math.abs(this.y);
        dest.z = Math.abs(this.z);
        return dest;
    }

    public int hashCode() {
        int result = 1;
        long temp = Double.doubleToLongBits(this.x);
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
            Vector3d other = (Vector3d) obj;
            if (Double.doubleToLongBits(this.x) != Double.doubleToLongBits(other.x)) {
                return false;
            } else if (Double.doubleToLongBits(this.y) != Double.doubleToLongBits(other.y)) {
                return false;
            } else {
                return Double.doubleToLongBits(this.z) == Double.doubleToLongBits(other.z);
            }
        }
    }

    public boolean equals(Vector3d v, double delta) {
        if (this == v) {
            return true;
        } else if (v == null) {
            return false;
        } else if (!Runtime.equals(this.x, v.x, delta)) {
            return false;
        } else if (!Runtime.equals(this.y, v.y, delta)) {
            return false;
        } else {
            return Runtime.equals(this.z, v.z, delta);
        }
    }

    public boolean equals(double x, double y, double z) {
        if (Double.doubleToLongBits(this.x) != Double.doubleToLongBits(x)) {
            return false;
        } else if (Double.doubleToLongBits(this.y) != Double.doubleToLongBits(y)) {
            return false;
        } else {
            return Double.doubleToLongBits(this.z) == Double.doubleToLongBits(z);
        }
    }

    public Vector3d reflect(Vector3d normal) {
        double x = normal.x;
        double y = normal.y;
        double z = normal.z;
        double dot = Math.fma(this.x, x, Math.fma(this.y, y, this.z * z));
        this.x -= (dot + dot) * x;
        this.y -= (dot + dot) * y;
        this.z -= (dot + dot) * z;
        return this;
    }

    public Vector3d reflect(double x, double y, double z) {
        double dot = Math.fma(this.x, x, Math.fma(this.y, y, this.z * z));
        this.x -= (dot + dot) * x;
        this.y -= (dot + dot) * y;
        this.z -= (dot + dot) * z;
        return this;
    }

    public Vector3d reflect(Vector3d normal, Vector3d dest) {
        double x = normal.x;
        double y = normal.y;
        double z = normal.z;
        double dot = Math.fma(this.x, x, Math.fma(this.y, y, this.z * z));
        dest.x = this.x - (dot + dot) * x;
        dest.y = this.y - (dot + dot) * y;
        dest.z = this.z - (dot + dot) * z;
        return dest;
    }

    public Vector3d reflect(double x, double y, double z, Vector3d dest) {
        double dot = Math.fma(this.x, x, Math.fma(this.y, y, this.z * z));
        dest.x = this.x - (dot + dot) * x;
        dest.y = this.y - (dot + dot) * y;
        dest.z = this.z - (dot + dot) * z;
        return dest;
    }

    public Vector3d half(Vector3d other) {
        return this.set(this).add(other.x, other.y, other.z).normalize();
    }

    public Vector3d half(double x, double y, double z) {
        return this.set(this).add(x, y, z).normalize();
    }

    public Vector3d half(Vector3d other, Vector3d dest) {
        return dest.set(this).add(other.x, other.y, other.z).normalize();
    }

    public Vector3d half(double x, double y, double z, Vector3d dest) {
        return dest.set(this).add(x, y, z).normalize();
    }

    public Vector3d smoothStep(Vector3d v, double t, Vector3d dest) {
        double t2 = t * t;
        double t3 = t2 * t;
        dest.x = (this.x + this.x - v.x - v.x) * t3 + (3.0 * v.x - 3.0 * this.x) * t2 + this.x * t + this.x;
        dest.y = (this.y + this.y - v.y - v.y) * t3 + (3.0 * v.y - 3.0 * this.y) * t2 + this.y * t + this.y;
        dest.z = (this.z + this.z - v.z - v.z) * t3 + (3.0 * v.z - 3.0 * this.z) * t2 + this.z * t + this.z;
        return dest;
    }

    public Vector3d hermite(Vector3d t0, Vector3d v1, Vector3d t1, double t, Vector3d dest) {
        double t2 = t * t;
        double t3 = t2 * t;
        dest.x = (this.x + this.x - v1.x - v1.x + t1.x + t0.x) * t3 + (3.0 * v1.x - 3.0 * this.x - t0.x - t0.x - t1.x) * t2 + this.x * t + this.x;
        dest.y = (this.y + this.y - v1.y - v1.y + t1.y + t0.y) * t3 + (3.0 * v1.y - 3.0 * this.y - t0.y - t0.y - t1.y) * t2 + this.y * t + this.y;
        dest.z = (this.z + this.z - v1.z - v1.z + t1.z + t0.z) * t3 + (3.0 * v1.z - 3.0 * this.z - t0.z - t0.z - t1.z) * t2 + this.z * t + this.z;
        return dest;
    }

    public Vector3d lerp(Vector3d other, double t) {
        this.x = Math.fma(other.x - this.x, t, this.x);
        this.y = Math.fma(other.y - this.y, t, this.y);
        this.z = Math.fma(other.z - this.z, t, this.z);
        return this;
    }

    public Vector3d lerp(Vector3d other, double t, Vector3d dest) {
        dest.x = Math.fma(other.x - this.x, t, this.x);
        dest.y = Math.fma(other.y - this.y, t, this.y);
        dest.z = Math.fma(other.z - this.z, t, this.z);
        return dest;
    }

    public double get(int component) throws IllegalArgumentException {
        switch (component) {
            case 0:
                return this.x;
            case 1:
                return this.y;
            case 2:
                return this.z;
            default:
                throw new IllegalArgumentException();
        }
    }

    public Vector3i get(int mode, Vector3i dest) {
        dest.x = Math.roundUsing(this.x, mode);
        dest.y = Math.roundUsing(this.y, mode);
        dest.z = Math.roundUsing(this.z, mode);
        return dest;
    }

    public Vector3f get(Vector3f dest) {
        dest.x = (float) this.x;
        dest.y = (float) this.y;
        dest.z = (float) this.z;
        return dest;
    }

    public Vector3d get(Vector3d dest) {
        dest.x = this.x;
        dest.y = this.y;
        dest.z = this.z;
        return dest;
    }

    public int maxComponent() {
        double absX = Math.abs(this.x);
        double absY = Math.abs(this.y);
        double absZ = Math.abs(this.z);
        if (absX >= absY && absX >= absZ) {
            return 0;
        } else {
            return absY >= absZ ? 1 : 2;
        }
    }

    public int minComponent() {
        double absX = Math.abs(this.x);
        double absY = Math.abs(this.y);
        double absZ = Math.abs(this.z);
        if (absX < absY && absX < absZ) {
            return 0;
        } else {
            return absY < absZ ? 1 : 2;
        }
    }

    public Vector3d orthogonalize(Vector3d v, Vector3d dest) {
        double rx;
        double ry;
        double rz;
        if (Math.abs(v.x) > Math.abs(v.z)) {
            rx = -v.y;
            ry = v.x;
            rz = 0.0;
        } else {
            rx = 0.0;
            ry = -v.z;
            rz = v.y;
        }

        double invLen = Math.invsqrt(rx * rx + ry * ry + rz * rz);
        dest.x = rx * invLen;
        dest.y = ry * invLen;
        dest.z = rz * invLen;
        return dest;
    }

    public Vector3d orthogonalize(Vector3d v) {
        return this.orthogonalize(v, this);
    }

    public Vector3d orthogonalizeUnit(Vector3d v, Vector3d dest) {
        return this.orthogonalize(v, dest);
    }

    public Vector3d orthogonalizeUnit(Vector3d v) {
        return this.orthogonalizeUnit(v, this);
    }

    public Vector3d floor() {
        this.x = Math.floor(this.x);
        this.y = Math.floor(this.y);
        this.z = Math.floor(this.z);
        return this;
    }

    public Vector3d floor(Vector3d dest) {
        dest.x = Math.floor(this.x);
        dest.y = Math.floor(this.y);
        dest.z = Math.floor(this.z);
        return dest;
    }

    public Vector3d ceil() {
        this.x = Math.ceil(this.x);
        this.y = Math.ceil(this.y);
        this.z = Math.ceil(this.z);
        return this;
    }

    public Vector3d ceil(Vector3d dest) {
        dest.x = Math.ceil(this.x);
        dest.y = Math.ceil(this.y);
        dest.z = Math.ceil(this.z);
        return dest;
    }

    public Vector3d round() {
        this.x = (double) Math.round(this.x);
        this.y = (double) Math.round(this.y);
        this.z = (double) Math.round(this.z);
        return this;
    }

    public Vector3d round(Vector3d dest) {
        dest.x = (double) Math.round(this.x);
        dest.y = (double) Math.round(this.y);
        dest.z = (double) Math.round(this.z);
        return dest;
    }

    public boolean isFinite() {
        return Math.isFinite(this.x) && Math.isFinite(this.y) && Math.isFinite(this.z);
    }

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
