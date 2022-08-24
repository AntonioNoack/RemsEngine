package org.joml;

@SuppressWarnings("unused")
public class Vector3f {

    public float x;
    public float y;
    public float z;

    public Vector3f() {
    }

    public Vector3f(float d) {
        this.x = d;
        this.y = d;
        this.z = d;
    }

    public Vector3f(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Vector3f(Vector3f v) {
        this.x = v.x;
        this.y = v.y;
        this.z = v.z;
    }

    public Vector3f(Vector3i v) {
        this.x = (float) v.x;
        this.y = (float) v.y;
        this.z = (float) v.z;
    }

    public Vector3f(Vector2f v, float z) {
        this.x = v.x;
        this.y = v.y;
        this.z = z;
    }

    public Vector3f(Vector2i v, float z) {
        this.x = (float) v.x;
        this.y = (float) v.y;
        this.z = z;
    }

    public Vector3f(float[] xyz) {
        this.x = xyz[0];
        this.y = xyz[1];
        this.z = xyz[2];
    }

    public Vector3f set(Vector3f v) {
        this.x = v.x;
        this.y = v.y;
        this.z = v.z;
        return this;
    }

    public Vector3f set(Vector3d v) {
        this.x = (float) v.x;
        this.y = (float) v.y;
        this.z = (float) v.z;
        return this;
    }

    public Vector3f set(Vector3i v) {
        this.x = (float) v.x;
        this.y = (float) v.y;
        this.z = (float) v.z;
        return this;
    }

    public Vector3f set(Vector2f v, float z) {
        this.x = v.x;
        this.y = v.y;
        this.z = z;
        return this;
    }

    public Vector3f set(Vector2d v, float z) {
        this.x = (float) v.x;
        this.y = (float) v.y;
        this.z = z;
        return this;
    }

    public Vector3f set(Vector2i v, float z) {
        this.x = (float) v.x;
        this.y = (float) v.y;
        this.z = z;
        return this;
    }

    public Vector3f set(float d) {
        this.x = d;
        this.y = d;
        this.z = d;
        return this;
    }

    public Vector3f set(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
        return this;
    }

    public Vector3f set(double d) {
        this.x = (float) d;
        this.y = (float) d;
        this.z = (float) d;
        return this;
    }

    public Vector3f set(double x, double y, double z) {
        this.x = (float) x;
        this.y = (float) y;
        this.z = (float) z;
        return this;
    }

    public Vector3f set(float[] xyz) {
        this.x = xyz[0];
        this.y = xyz[1];
        this.z = xyz[2];
        return this;
    }

    public Vector3f setComponent(int component, float value) throws IllegalArgumentException {
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

    public Vector3f sub(Vector3f v) {
        this.x -= v.x;
        this.y -= v.y;
        this.z -= v.z;
        return this;
    }

    public Vector3f sub(Vector3f v, Vector3f dest) {
        dest.x = this.x - v.x;
        dest.y = this.y - v.y;
        dest.z = this.z - v.z;
        return dest;
    }

    public Vector3f sub(float x, float y, float z) {
        this.x -= x;
        this.y -= y;
        this.z -= z;
        return this;
    }

    public Vector3f sub(float x, float y, float z, Vector3f dest) {
        dest.x = this.x - x;
        dest.y = this.y - y;
        dest.z = this.z - z;
        return dest;
    }

    public Vector3f add(Vector3f v) {
        this.x += v.x;
        this.y += v.y;
        this.z += v.z;
        return this;
    }

    public Vector3f add(Vector3f v, Vector3f dest) {
        dest.x = this.x + v.x;
        dest.y = this.y + v.y;
        dest.z = this.z + v.z;
        return dest;
    }

    public Vector3f add(float x, float y, float z) {
        this.x += x;
        this.y += y;
        this.z += z;
        return this;
    }

    public Vector3f add(float x, float y, float z, Vector3f dest) {
        dest.x = this.x + x;
        dest.y = this.y + y;
        dest.z = this.z + z;
        return dest;
    }

    public Vector3f fma(Vector3f a, Vector3f b) {
        this.x = Math.fma(a.x, b.x, this.x);
        this.y = Math.fma(a.y, b.y, this.y);
        this.z = Math.fma(a.z, b.z, this.z);
        return this;
    }

    public Vector3f fma(float a, Vector3f b) {
        this.x = Math.fma(a, b.x, this.x);
        this.y = Math.fma(a, b.y, this.y);
        this.z = Math.fma(a, b.z, this.z);
        return this;
    }

    public Vector3f fma(Vector3f a, Vector3f b, Vector3f dest) {
        dest.x = Math.fma(a.x, b.x, this.x);
        dest.y = Math.fma(a.y, b.y, this.y);
        dest.z = Math.fma(a.z, b.z, this.z);
        return dest;
    }

    public Vector3f fma(float a, Vector3f b, Vector3f dest) {
        dest.x = Math.fma(a, b.x, this.x);
        dest.y = Math.fma(a, b.y, this.y);
        dest.z = Math.fma(a, b.z, this.z);
        return dest;
    }

    public Vector3f mulAdd(Vector3f a, Vector3f b) {
        this.x = Math.fma(this.x, a.x, b.x);
        this.y = Math.fma(this.y, a.y, b.y);
        this.z = Math.fma(this.z, a.z, b.z);
        return this;
    }

    public Vector3f mulAdd(float a, Vector3f b) {
        this.x = Math.fma(this.x, a, b.x);
        this.y = Math.fma(this.y, a, b.y);
        this.z = Math.fma(this.z, a, b.z);
        return this;
    }

    public Vector3f mulAdd(Vector3f a, Vector3f b, Vector3f dest) {
        dest.x = Math.fma(this.x, a.x, b.x);
        dest.y = Math.fma(this.y, a.y, b.y);
        dest.z = Math.fma(this.z, a.z, b.z);
        return dest;
    }

    public Vector3f mulAdd(float a, Vector3f b, Vector3f dest) {
        dest.x = Math.fma(this.x, a, b.x);
        dest.y = Math.fma(this.y, a, b.y);
        dest.z = Math.fma(this.z, a, b.z);
        return dest;
    }

    public Vector3f mul(Vector3f v) {
        this.x *= v.x;
        this.y *= v.y;
        this.z *= v.z;
        return this;
    }

    public Vector3f mul(Vector3f v, Vector3f dest) {
        dest.x = this.x * v.x;
        dest.y = this.y * v.y;
        dest.z = this.z * v.z;
        return dest;
    }

    public Vector3f div(Vector3f v) {
        this.x /= v.x;
        this.y /= v.y;
        this.z /= v.z;
        return this;
    }

    public Vector3f div(Vector3f v, Vector3f dest) {
        dest.x = this.x / v.x;
        dest.y = this.y / v.y;
        dest.z = this.z / v.z;
        return dest;
    }

    public Vector3f mulProject(Matrix4f mat, Vector3f dest) {
        float x = this.x;
        float y = this.y;
        float z = this.z;
        float invW = 1f / Math.fma(mat.m03, x, Math.fma(mat.m13, y, Math.fma(mat.m23, z, mat.m33)));
        dest.x = Math.fma(mat.m00, x, Math.fma(mat.m10, y, Math.fma(mat.m20, z, mat.m30))) * invW;
        dest.y = Math.fma(mat.m01, x, Math.fma(mat.m11, y, Math.fma(mat.m21, z, mat.m31))) * invW;
        dest.z = Math.fma(mat.m02, x, Math.fma(mat.m12, y, Math.fma(mat.m22, z, mat.m32))) * invW;
        return dest;
    }

    public Vector3f mulProject(Matrix4f mat, float w, Vector3f dest) {
        float x = this.x;
        float y = this.y;
        float z = this.z;
        float invW = 1f / Math.fma(mat.m03, x, Math.fma(mat.m13, y, Math.fma(mat.m23, z, mat.m33 * w)));
        dest.x = Math.fma(mat.m00, x, Math.fma(mat.m10, y, Math.fma(mat.m20, z, mat.m30 * w))) * invW;
        dest.y = Math.fma(mat.m01, x, Math.fma(mat.m11, y, Math.fma(mat.m21, z, mat.m31 * w))) * invW;
        dest.z = Math.fma(mat.m02, x, Math.fma(mat.m12, y, Math.fma(mat.m22, z, mat.m32 * w))) * invW;
        return dest;
    }

    public Vector3f mulProject(Matrix4f mat) {
        float x = this.x;
        float y = this.y;
        float z = this.z;
        float invW = 1f / Math.fma(mat.m03, x, Math.fma(mat.m13, y, Math.fma(mat.m23, z, mat.m33)));
        this.x = Math.fma(mat.m00, x, Math.fma(mat.m10, y, Math.fma(mat.m20, z, mat.m30))) * invW;
        this.y = Math.fma(mat.m01, x, Math.fma(mat.m11, y, Math.fma(mat.m21, z, mat.m31))) * invW;
        this.z = Math.fma(mat.m02, x, Math.fma(mat.m12, y, Math.fma(mat.m22, z, mat.m32))) * invW;
        return this;
    }

    public Vector3f mul(Matrix3f mat) {
        float lx = this.x;
        float ly = this.y;
        float lz = this.z;
        this.x = Math.fma(mat.m00, lx, Math.fma(mat.m10, ly, mat.m20 * lz));
        this.y = Math.fma(mat.m01, lx, Math.fma(mat.m11, ly, mat.m21 * lz));
        this.z = Math.fma(mat.m02, lx, Math.fma(mat.m12, ly, mat.m22 * lz));
        return this;
    }

    public Vector3f mul(Matrix3f mat, Vector3f dest) {
        float lx = this.x;
        float ly = this.y;
        float lz = this.z;
        dest.x = Math.fma(mat.m00, lx, Math.fma(mat.m10, ly, mat.m20 * lz));
        dest.y = Math.fma(mat.m01, lx, Math.fma(mat.m11, ly, mat.m21 * lz));
        dest.z = Math.fma(mat.m02, lx, Math.fma(mat.m12, ly, mat.m22 * lz));
        return dest;
    }

    public Vector3f mul(Matrix3d mat) {
        float lx = this.x;
        float ly = this.y;
        float lz = this.z;
        this.x = (float) Math.fma(mat.m00, lx, Math.fma(mat.m10, ly, mat.m20 * (double) lz));
        this.y = (float) Math.fma(mat.m01, lx, Math.fma(mat.m11, ly, mat.m21 * (double) lz));
        this.z = (float) Math.fma(mat.m02, lx, Math.fma(mat.m12, ly, mat.m22 * (double) lz));
        return this;
    }

    public Vector3f mul(Matrix3d mat, Vector3f dest) {
        float lx = this.x;
        float ly = this.y;
        float lz = this.z;
        dest.x = (float) Math.fma(mat.m00, lx, Math.fma(mat.m10, ly, mat.m20 * (double) lz));
        dest.y = (float) Math.fma(mat.m01, lx, Math.fma(mat.m11, ly, mat.m21 * (double) lz));
        dest.z = (float) Math.fma(mat.m02, lx, Math.fma(mat.m12, ly, mat.m22 * (double) lz));
        return dest;
    }

    public Vector3f mul(Matrix3x2f mat) {
        float x = this.x;
        float y = this.y;
        float z = this.z;
        this.x = Math.fma(mat.m00, x, Math.fma(mat.m10, y, mat.m20 * z));
        this.y = Math.fma(mat.m01, x, Math.fma(mat.m11, y, mat.m21 * z));
        this.z = z;
        return this;
    }

    public Vector3f mul(Matrix3x2f mat, Vector3f dest) {
        float x = this.x;
        float y = this.y;
        float z = this.z;
        dest.x = Math.fma(mat.m00, x, Math.fma(mat.m10, y, mat.m20 * z));
        dest.y = Math.fma(mat.m01, x, Math.fma(mat.m11, y, mat.m21 * z));
        dest.z = z;
        return dest;
    }

    public Vector3f mulTranspose(Matrix3f mat) {
        float x = this.x;
        float y = this.y;
        float z = this.z;
        this.x = Math.fma(mat.m00, x, Math.fma(mat.m01, y, mat.m02 * z));
        this.y = Math.fma(mat.m10, x, Math.fma(mat.m11, y, mat.m12 * z));
        this.z = Math.fma(mat.m20, x, Math.fma(mat.m21, y, mat.m22 * z));
        return this;
    }

    public Vector3f mulTranspose(Matrix3f mat, Vector3f dest) {
        float x = this.x;
        float y = this.y;
        float z = this.z;
        dest.x = Math.fma(mat.m00, x, Math.fma(mat.m01, y, mat.m02 * z));
        dest.y = Math.fma(mat.m10, x, Math.fma(mat.m11, y, mat.m12 * z));
        dest.z = Math.fma(mat.m20, x, Math.fma(mat.m21, y, mat.m22 * z));
        return dest;
    }

    public Vector3f mulPosition(Matrix4f mat) {
        float x = this.x;
        float y = this.y;
        float z = this.z;
        this.x = Math.fma(mat.m00, x, Math.fma(mat.m10, y, Math.fma(mat.m20, z, mat.m30)));
        this.y = Math.fma(mat.m01, x, Math.fma(mat.m11, y, Math.fma(mat.m21, z, mat.m31)));
        this.z = Math.fma(mat.m02, x, Math.fma(mat.m12, y, Math.fma(mat.m22, z, mat.m32)));
        return this;
    }

    public Vector3f mulPosition(Matrix4x3f mat) {
        float x = this.x;
        float y = this.y;
        float z = this.z;
        this.x = Math.fma(mat.m00, x, Math.fma(mat.m10, y, Math.fma(mat.m20, z, mat.m30)));
        this.y = Math.fma(mat.m01, x, Math.fma(mat.m11, y, Math.fma(mat.m21, z, mat.m31)));
        this.z = Math.fma(mat.m02, x, Math.fma(mat.m12, y, Math.fma(mat.m22, z, mat.m32)));
        return this;
    }

    public Vector3f mulPosition(Matrix4f mat, Vector3f dest) {
        float x = this.x;
        float y = this.y;
        float z = this.z;
        dest.x = Math.fma(mat.m00, x, Math.fma(mat.m10, y, Math.fma(mat.m20, z, mat.m30)));
        dest.y = Math.fma(mat.m01, x, Math.fma(mat.m11, y, Math.fma(mat.m21, z, mat.m31)));
        dest.z = Math.fma(mat.m02, x, Math.fma(mat.m12, y, Math.fma(mat.m22, z, mat.m32)));
        return dest;
    }

    public Vector3f mulPosition(Matrix4x3f mat, Vector3f dest) {
        float x = this.x;
        float y = this.y;
        float z = this.z;
        dest.x = Math.fma(mat.m00, x, Math.fma(mat.m10, y, Math.fma(mat.m20, z, mat.m30)));
        dest.y = Math.fma(mat.m01, x, Math.fma(mat.m11, y, Math.fma(mat.m21, z, mat.m31)));
        dest.z = Math.fma(mat.m02, x, Math.fma(mat.m12, y, Math.fma(mat.m22, z, mat.m32)));
        return dest;
    }

    public Vector3f mulTransposePosition(Matrix4f mat) {
        float x = this.x;
        float y = this.y;
        float z = this.z;
        this.x = Math.fma(mat.m00, x, Math.fma(mat.m01, y, Math.fma(mat.m02, z, mat.m03)));
        this.y = Math.fma(mat.m10, x, Math.fma(mat.m11, y, Math.fma(mat.m12, z, mat.m13)));
        this.z = Math.fma(mat.m20, x, Math.fma(mat.m21, y, Math.fma(mat.m22, z, mat.m23)));
        return this;
    }

    public Vector3f mulTransposePosition(Matrix4f mat, Vector3f dest) {
        float x = this.x;
        float y = this.y;
        float z = this.z;
        dest.x = Math.fma(mat.m00, x, Math.fma(mat.m01, y, Math.fma(mat.m02, z, mat.m03)));
        dest.y = Math.fma(mat.m10, x, Math.fma(mat.m11, y, Math.fma(mat.m12, z, mat.m13)));
        dest.z = Math.fma(mat.m20, x, Math.fma(mat.m21, y, Math.fma(mat.m22, z, mat.m23)));
        return dest;
    }

    public float mulPositionW(Matrix4f mat) {
        float x = this.x;
        float y = this.y;
        float z = this.z;
        float w = Math.fma(mat.m03, x, Math.fma(mat.m13, y, Math.fma(mat.m23, z, mat.m33)));
        this.x = Math.fma(mat.m00, x, Math.fma(mat.m10, y, Math.fma(mat.m20, z, mat.m30)));
        this.y = Math.fma(mat.m01, x, Math.fma(mat.m11, y, Math.fma(mat.m21, z, mat.m31)));
        this.z = Math.fma(mat.m02, x, Math.fma(mat.m12, y, Math.fma(mat.m22, z, mat.m32)));
        return w;
    }

    public float mulPositionW(Matrix4f mat, Vector3f dest) {
        float x = this.x;
        float y = this.y;
        float z = this.z;
        float w = Math.fma(mat.m03, x, Math.fma(mat.m13, y, Math.fma(mat.m23, z, mat.m33)));
        dest.x = Math.fma(mat.m00, x, Math.fma(mat.m10, y, Math.fma(mat.m20, z, mat.m30)));
        dest.y = Math.fma(mat.m01, x, Math.fma(mat.m11, y, Math.fma(mat.m21, z, mat.m31)));
        dest.z = Math.fma(mat.m02, x, Math.fma(mat.m12, y, Math.fma(mat.m22, z, mat.m32)));
        return w;
    }

    public Vector3f mulDirection(Matrix4d mat) {
        float x = this.x;
        float y = this.y;
        float z = this.z;
        this.x = (float) Math.fma(mat.m00, x, Math.fma(mat.m10, y, mat.m20 * (double) z));
        this.y = (float) Math.fma(mat.m01, x, Math.fma(mat.m11, y, mat.m21 * (double) z));
        this.z = (float) Math.fma(mat.m02, x, Math.fma(mat.m12, y, mat.m22 * (double) z));
        return this;
    }

    public Vector3f mulDirection(Matrix4f mat) {
        float x = this.x;
        float y = this.y;
        float z = this.z;
        this.x = Math.fma(mat.m00, x, Math.fma(mat.m10, y, mat.m20 * z));
        this.y = Math.fma(mat.m01, x, Math.fma(mat.m11, y, mat.m21 * z));
        this.z = Math.fma(mat.m02, x, Math.fma(mat.m12, y, mat.m22 * z));
        return this;
    }

    public Vector3f mulDirection(Matrix4x3f mat) {
        float x = this.x;
        float y = this.y;
        float z = this.z;
        this.x = Math.fma(mat.m00, x, Math.fma(mat.m10, y, mat.m20 * z));
        this.y = Math.fma(mat.m01, x, Math.fma(mat.m11, y, mat.m21 * z));
        this.z = Math.fma(mat.m02, x, Math.fma(mat.m12, y, mat.m22 * z));
        return this;
    }

    public Vector3f mulDirection(Matrix4d mat, Vector3f dest) {
        float x = this.x;
        float y = this.y;
        float z = this.z;
        dest.x = (float) Math.fma(mat.m00, x, Math.fma(mat.m10, y, mat.m20 * (double) z));
        dest.y = (float) Math.fma(mat.m01, x, Math.fma(mat.m11, y, mat.m21 * (double) z));
        dest.z = (float) Math.fma(mat.m02, x, Math.fma(mat.m12, y, mat.m22 * (double) z));
        return dest;
    }

    public Vector3f mulDirection(Matrix4f mat, Vector3f dest) {
        float x = this.x;
        float y = this.y;
        float z = this.z;
        dest.x = Math.fma(mat.m00, x, Math.fma(mat.m10, y, mat.m20 * z));
        dest.y = Math.fma(mat.m01, x, Math.fma(mat.m11, y, mat.m21 * z));
        dest.z = Math.fma(mat.m02, x, Math.fma(mat.m12, y, mat.m22 * z));
        return dest;
    }

    public Vector3f mulDirection(Matrix4x3f mat, Vector3f dest) {
        float x = this.x;
        float y = this.y;
        float z = this.z;
        dest.x = Math.fma(mat.m00, x, Math.fma(mat.m10, y, mat.m20 * z));
        dest.y = Math.fma(mat.m01, x, Math.fma(mat.m11, y, mat.m21 * z));
        dest.z = Math.fma(mat.m02, x, Math.fma(mat.m12, y, mat.m22 * z));
        return dest;
    }

    public Vector3f mulTransposeDirection(Matrix4f mat) {
        float x = this.x;
        float y = this.y;
        float z = this.z;
        this.x = Math.fma(mat.m00, x, Math.fma(mat.m01, y, mat.m02 * z));
        this.y = Math.fma(mat.m10, x, Math.fma(mat.m11, y, mat.m12 * z));
        this.z = Math.fma(mat.m20, x, Math.fma(mat.m21, y, mat.m22 * z));
        return this;
    }

    public Vector3f mulTransposeDirection(Matrix4f mat, Vector3f dest) {
        float x = this.x;
        float y = this.y;
        float z = this.z;
        dest.x = Math.fma(mat.m00, x, Math.fma(mat.m01, y, mat.m02 * z));
        dest.y = Math.fma(mat.m10, x, Math.fma(mat.m11, y, mat.m12 * z));
        dest.z = Math.fma(mat.m20, x, Math.fma(mat.m21, y, mat.m22 * z));
        return dest;
    }

    public Vector3f mul(float scalar) {
        this.x *= scalar;
        this.y *= scalar;
        this.z *= scalar;
        return this;
    }

    public Vector3f mul(float scalar, Vector3f dest) {
        dest.x = this.x * scalar;
        dest.y = this.y * scalar;
        dest.z = this.z * scalar;
        return dest;
    }

    public Vector3f mul(float x, float y, float z) {
        this.x *= x;
        this.y *= y;
        this.z *= z;
        return this;
    }

    public Vector3f mul(float x, float y, float z, Vector3f dest) {
        dest.x = this.x * x;
        dest.y = this.y * y;
        dest.z = this.z * z;
        return dest;
    }

    public Vector3f div(float scalar) {
        float inv = 1f / scalar;
        this.x *= inv;
        this.y *= inv;
        this.z *= inv;
        return this;
    }

    public Vector3f div(float scalar, Vector3f dest) {
        float inv = 1f / scalar;
        dest.x = this.x * inv;
        dest.y = this.y * inv;
        dest.z = this.z * inv;
        return dest;
    }

    public Vector3f div(float x, float y, float z) {
        this.x /= x;
        this.y /= y;
        this.z /= z;
        return this;
    }

    public Vector3f div(float x, float y, float z, Vector3f dest) {
        dest.x = this.x / x;
        dest.y = this.y / y;
        dest.z = this.z / z;
        return dest;
    }

    public Vector3f rotate(Quaternionf quat) {
        return quat.transform(this, this);
    }

    public Vector3f rotate(Quaternionf quat, Vector3f dest) {
        return quat.transform(this, dest);
    }

    public Quaternionf rotationTo(Vector3f toDir, Quaternionf dest) {
        return dest.rotationTo(this, toDir);
    }

    public Quaternionf rotationTo(float toDirX, float toDirY, float toDirZ, Quaternionf dest) {
        return dest.rotationTo(this.x, this.y, this.z, toDirX, toDirY, toDirZ);
    }

    public Vector3f rotateAxis(float angle, float x, float y, float z) {
        if (y == 0f && z == 0f && Math.absEqualsOne(x)) {
            return this.rotateX(x * angle, this);
        } else if (x == 0f && z == 0f && Math.absEqualsOne(y)) {
            return this.rotateY(y * angle, this);
        } else {
            return x == 0f && y == 0f && Math.absEqualsOne(z) ? this.rotateZ(z * angle, this) : this.rotateAxisInternal(angle, x, y, z, this);
        }
    }

    public Vector3f rotateAxis(float angle, float aX, float aY, float aZ, Vector3f dest) {
        if (aY == 0f && aZ == 0f && Math.absEqualsOne(aX)) {
            return this.rotateX(aX * angle, dest);
        } else if (aX == 0f && aZ == 0f && Math.absEqualsOne(aY)) {
            return this.rotateY(aY * angle, dest);
        } else {
            return aX == 0f && aY == 0f && Math.absEqualsOne(aZ) ? this.rotateZ(aZ * angle, dest) : this.rotateAxisInternal(angle, aX, aY, aZ, dest);
        }
    }

    private Vector3f rotateAxisInternal(float angle, float aX, float aY, float aZ, Vector3f dest) {
        float halfAngle = angle * 0.5F;
        float sinAngle = Math.sin(halfAngle);
        float qx = aX * sinAngle;
        float qy = aY * sinAngle;
        float qz = aZ * sinAngle;
        float qw = Math.cosFromSin(sinAngle, halfAngle);
        float w2 = qw * qw;
        float x2 = qx * qx;
        float y2 = qy * qy;
        float z2 = qz * qz;
        float zw = qz * qw;
        float xy = qx * qy;
        float xz = qx * qz;
        float yw = qy * qw;
        float yz = qy * qz;
        float xw = qx * qw;
        float x = this.x;
        float y = this.y;
        float z = this.z;
        dest.x = (w2 + x2 - z2 - y2) * x + (-zw + xy - zw + xy) * y + (yw + xz + xz + yw) * z;
        dest.y = (xy + zw + zw + xy) * x + (y2 - z2 + w2 - x2) * y + (yz + yz - xw - xw) * z;
        dest.z = (xz - yw + xz - yw) * x + (yz + yz + xw + xw) * y + (z2 - y2 - x2 + w2) * z;
        return dest;
    }

    public Vector3f rotateX(float angle) {
        float sin = Math.sin(angle);
        float cos = Math.cosFromSin(sin, angle);
        float y = this.y * cos - this.z * sin;
        float z = this.y * sin + this.z * cos;
        this.y = y;
        this.z = z;
        return this;
    }

    public Vector3f rotateX(float angle, Vector3f dest) {
        float sin = Math.sin(angle);
        float cos = Math.cosFromSin(sin, angle);
        float y = this.y * cos - this.z * sin;
        float z = this.y * sin + this.z * cos;
        dest.x = this.x;
        dest.y = y;
        dest.z = z;
        return dest;
    }

    public Vector3f rotateY(float angle) {
        float sin = Math.sin(angle);
        float cos = Math.cosFromSin(sin, angle);
        float x = this.x * cos + this.z * sin;
        float z = -this.x * sin + this.z * cos;
        this.x = x;
        this.z = z;
        return this;
    }

    public Vector3f rotateY(float angle, Vector3f dest) {
        float sin = Math.sin(angle);
        float cos = Math.cosFromSin(sin, angle);
        float x = this.x * cos + this.z * sin;
        float z = -this.x * sin + this.z * cos;
        dest.x = x;
        dest.y = this.y;
        dest.z = z;
        return dest;
    }

    public Vector3f rotateZ(float angle) {
        float sin = Math.sin(angle);
        float cos = Math.cosFromSin(sin, angle);
        float x = this.x * cos - this.y * sin;
        float y = this.x * sin + this.y * cos;
        this.x = x;
        this.y = y;
        return this;
    }

    public Vector3f rotateZ(float angle, Vector3f dest) {
        float sin = Math.sin(angle);
        float cos = Math.cosFromSin(sin, angle);
        float x = this.x * cos - this.y * sin;
        float y = this.x * sin + this.y * cos;
        dest.x = x;
        dest.y = y;
        dest.z = this.z;
        return dest;
    }

    public float lengthSquared() {
        return Math.fma(this.x, this.x, Math.fma(this.y, this.y, this.z * this.z));
    }

    public static float lengthSquared(float x, float y, float z) {
        return Math.fma(x, x, Math.fma(y, y, z * z));
    }

    public float length() {
        return Math.sqrt(Math.fma(this.x, this.x, Math.fma(this.y, this.y, this.z * this.z)));
    }

    public static float length(float x, float y, float z) {
        return Math.sqrt(Math.fma(x, x, Math.fma(y, y, z * z)));
    }

    public Vector3f normalize() {
        float scalar = Math.invsqrt(Math.fma(this.x, this.x, Math.fma(this.y, this.y, this.z * this.z)));
        this.x *= scalar;
        this.y *= scalar;
        this.z *= scalar;
        return this;
    }

    public Vector3f normalize(Vector3f dest) {
        float scalar = Math.invsqrt(Math.fma(this.x, this.x, Math.fma(this.y, this.y, this.z * this.z)));
        dest.x = this.x * scalar;
        dest.y = this.y * scalar;
        dest.z = this.z * scalar;
        return dest;
    }

    public Vector3f normalize(float length) {
        float scalar = Math.invsqrt(Math.fma(this.x, this.x, Math.fma(this.y, this.y, this.z * this.z))) * length;
        this.x *= scalar;
        this.y *= scalar;
        this.z *= scalar;
        return this;
    }

    public Vector3f normalize(float length, Vector3f dest) {
        float scalar = Math.invsqrt(Math.fma(this.x, this.x, Math.fma(this.y, this.y, this.z * this.z))) * length;
        dest.x = this.x * scalar;
        dest.y = this.y * scalar;
        dest.z = this.z * scalar;
        return dest;
    }

    public Vector3f cross(Vector3f v) {
        float rx = Math.fma(this.y, v.z, -this.z * v.y);
        float ry = Math.fma(this.z, v.x, -this.x * v.z);
        float rz = Math.fma(this.x, v.y, -this.y * v.x);
        this.x = rx;
        this.y = ry;
        this.z = rz;
        return this;
    }

    public Vector3f cross(float x, float y, float z) {
        float rx = Math.fma(this.y, z, -this.z * y);
        float ry = Math.fma(this.z, x, -this.x * z);
        float rz = Math.fma(this.x, y, -this.y * x);
        this.x = rx;
        this.y = ry;
        this.z = rz;
        return this;
    }

    public Vector3f cross(Vector3f v, Vector3f dest) {
        float rx = Math.fma(this.y, v.z, -this.z * v.y);
        float ry = Math.fma(this.z, v.x, -this.x * v.z);
        float rz = Math.fma(this.x, v.y, -this.y * v.x);
        dest.x = rx;
        dest.y = ry;
        dest.z = rz;
        return dest;
    }

    public Vector3f cross(float x, float y, float z, Vector3f dest) {
        float rx = Math.fma(this.y, z, -this.z * y);
        float ry = Math.fma(this.z, x, -this.x * z);
        float rz = Math.fma(this.x, y, -this.y * x);
        dest.x = rx;
        dest.y = ry;
        dest.z = rz;
        return dest;
    }

    public float distance(Vector3f v) {
        float dx = this.x - v.x;
        float dy = this.y - v.y;
        float dz = this.z - v.z;
        return Math.sqrt(Math.fma(dx, dx, Math.fma(dy, dy, dz * dz)));
    }

    public float distance(float x, float y, float z) {
        float dx = this.x - x;
        float dy = this.y - y;
        float dz = this.z - z;
        return Math.sqrt(Math.fma(dx, dx, Math.fma(dy, dy, dz * dz)));
    }

    public float distanceSquared(Vector3f v) {
        float dx = this.x - v.x;
        float dy = this.y - v.y;
        float dz = this.z - v.z;
        return Math.fma(dx, dx, Math.fma(dy, dy, dz * dz));
    }

    public float distanceSquared(float x, float y, float z) {
        float dx = this.x - x;
        float dy = this.y - y;
        float dz = this.z - z;
        return Math.fma(dx, dx, Math.fma(dy, dy, dz * dz));
    }

    public static float distance(float x1, float y1, float z1, float x2, float y2, float z2) {
        return Math.sqrt(distanceSquared(x1, y1, z1, x2, y2, z2));
    }

    public static float distanceSquared(float x1, float y1, float z1, float x2, float y2, float z2) {
        float dx = x1 - x2;
        float dy = y1 - y2;
        float dz = z1 - z2;
        return Math.fma(dx, dx, Math.fma(dy, dy, dz * dz));
    }

    public float dot(Vector3f v) {
        return Math.fma(this.x, v.x, Math.fma(this.y, v.y, this.z * v.z));
    }

    public float dot(float x, float y, float z) {
        return Math.fma(this.x, x, Math.fma(this.y, y, this.z * z));
    }

    public float angleCos(Vector3f v) {
        float x = this.x;
        float y = this.y;
        float z = this.z;
        float length1Squared = Math.fma(x, x, Math.fma(y, y, z * z));
        float length2Squared = Math.fma(v.x, v.x, Math.fma(v.y, v.y, v.z * v.z));
        float dot = Math.fma(x, v.x, Math.fma(y, v.y, z * v.z));
        return dot / Math.sqrt(length1Squared * length2Squared);
    }

    public float angle(Vector3f v) {
        float cos = this.angleCos(v);
        cos = java.lang.Math.min(cos, 1f);
        cos = java.lang.Math.max(cos, -1f);
        return Math.acos(cos);
    }

    public float angleSigned(Vector3f v, Vector3f n) {
        return this.angleSigned(v.x, v.y, v.z, n.x, n.y, n.z);
    }

    public float angleSigned(float x, float y, float z, float nx, float ny, float nz) {
        float tx = this.x;
        float ty = this.y;
        float tz = this.z;
        return Math.atan2((ty * z - tz * y) * nx + (tz * x - tx * z) * ny + (tx * y - ty * x) * nz, tx * x + ty * y + tz * z);
    }

    public Vector3f min(Vector3f v) {
        float x = this.x;
        float y = this.y;
        float z = this.z;
        this.x = java.lang.Math.min(x, v.x);
        this.y = java.lang.Math.min(y, v.y);
        this.z = java.lang.Math.min(z, v.z);
        return this;
    }

    public Vector3f min(Vector3f v, Vector3f dest) {
        float x = this.x;
        float y = this.y;
        float z = this.z;
        dest.x = java.lang.Math.min(x, v.x);
        dest.y = java.lang.Math.min(y, v.y);
        dest.z = java.lang.Math.min(z, v.z);
        return dest;
    }

    public Vector3f max(Vector3f v) {
        float x = this.x;
        float y = this.y;
        float z = this.z;
        this.x = java.lang.Math.max(x, v.x);
        this.y = java.lang.Math.max(y, v.y);
        this.z = java.lang.Math.max(z, v.z);
        return this;
    }

    public Vector3f max(Vector3f v, Vector3f dest) {
        float x = this.x;
        float y = this.y;
        float z = this.z;
        dest.x = Math.max(x, v.x);
        dest.y = Math.max(y, v.y);
        dest.z = Math.max(z, v.z);
        return dest;
    }

    public Vector3f zero() {
        this.x = 0f;
        this.y = 0f;
        this.z = 0f;
        return this;
    }

    public String toString() {
        return "(" + x + "," + y + "," + z + ")";
    }

    public Vector3f negate() {
        this.x = -this.x;
        this.y = -this.y;
        this.z = -this.z;
        return this;
    }

    public Vector3f negate(Vector3f dest) {
        dest.x = -this.x;
        dest.y = -this.y;
        dest.z = -this.z;
        return dest;
    }

    public Vector3f absolute() {
        this.x = Math.abs(this.x);
        this.y = Math.abs(this.y);
        this.z = Math.abs(this.z);
        return this;
    }

    public Vector3f absolute(Vector3f dest) {
        dest.x = Math.abs(this.x);
        dest.y = Math.abs(this.y);
        dest.z = Math.abs(this.z);
        return dest;
    }

    public int hashCode() {
        int result = Float.floatToIntBits(this.x);
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
            Vector3f other = (Vector3f) obj;
            if (Float.floatToIntBits(this.x) != Float.floatToIntBits(other.x)) {
                return false;
            } else if (Float.floatToIntBits(this.y) != Float.floatToIntBits(other.y)) {
                return false;
            } else {
                return Float.floatToIntBits(this.z) == Float.floatToIntBits(other.z);
            }
        }
    }

    public boolean equals(Vector3f v, float delta) {
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

    public boolean equals(float x, float y, float z) {
        if (Float.floatToIntBits(this.x) != Float.floatToIntBits(x)) {
            return false;
        } else if (Float.floatToIntBits(this.y) != Float.floatToIntBits(y)) {
            return false;
        } else {
            return Float.floatToIntBits(this.z) == Float.floatToIntBits(z);
        }
    }

    public Vector3f reflect(Vector3f normal) {
        float x = normal.x;
        float y = normal.y;
        float z = normal.z;
        float dot = Math.fma(this.x, x, Math.fma(this.y, y, this.z * z));
        this.x -= (dot + dot) * x;
        this.y -= (dot + dot) * y;
        this.z -= (dot + dot) * z;
        return this;
    }

    public Vector3f reflect(float x, float y, float z) {
        float dot = Math.fma(this.x, x, Math.fma(this.y, y, this.z * z));
        this.x -= (dot + dot) * x;
        this.y -= (dot + dot) * y;
        this.z -= (dot + dot) * z;
        return this;
    }

    public Vector3f reflect(Vector3f normal, Vector3f dest) {
        return this.reflect(normal.x, normal.y, normal.z, dest);
    }

    public Vector3f reflect(float x, float y, float z, Vector3f dest) {
        float dot = this.dot(x, y, z);
        dest.x = this.x - (dot + dot) * x;
        dest.y = this.y - (dot + dot) * y;
        dest.z = this.z - (dot + dot) * z;
        return dest;
    }

    public Vector3f half(Vector3f other) {
        return this.set(this).add(other.x, other.y, other.z).normalize();
    }

    public Vector3f half(float x, float y, float z) {
        return this.half(x, y, z, this);
    }

    public Vector3f half(Vector3f other, Vector3f dest) {
        return this.half(other.x, other.y, other.z, dest);
    }

    public Vector3f half(float x, float y, float z, Vector3f dest) {
        return dest.set(this).add(x, y, z).normalize();
    }

    public Vector3f smoothStep(Vector3f v, float t, Vector3f dest) {
        float x = this.x;
        float y = this.y;
        float z = this.z;
        float t2 = t * t;
        float t3 = t2 * t;
        dest.x = (x + x - v.x - v.x) * t3 + (3f * v.x - 3f * x) * t2 + x * t + x;
        dest.y = (y + y - v.y - v.y) * t3 + (3f * v.y - 3f * y) * t2 + y * t + y;
        dest.z = (z + z - v.z - v.z) * t3 + (3f * v.z - 3f * z) * t2 + z * t + z;
        return dest;
    }

    public Vector3f hermite(Vector3f t0, Vector3f v1, Vector3f t1, float t, Vector3f dest) {
        float x = this.x;
        float y = this.y;
        float z = this.z;
        float t2 = t * t;
        float t3 = t2 * t;
        dest.x = (x + x - v1.x - v1.x + t1.x + t0.x) * t3 + (3f * v1.x - 3f * x - t0.x - t0.x - t1.x) * t2 + x * t + x;
        dest.y = (y + y - v1.y - v1.y + t1.y + t0.y) * t3 + (3f * v1.y - 3f * y - t0.y - t0.y - t1.y) * t2 + y * t + y;
        dest.z = (z + z - v1.z - v1.z + t1.z + t0.z) * t3 + (3f * v1.z - 3f * z - t0.z - t0.z - t1.z) * t2 + z * t + z;
        return dest;
    }

    public Vector3f lerp(Vector3f other, float t) {
        return this.lerp(other, t, this);
    }

    public Vector3f lerp(Vector3f other, float t, Vector3f dest) {
        dest.x = Math.fma(other.x - this.x, t, this.x);
        dest.y = Math.fma(other.y - this.y, t, this.y);
        dest.z = Math.fma(other.z - this.z, t, this.z);
        return dest;
    }

    public float get(int component) throws IllegalArgumentException {
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
        dest.x = this.x;
        dest.y = this.y;
        dest.z = this.z;
        return dest;
    }

    public Vector3d get(Vector3d dest) {
        dest.x = this.x;
        dest.y = this.y;
        dest.z = this.z;
        return dest;
    }

    public int maxComponent() {
        float absX = Math.abs(this.x);
        float absY = Math.abs(this.y);
        float absZ = Math.abs(this.z);
        if (absX >= absY && absX >= absZ) {
            return 0;
        } else {
            return absY >= absZ ? 1 : 2;
        }
    }

    public int minComponent() {
        float absX = Math.abs(this.x);
        float absY = Math.abs(this.y);
        float absZ = Math.abs(this.z);
        if (absX < absY && absX < absZ) {
            return 0;
        } else {
            return absY < absZ ? 1 : 2;
        }
    }

    public Vector3f orthogonalize(Vector3f v, Vector3f dest) {
        float rx;
        float ry;
        float rz;
        if (Math.abs(v.x) > Math.abs(v.z)) {
            rx = -v.y;
            ry = v.x;
            rz = 0f;
        } else {
            rx = 0f;
            ry = -v.z;
            rz = v.y;
        }

        float invLen = Math.invsqrt(rx * rx + ry * ry + rz * rz);
        dest.x = rx * invLen;
        dest.y = ry * invLen;
        dest.z = rz * invLen;
        return dest;
    }

    public Vector3f orthogonalize(Vector3f v) {
        return this.orthogonalize(v, this);
    }

    public Vector3f orthogonalizeUnit(Vector3f v, Vector3f dest) {
        return this.orthogonalize(v, dest);
    }

    public Vector3f orthogonalizeUnit(Vector3f v) {
        return this.orthogonalizeUnit(v, this);
    }

    public Vector3f floor() {
        return this.floor(this);
    }

    public Vector3f floor(Vector3f dest) {
        dest.x = Math.floor(this.x);
        dest.y = Math.floor(this.y);
        dest.z = Math.floor(this.z);
        return dest;
    }

    public Vector3f ceil() {
        return this.ceil(this);
    }

    public Vector3f ceil(Vector3f dest) {
        dest.x = Math.ceil(this.x);
        dest.y = Math.ceil(this.y);
        dest.z = Math.ceil(this.z);
        return dest;
    }

    public Vector3f round() {
        return this.round(this);
    }

    public Vector3f round(Vector3f dest) {
        dest.x = (float) Math.round(this.x);
        dest.y = (float) Math.round(this.y);
        dest.z = (float) Math.round(this.z);
        return dest;
    }

    public boolean isFinite() {
        return Math.isFinite(this.x) && Math.isFinite(this.y) && Math.isFinite(this.z);
    }

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
