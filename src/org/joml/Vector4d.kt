package org.joml;

@SuppressWarnings("unused")
public class Vector4d {
    
    public double x;
    public double y;
    public double z;
    public double w;

    public Vector4d() {
        this.w = 1.0;
    }

    public Vector4d(Vector4d v) {
        this.x = v.x;
        this.y = v.y;
        this.z = v.z;
        this.w = v.w;
    }

    public Vector4d(Vector4i v) {
        this.x = v.x;
        this.y = v.y;
        this.z = v.z;
        this.w = v.w;
    }

    public Vector4d(Vector3d v, double w) {
        this.x = v.x;
        this.y = v.y;
        this.z = v.z;
        this.w = w;
    }

    public Vector4d(Vector3i v, double w) {
        this.x = v.x;
        this.y = v.y;
        this.z = v.z;
        this.w = w;
    }

    public Vector4d(Vector2d v, double z, double w) {
        this.x = v.x;
        this.y = v.y;
        this.z = z;
        this.w = w;
    }

    public Vector4d(Vector2i v, double z, double w) {
        this.x = v.x;
        this.y = v.y;
        this.z = z;
        this.w = w;
    }

    public Vector4d(double d) {
        this.x = d;
        this.y = d;
        this.z = d;
        this.w = d;
    }

    public Vector4d(Vector4f v) {
        this.x = v.x;
        this.y = v.y;
        this.z = v.z;
        this.w = v.w;
    }

    public Vector4d(Vector3f v, double w) {
        this.x = v.x;
        this.y = v.y;
        this.z = v.z;
        this.w = w;
    }

    public Vector4d(Vector2f v, double z, double w) {
        this.x = v.x;
        this.y = v.y;
        this.z = z;
        this.w = w;
    }

    public Vector4d(double x, double y, double z, double w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
    }

    public Vector4d(float[] xyzw) {
        this.x = xyzw[0];
        this.y = xyzw[1];
        this.z = xyzw[2];
        this.w = xyzw[3];
    }

    public Vector4d(double[] xyzw) {
        this.x = xyzw[0];
        this.y = xyzw[1];
        this.z = xyzw[2];
        this.w = xyzw[3];
    }

    public Vector4d set(Vector4d v) {
        this.x = v.x;
        this.y = v.y;
        this.z = v.z;
        this.w = v.w;
        return this;
    }

    public Vector4d set(Vector4f v) {
        this.x = v.x;
        this.y = v.y;
        this.z = v.z;
        this.w = v.w;
        return this;
    }

    public Vector4d set(Vector4i v) {
        this.x = v.x;
        this.y = v.y;
        this.z = v.z;
        this.w = v.w;
        return this;
    }

    public Vector4d set(Vector3d v, double w) {
        this.x = v.x;
        this.y = v.y;
        this.z = v.z;
        this.w = w;
        return this;
    }

    public Vector4d set(Vector3i v, double w) {
        this.x = v.x;
        this.y = v.y;
        this.z = v.z;
        this.w = w;
        return this;
    }

    public Vector4d set(Vector3f v, double w) {
        this.x = v.x;
        this.y = v.y;
        this.z = v.z;
        this.w = w;
        return this;
    }

    public Vector4d set(Vector2d v, double z, double w) {
        this.x = v.x;
        this.y = v.y;
        this.z = z;
        this.w = w;
        return this;
    }

    public Vector4d set(Vector2i v, double z, double w) {
        this.x = v.x;
        this.y = v.y;
        this.z = z;
        this.w = w;
        return this;
    }

    public Vector4d set(double d) {
        this.x = d;
        this.y = d;
        this.z = d;
        this.w = d;
        return this;
    }

    public Vector4d set(Vector2f v, double z, double w) {
        this.x = v.x;
        this.y = v.y;
        this.z = z;
        this.w = w;
        return this;
    }

    public Vector4d set(double x, double y, double z, double w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
        return this;
    }

    public Vector4d set(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
        return this;
    }

    public Vector4d set(double[] xyzw) {
        this.x = xyzw[0];
        this.y = xyzw[1];
        this.z = xyzw[2];
        this.w = xyzw[3];
        return this;
    }

    public Vector4d set(float[] xyzw) {
        this.x = xyzw[0];
        this.y = xyzw[1];
        this.z = xyzw[2];
        this.w = xyzw[3];
        return this;
    }

    public Vector4d setComponent(int component, double value) throws IllegalArgumentException {
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
            case 3:
                this.w = value;
                break;
            default:
                throw new IllegalArgumentException();
        }

        return this;
    }

    public Vector4d sub(Vector4d v) {
        this.x -= v.x;
        this.y -= v.y;
        this.z -= v.z;
        this.w -= v.w;
        return this;
    }

    public Vector4d sub(Vector4d v, Vector4d dest) {
        dest.x = this.x - v.x;
        dest.y = this.y - v.y;
        dest.z = this.z - v.z;
        dest.w = this.w - v.w;
        return dest;
    }

    public Vector4d sub(Vector4f v) {
        this.x -= v.x;
        this.y -= v.y;
        this.z -= v.z;
        this.w -= v.w;
        return this;
    }

    public Vector4d sub(Vector4f v, Vector4d dest) {
        dest.x = this.x - (double) v.x;
        dest.y = this.y - (double) v.y;
        dest.z = this.z - (double) v.z;
        dest.w = this.w - (double) v.w;
        return dest;
    }

    public Vector4d sub(double x, double y, double z, double w) {
        this.x -= x;
        this.y -= y;
        this.z -= z;
        this.w -= w;
        return this;
    }

    public Vector4d sub(double x, double y, double z, double w, Vector4d dest) {
        dest.x = this.x - x;
        dest.y = this.y - y;
        dest.z = this.z - z;
        dest.w = this.w - w;
        return dest;
    }

    public Vector4d add(Vector4d v) {
        this.x += v.x;
        this.y += v.y;
        this.z += v.z;
        this.w += v.w;
        return this;
    }

    public Vector4d add(Vector4d v, Vector4d dest) {
        dest.x = this.x + v.x;
        dest.y = this.y + v.y;
        dest.z = this.z + v.z;
        dest.w = this.w + v.w;
        return dest;
    }

    public Vector4d add(Vector4f v, Vector4d dest) {
        dest.x = this.x + (double) v.x;
        dest.y = this.y + (double) v.y;
        dest.z = this.z + (double) v.z;
        dest.w = this.w + (double) v.w;
        return dest;
    }

    public Vector4d add(double x, double y, double z, double w) {
        this.x += x;
        this.y += y;
        this.z += z;
        this.w += w;
        return this;
    }

    public Vector4d add(double x, double y, double z, double w, Vector4d dest) {
        dest.x = this.x + x;
        dest.y = this.y + y;
        dest.z = this.z + z;
        dest.w = this.w + w;
        return dest;
    }

    public Vector4d add(Vector4f v) {
        this.x += v.x;
        this.y += v.y;
        this.z += v.z;
        this.w += v.w;
        return this;
    }

    public Vector4d fma(Vector4d a, Vector4d b) {
        this.x = Math.fma(a.x, b.x, this.x);
        this.y = Math.fma(a.y, b.y, this.y);
        this.z = Math.fma(a.z, b.z, this.z);
        this.w = Math.fma(a.w, b.w, this.w);
        return this;
    }

    public Vector4d fma(double a, Vector4d b) {
        this.x = Math.fma(a, b.x, this.x);
        this.y = Math.fma(a, b.y, this.y);
        this.z = Math.fma(a, b.z, this.z);
        this.w = Math.fma(a, b.w, this.w);
        return this;
    }

    public Vector4d fma(Vector4d a, Vector4d b, Vector4d dest) {
        dest.x = Math.fma(a.x, b.x, this.x);
        dest.y = Math.fma(a.y, b.y, this.y);
        dest.z = Math.fma(a.z, b.z, this.z);
        dest.w = Math.fma(a.w, b.w, this.w);
        return dest;
    }

    public Vector4d fma(double a, Vector4d b, Vector4d dest) {
        dest.x = Math.fma(a, b.x, this.x);
        dest.y = Math.fma(a, b.y, this.y);
        dest.z = Math.fma(a, b.z, this.z);
        dest.w = Math.fma(a, b.w, this.w);
        return dest;
    }

    public Vector4d mulAdd(Vector4d a, Vector4d b) {
        this.x = Math.fma(this.x, a.x, b.x);
        this.y = Math.fma(this.y, a.y, b.y);
        this.z = Math.fma(this.z, a.z, b.z);
        return this;
    }

    public Vector4d mulAdd(double a, Vector4d b) {
        this.x = Math.fma(this.x, a, b.x);
        this.y = Math.fma(this.y, a, b.y);
        this.z = Math.fma(this.z, a, b.z);
        return this;
    }

    public Vector4d mulAdd(Vector4d a, Vector4d b, Vector4d dest) {
        dest.x = Math.fma(this.x, a.x, b.x);
        dest.y = Math.fma(this.y, a.y, b.y);
        dest.z = Math.fma(this.z, a.z, b.z);
        return dest;
    }

    public Vector4d mulAdd(double a, Vector4d b, Vector4d dest) {
        dest.x = Math.fma(this.x, a, b.x);
        dest.y = Math.fma(this.y, a, b.y);
        dest.z = Math.fma(this.z, a, b.z);
        return dest;
    }

    public Vector4d mul(Vector4d v) {
        this.x *= v.x;
        this.y *= v.y;
        this.z *= v.z;
        this.w *= v.w;
        return this;
    }

    public Vector4d mul(Vector4d v, Vector4d dest) {
        dest.x = this.x * v.x;
        dest.y = this.y * v.y;
        dest.z = this.z * v.z;
        dest.w = this.w * v.w;
        return dest;
    }

    public Vector4d div(Vector4d v) {
        this.x /= v.x;
        this.y /= v.y;
        this.z /= v.z;
        this.w /= v.w;
        return this;
    }

    public Vector4d div(Vector4d v, Vector4d dest) {
        dest.x = this.x / v.x;
        dest.y = this.y / v.y;
        dest.z = this.z / v.z;
        dest.w = this.w / v.w;
        return dest;
    }

    public Vector4d mul(Vector4f v) {
        this.x *= v.x;
        this.y *= v.y;
        this.z *= v.z;
        this.w *= v.w;
        return this;
    }

    public Vector4d mul(Vector4f v, Vector4d dest) {
        dest.x = this.x * (double) v.x;
        dest.y = this.y * (double) v.y;
        dest.z = this.z * (double) v.z;
        dest.w = this.w * (double) v.w;
        return dest;
    }

    public Vector4d mul(Matrix4d mat) {
        return (mat.properties() & 2) != 0 ? this.mulAffine(mat, this) : this.mulGeneric(mat, this);
    }

    public Vector4d mul(Matrix4d mat, Vector4d dest) {
        return (mat.properties() & 2) != 0 ? this.mulAffine(mat, dest) : this.mulGeneric(mat, dest);
    }

    public Vector4d mulTranspose(Matrix4d mat) {
        return (mat.properties() & 2) != 0 ? this.mulAffineTranspose(mat, this) : this.mulGenericTranspose(mat, this);
    }

    public Vector4d mulTranspose(Matrix4d mat, Vector4d dest) {
        return (mat.properties() & 2) != 0 ? this.mulAffineTranspose(mat, dest) : this.mulGenericTranspose(mat, dest);
    }

    public Vector4d mulAffine(Matrix4d mat, Vector4d dest) {
        double rx = Math.fma(mat.m00, this.x, Math.fma(mat.m10, this.y, Math.fma(mat.m20, this.z, mat.m30 * this.w)));
        double ry = Math.fma(mat.m01, this.x, Math.fma(mat.m11, this.y, Math.fma(mat.m21, this.z, mat.m31 * this.w)));
        double rz = Math.fma(mat.m02, this.x, Math.fma(mat.m12, this.y, Math.fma(mat.m22, this.z, mat.m32 * this.w)));
        dest.x = rx;
        dest.y = ry;
        dest.z = rz;
        dest.w = this.w;
        return dest;
    }

    private Vector4d mulGeneric(Matrix4d mat, Vector4d dest) {
        double rx = Math.fma(mat.m00, this.x, Math.fma(mat.m10, this.y, Math.fma(mat.m20, this.z, mat.m30 * this.w)));
        double ry = Math.fma(mat.m01, this.x, Math.fma(mat.m11, this.y, Math.fma(mat.m21, this.z, mat.m31 * this.w)));
        double rz = Math.fma(mat.m02, this.x, Math.fma(mat.m12, this.y, Math.fma(mat.m22, this.z, mat.m32 * this.w)));
        double rw = Math.fma(mat.m03, this.x, Math.fma(mat.m13, this.y, Math.fma(mat.m23, this.z, mat.m33 * this.w)));
        dest.x = rx;
        dest.y = ry;
        dest.z = rz;
        dest.w = rw;
        return dest;
    }

    public Vector4d mulAffineTranspose(Matrix4d mat, Vector4d dest) {
        double x = this.x;
        double y = this.y;
        double z = this.z;
        double w = this.w;
        dest.x = Math.fma(mat.m00, x, Math.fma(mat.m01, y, mat.m02 * z));
        dest.y = Math.fma(mat.m10, x, Math.fma(mat.m11, y, mat.m12 * z));
        dest.z = Math.fma(mat.m20, x, Math.fma(mat.m21, y, mat.m22 * z));
        dest.w = Math.fma(mat.m30, x, Math.fma(mat.m31, y, mat.m32 * z + w));
        return dest;
    }

    private Vector4d mulGenericTranspose(Matrix4d mat, Vector4d dest) {
        double x = this.x;
        double y = this.y;
        double z = this.z;
        double w = this.w;
        dest.x = Math.fma(mat.m00, x, Math.fma(mat.m01, y, Math.fma(mat.m02, z, mat.m03 * w)));
        dest.y = Math.fma(mat.m10, x, Math.fma(mat.m11, y, Math.fma(mat.m12, z, mat.m13 * w)));
        dest.z = Math.fma(mat.m20, x, Math.fma(mat.m21, y, Math.fma(mat.m22, z, mat.m23 * w)));
        dest.w = Math.fma(mat.m30, x, Math.fma(mat.m31, y, Math.fma(mat.m32, z, mat.m33 * w)));
        return dest;
    }

    public Vector4d mul(Matrix4x3d mat) {
        double rx = Math.fma(mat.m00, this.x, Math.fma(mat.m10, this.y, Math.fma(mat.m20, this.z, mat.m30 * this.w)));
        double ry = Math.fma(mat.m01, this.x, Math.fma(mat.m11, this.y, Math.fma(mat.m21, this.z, mat.m31 * this.w)));
        double rz = Math.fma(mat.m02, this.x, Math.fma(mat.m12, this.y, Math.fma(mat.m22, this.z, mat.m32 * this.w)));
        this.x = rx;
        this.y = ry;
        this.z = rz;
        return this;
    }

    public Vector4d mul(Matrix4x3d mat, Vector4d dest) {
        double rx = Math.fma(mat.m00, this.x, Math.fma(mat.m10, this.y, Math.fma(mat.m20, this.z, mat.m30 * this.w)));
        double ry = Math.fma(mat.m01, this.x, Math.fma(mat.m11, this.y, Math.fma(mat.m21, this.z, mat.m31 * this.w)));
        double rz = Math.fma(mat.m02, this.x, Math.fma(mat.m12, this.y, Math.fma(mat.m22, this.z, mat.m32 * this.w)));
        dest.x = rx;
        dest.y = ry;
        dest.z = rz;
        dest.w = this.w;
        return dest;
    }

    public Vector4d mul(Matrix4x3f mat) {
        double rx = Math.fma(mat.m00, this.x, Math.fma(mat.m10, this.y, Math.fma(mat.m20, this.z, (double) mat.m30 * this.w)));
        double ry = Math.fma(mat.m01, this.x, Math.fma(mat.m11, this.y, Math.fma(mat.m21, this.z, (double) mat.m31 * this.w)));
        double rz = Math.fma(mat.m02, this.x, Math.fma(mat.m12, this.y, Math.fma(mat.m22, this.z, (double) mat.m32 * this.w)));
        this.x = rx;
        this.y = ry;
        this.z = rz;
        return this;
    }

    public Vector4d mul(Matrix4x3f mat, Vector4d dest) {
        double rx = Math.fma(mat.m00, this.x, Math.fma(mat.m10, this.y, Math.fma(mat.m20, this.z, (double) mat.m30 * this.w)));
        double ry = Math.fma(mat.m01, this.x, Math.fma(mat.m11, this.y, Math.fma(mat.m21, this.z, (double) mat.m31 * this.w)));
        double rz = Math.fma(mat.m02, this.x, Math.fma(mat.m12, this.y, Math.fma(mat.m22, this.z, (double) mat.m32 * this.w)));
        dest.x = rx;
        dest.y = ry;
        dest.z = rz;
        dest.w = this.w;
        return dest;
    }

    public Vector4d mul(Matrix4f mat) {
        return (mat.properties() & 2) != 0 ? this.mulAffine(mat, this) : this.mulGeneric(mat, this);
    }

    public Vector4d mul(Matrix4f mat, Vector4d dest) {
        return (mat.properties() & 2) != 0 ? this.mulAffine(mat, dest) : this.mulGeneric(mat, dest);
    }

    private Vector4d mulAffine(Matrix4f mat, Vector4d dest) {
        double rx = Math.fma(mat.m00, this.x, Math.fma(mat.m10, this.y, Math.fma(mat.m20, this.z, (double) mat.m30 * this.w)));
        double ry = Math.fma(mat.m01, this.x, Math.fma(mat.m11, this.y, Math.fma(mat.m21, this.z, (double) mat.m31 * this.w)));
        double rz = Math.fma(mat.m02, this.x, Math.fma(mat.m12, this.y, Math.fma(mat.m22, this.z, (double) mat.m32 * this.w)));
        dest.x = rx;
        dest.y = ry;
        dest.z = rz;
        dest.w = this.w;
        return dest;
    }

    private Vector4d mulGeneric(Matrix4f mat, Vector4d dest) {
        double rx = Math.fma(mat.m00, this.x, Math.fma(mat.m10, this.y, Math.fma(mat.m20, this.z, (double) mat.m30 * this.w)));
        double ry = Math.fma(mat.m01, this.x, Math.fma(mat.m11, this.y, Math.fma(mat.m21, this.z, (double) mat.m31 * this.w)));
        double rz = Math.fma(mat.m02, this.x, Math.fma(mat.m12, this.y, Math.fma(mat.m22, this.z, (double) mat.m32 * this.w)));
        double rw = Math.fma(mat.m03, this.x, Math.fma(mat.m13, this.y, Math.fma(mat.m23, this.z, (double) mat.m33 * this.w)));
        dest.x = rx;
        dest.y = ry;
        dest.z = rz;
        dest.w = rw;
        return dest;
    }

    public Vector4d mulProject(Matrix4d mat, Vector4d dest) {
        double invW = 1.0 / Math.fma(mat.m03, this.x, Math.fma(mat.m13, this.y, Math.fma(mat.m23, this.z, mat.m33 * this.w)));
        double rx = Math.fma(mat.m00, this.x, Math.fma(mat.m10, this.y, Math.fma(mat.m20, this.z, mat.m30 * this.w))) * invW;
        double ry = Math.fma(mat.m01, this.x, Math.fma(mat.m11, this.y, Math.fma(mat.m21, this.z, mat.m31 * this.w))) * invW;
        double rz = Math.fma(mat.m02, this.x, Math.fma(mat.m12, this.y, Math.fma(mat.m22, this.z, mat.m32 * this.w))) * invW;
        dest.x = rx;
        dest.y = ry;
        dest.z = rz;
        dest.w = 1.0;
        return dest;
    }

    public Vector4d mulProject(Matrix4d mat) {
        double invW = 1.0 / Math.fma(mat.m03, this.x, Math.fma(mat.m13, this.y, Math.fma(mat.m23, this.z, mat.m33 * this.w)));
        double rx = Math.fma(mat.m00, this.x, Math.fma(mat.m10, this.y, Math.fma(mat.m20, this.z, mat.m30 * this.w))) * invW;
        double ry = Math.fma(mat.m01, this.x, Math.fma(mat.m11, this.y, Math.fma(mat.m21, this.z, mat.m31 * this.w))) * invW;
        double rz = Math.fma(mat.m02, this.x, Math.fma(mat.m12, this.y, Math.fma(mat.m22, this.z, mat.m32 * this.w))) * invW;
        this.x = rx;
        this.y = ry;
        this.z = rz;
        this.w = 1.0;
        return this;
    }

    public Vector3d mulProject(Matrix4d mat, Vector3d dest) {
        double invW = 1.0 / Math.fma(mat.m03, this.x, Math.fma(mat.m13, this.y, Math.fma(mat.m23, this.z, mat.m33 * this.w)));
        double rx = Math.fma(mat.m00, this.x, Math.fma(mat.m10, this.y, Math.fma(mat.m20, this.z, mat.m30 * this.w))) * invW;
        double ry = Math.fma(mat.m01, this.x, Math.fma(mat.m11, this.y, Math.fma(mat.m21, this.z, mat.m31 * this.w))) * invW;
        double rz = Math.fma(mat.m02, this.x, Math.fma(mat.m12, this.y, Math.fma(mat.m22, this.z, mat.m32 * this.w))) * invW;
        dest.x = rx;
        dest.y = ry;
        dest.z = rz;
        return dest;
    }

    public Vector4d mul(double scalar) {
        this.x *= scalar;
        this.y *= scalar;
        this.z *= scalar;
        this.w *= scalar;
        return this;
    }

    public Vector4d mul(double scalar, Vector4d dest) {
        dest.x = this.x * scalar;
        dest.y = this.y * scalar;
        dest.z = this.z * scalar;
        dest.w = this.w * scalar;
        return dest;
    }

    public Vector4d div(double scalar) {
        double inv = 1.0 / scalar;
        this.x *= inv;
        this.y *= inv;
        this.z *= inv;
        this.w *= inv;
        return this;
    }

    public Vector4d div(double scalar, Vector4d dest) {
        double inv = 1.0 / scalar;
        dest.x = this.x * inv;
        dest.y = this.y * inv;
        dest.z = this.z * inv;
        dest.w = this.w * inv;
        return dest;
    }

    public Vector4d rotate(Quaterniond quat) {
        quat.transform(this, this);
        return this;
    }

    public Vector4d rotate(Quaterniond quat, Vector4d dest) {
        quat.transform(this, dest);
        return dest;
    }

    public Vector4d rotateAxis(double angle, double x, double y, double z) {
        if (y == 0.0 && z == 0.0 && Math.absEqualsOne(x)) {
            return this.rotateX(x * angle, this);
        } else if (x == 0.0 && z == 0.0 && Math.absEqualsOne(y)) {
            return this.rotateY(y * angle, this);
        } else {
            return x == 0.0 && y == 0.0 && Math.absEqualsOne(z) ? this.rotateZ(z * angle, this) : this.rotateAxisInternal(angle, x, y, z, this);
        }
    }

    public Vector4d rotateAxis(double angle, double aX, double aY, double aZ, Vector4d dest) {
        if (aY == 0.0 && aZ == 0.0 && Math.absEqualsOne(aX)) {
            return this.rotateX(aX * angle, dest);
        } else if (aX == 0.0 && aZ == 0.0 && Math.absEqualsOne(aY)) {
            return this.rotateY(aY * angle, dest);
        } else {
            return aX == 0.0 && aY == 0.0 && Math.absEqualsOne(aZ) ? this.rotateZ(aZ * angle, dest) : this.rotateAxisInternal(angle, aX, aY, aZ, dest);
        }
    }

    private Vector4d rotateAxisInternal(double angle, double aX, double aY, double aZ, Vector4d dest) {
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

    public Vector4d rotateX(double angle) {
        double sin = Math.sin(angle);
        double cos = Math.cosFromSin(sin, angle);
        double y = this.y * cos - this.z * sin;
        double z = this.y * sin + this.z * cos;
        this.y = y;
        this.z = z;
        return this;
    }

    public Vector4d rotateX(double angle, Vector4d dest) {
        double sin = Math.sin(angle);
        double cos = Math.cosFromSin(sin, angle);
        double y = this.y * cos - this.z * sin;
        double z = this.y * sin + this.z * cos;
        dest.x = this.x;
        dest.y = y;
        dest.z = z;
        dest.w = this.w;
        return dest;
    }

    public Vector4d rotateY(double angle) {
        double sin = Math.sin(angle);
        double cos = Math.cosFromSin(sin, angle);
        double x = this.x * cos + this.z * sin;
        double z = -this.x * sin + this.z * cos;
        this.x = x;
        this.z = z;
        return this;
    }

    public Vector4d rotateY(double angle, Vector4d dest) {
        double sin = Math.sin(angle);
        double cos = Math.cosFromSin(sin, angle);
        double x = this.x * cos + this.z * sin;
        double z = -this.x * sin + this.z * cos;
        dest.x = x;
        dest.y = this.y;
        dest.z = z;
        dest.w = this.w;
        return dest;
    }

    public Vector4d rotateZ(double angle) {
        double sin = Math.sin(angle);
        double cos = Math.cosFromSin(sin, angle);
        double x = this.x * cos - this.y * sin;
        double y = this.x * sin + this.y * cos;
        this.x = x;
        this.y = y;
        return this;
    }

    public Vector4d rotateZ(double angle, Vector4d dest) {
        double sin = Math.sin(angle);
        double cos = Math.cosFromSin(sin, angle);
        double x = this.x * cos - this.y * sin;
        double y = this.x * sin + this.y * cos;
        dest.x = x;
        dest.y = y;
        dest.z = this.z;
        dest.w = this.w;
        return dest;
    }

    public double lengthSquared() {
        return Math.fma(this.x, this.x, Math.fma(this.y, this.y, Math.fma(this.z, this.z, this.w * this.w)));
    }

    public static double lengthSquared(double x, double y, double z, double w) {
        return Math.fma(x, x, Math.fma(y, y, Math.fma(z, z, w * w)));
    }

    public double length() {
        return Math.sqrt(Math.fma(this.x, this.x, Math.fma(this.y, this.y, Math.fma(this.z, this.z, this.w * this.w))));
    }

    public static double length(double x, double y, double z, double w) {
        return Math.sqrt(Math.fma(x, x, Math.fma(y, y, Math.fma(z, z, w * w))));
    }

    public Vector4d normalize() {
        double invLength = 1.0 / this.length();
        this.x *= invLength;
        this.y *= invLength;
        this.z *= invLength;
        this.w *= invLength;
        return this;
    }

    public Vector4d normalize(Vector4d dest) {
        double invLength = 1.0 / this.length();
        dest.x = this.x * invLength;
        dest.y = this.y * invLength;
        dest.z = this.z * invLength;
        dest.w = this.w * invLength;
        return dest;
    }

    public Vector4d normalize(double length) {
        double invLength = 1.0 / this.length() * length;
        this.x *= invLength;
        this.y *= invLength;
        this.z *= invLength;
        this.w *= invLength;
        return this;
    }

    public Vector4d normalize(double length, Vector4d dest) {
        double invLength = 1.0 / this.length() * length;
        dest.x = this.x * invLength;
        dest.y = this.y * invLength;
        dest.z = this.z * invLength;
        dest.w = this.w * invLength;
        return dest;
    }

    public Vector4d normalize3() {
        double invLength = Math.invsqrt(Math.fma(this.x, this.x, Math.fma(this.y, this.y, this.z * this.z)));
        this.x *= invLength;
        this.y *= invLength;
        this.z *= invLength;
        this.w *= invLength;
        return this;
    }

    public Vector4d normalize3(Vector4d dest) {
        double invLength = Math.invsqrt(Math.fma(this.x, this.x, Math.fma(this.y, this.y, this.z * this.z)));
        dest.x = this.x * invLength;
        dest.y = this.y * invLength;
        dest.z = this.z * invLength;
        dest.w = this.w * invLength;
        return dest;
    }

    public double distance(Vector4d v) {
        double dx = this.x - v.x;
        double dy = this.y - v.y;
        double dz = this.z - v.z;
        double dw = this.w - v.w;
        return Math.sqrt(Math.fma(dx, dx, Math.fma(dy, dy, Math.fma(dz, dz, dw * dw))));
    }

    public double distance(double x, double y, double z, double w) {
        double dx = this.x - x;
        double dy = this.y - y;
        double dz = this.z - z;
        double dw = this.w - w;
        return Math.sqrt(Math.fma(dx, dx, Math.fma(dy, dy, Math.fma(dz, dz, dw * dw))));
    }

    public double distanceSquared(Vector4d v) {
        double dx = this.x - v.x;
        double dy = this.y - v.y;
        double dz = this.z - v.z;
        double dw = this.w - v.w;
        return Math.fma(dx, dx, Math.fma(dy, dy, Math.fma(dz, dz, dw * dw)));
    }

    public double distanceSquared(double x, double y, double z, double w) {
        double dx = this.x - x;
        double dy = this.y - y;
        double dz = this.z - z;
        double dw = this.w - w;
        return Math.fma(dx, dx, Math.fma(dy, dy, Math.fma(dz, dz, dw * dw)));
    }

    public static double distance(double x1, double y1, double z1, double w1, double x2, double y2, double z2, double w2) {
        double dx = x1 - x2;
        double dy = y1 - y2;
        double dz = z1 - z2;
        double dw = w1 - w2;
        return Math.sqrt(Math.fma(dx, dx, Math.fma(dy, dy, Math.fma(dz, dz, dw * dw))));
    }

    public static double distanceSquared(double x1, double y1, double z1, double w1, double x2, double y2, double z2, double w2) {
        double dx = x1 - x2;
        double dy = y1 - y2;
        double dz = z1 - z2;
        double dw = w1 - w2;
        return Math.fma(dx, dx, Math.fma(dy, dy, Math.fma(dz, dz, dw * dw)));
    }

    public double dot(Vector4d v) {
        return Math.fma(this.x, v.x, Math.fma(this.y, v.y, Math.fma(this.z, v.z, this.w * v.w)));
    }

    public double dot(double x, double y, double z, double w) {
        return Math.fma(this.x, x, Math.fma(this.y, y, Math.fma(this.z, z, this.w * w)));
    }

    public double angleCos(Vector4d v) {
        double length1Squared = Math.fma(this.x, this.x, Math.fma(this.y, this.y, Math.fma(this.z, this.z, this.w * this.w)));
        double length2Squared = Math.fma(v.x, v.x, Math.fma(v.y, v.y, Math.fma(v.z, v.z, v.w * v.w)));
        double dot = Math.fma(this.x, v.x, Math.fma(this.y, v.y, Math.fma(this.z, v.z, this.w * v.w)));
        return dot / Math.sqrt(length1Squared * length2Squared);
    }

    public double angle(Vector4d v) {
        double cos = this.angleCos(v);
        cos = java.lang.Math.min(cos, 1.0);
        cos = java.lang.Math.max(cos, -1.0);
        return Math.acos(cos);
    }

    public Vector4d zero() {
        this.x = 0.0;
        this.y = 0.0;
        this.z = 0.0;
        this.w = 0.0;
        return this;
    }

    public Vector4d negate() {
        this.x = -this.x;
        this.y = -this.y;
        this.z = -this.z;
        this.w = -this.w;
        return this;
    }

    public Vector4d negate(Vector4d dest) {
        dest.x = -this.x;
        dest.y = -this.y;
        dest.z = -this.z;
        dest.w = -this.w;
        return dest;
    }

    public Vector4d min(Vector4d v) {
        this.x = java.lang.Math.min(this.x, v.x);
        this.y = java.lang.Math.min(this.y, v.y);
        this.z = java.lang.Math.min(this.z, v.z);
        this.w = java.lang.Math.min(this.w, v.w);
        return this;
    }

    public Vector4d min(Vector4d v, Vector4d dest) {
        dest.x = java.lang.Math.min(this.x, v.x);
        dest.y = java.lang.Math.min(this.y, v.y);
        dest.z = java.lang.Math.min(this.z, v.z);
        dest.w = java.lang.Math.min(this.w, v.w);
        return dest;
    }

    public Vector4d max(Vector4d v) {
        this.x = java.lang.Math.max(this.x, v.x);
        this.y = java.lang.Math.max(this.y, v.y);
        this.z = java.lang.Math.max(this.z, v.z);
        this.w = java.lang.Math.max(this.w, v.w);
        return this;
    }

    public Vector4d max(Vector4d v, Vector4d dest) {
        dest.x = java.lang.Math.max(this.x, v.x);
        dest.y = java.lang.Math.max(this.y, v.y);
        dest.z = java.lang.Math.max(this.z, v.z);
        dest.w = java.lang.Math.max(this.w, v.w);
        return dest;
    }

    public String toString() {
        return "(" + x+","+y+","+z+","+w + ")";
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
            Vector4d other = (Vector4d) obj;
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

    public boolean equals(Vector4d v, double delta) {
        if (this == v) {
            return true;
        } else if (v == null) {
            return false;
        } else if (!Runtime.equals(this.x, v.x, delta)) {
            return false;
        } else if (!Runtime.equals(this.y, v.y, delta)) {
            return false;
        } else if (!Runtime.equals(this.z, v.z, delta)) {
            return false;
        } else {
            return Runtime.equals(this.w, v.w, delta);
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

    public Vector4d smoothStep(Vector4d v, double t, Vector4d dest) {
        double t2 = t * t;
        double t3 = t2 * t;
        dest.x = (this.x + this.x - v.x - v.x) * t3 + (3.0 * v.x - 3.0 * this.x) * t2 + this.x * t + this.x;
        dest.y = (this.y + this.y - v.y - v.y) * t3 + (3.0 * v.y - 3.0 * this.y) * t2 + this.y * t + this.y;
        dest.z = (this.z + this.z - v.z - v.z) * t3 + (3.0 * v.z - 3.0 * this.z) * t2 + this.z * t + this.z;
        dest.w = (this.w + this.w - v.w - v.w) * t3 + (3.0 * v.w - 3.0 * this.w) * t2 + this.w * t + this.w;
        return dest;
    }

    public Vector4d hermite(Vector4d t0, Vector4d v1, Vector4d t1, double t, Vector4d dest) {
        double t2 = t * t;
        double t3 = t2 * t;
        dest.x = (this.x + this.x - v1.x - v1.x + t1.x + t0.x) * t3 + (3.0 * v1.x - 3.0 * this.x - t0.x - t0.x - t1.x) * t2 + this.x * t + this.x;
        dest.y = (this.y + this.y - v1.y - v1.y + t1.y + t0.y) * t3 + (3.0 * v1.y - 3.0 * this.y - t0.y - t0.y - t1.y) * t2 + this.y * t + this.y;
        dest.z = (this.z + this.z - v1.z - v1.z + t1.z + t0.z) * t3 + (3.0 * v1.z - 3.0 * this.z - t0.z - t0.z - t1.z) * t2 + this.z * t + this.z;
        dest.w = (this.w + this.w - v1.w - v1.w + t1.w + t0.w) * t3 + (3.0 * v1.w - 3.0 * this.w - t0.w - t0.w - t1.w) * t2 + this.w * t + this.w;
        return dest;
    }

    public Vector4d lerp(Vector4d other, double t) {
        this.x = Math.fma(other.x - this.x, t, this.x);
        this.y = Math.fma(other.y - this.y, t, this.y);
        this.z = Math.fma(other.z - this.z, t, this.z);
        this.w = Math.fma(other.w - this.w, t, this.w);
        return this;
    }

    public Vector4d lerp(Vector4d other, double t, Vector4d dest) {
        dest.x = Math.fma(other.x - this.x, t, this.x);
        dest.y = Math.fma(other.y - this.y, t, this.y);
        dest.z = Math.fma(other.z - this.z, t, this.z);
        dest.w = Math.fma(other.w - this.w, t, this.w);
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
            case 3:
                return this.w;
            default:
                throw new IllegalArgumentException();
        }
    }

    public Vector4i get(int mode, Vector4i dest) {
        dest.x = Math.roundUsing(this.x, mode);
        dest.y = Math.roundUsing(this.y, mode);
        dest.z = Math.roundUsing(this.z, mode);
        dest.w = Math.roundUsing(this.w, mode);
        return dest;
    }

    public Vector4f get(Vector4f dest) {
        dest.x = (float) this.x;
        dest.y = (float) this.y;
        dest.z = (float) this.z;
        dest.w = (float) this.w;
        return dest;
    }

    public Vector4d get(Vector4d dest) {
        dest.x = this.x;
        dest.y = this.y;
        dest.z = this.z;
        dest.w = this.w;
        return dest;
    }

    public int maxComponent() {
        double absX = Math.abs(this.x);
        double absY = Math.abs(this.y);
        double absZ = Math.abs(this.z);
        double absW = Math.abs(this.w);
        if (absX >= absY && absX >= absZ && absX >= absW) {
            return 0;
        } else if (absY >= absZ && absY >= absW) {
            return 1;
        } else {
            return absZ >= absW ? 2 : 3;
        }
    }

    public int minComponent() {
        double absX = Math.abs(this.x);
        double absY = Math.abs(this.y);
        double absZ = Math.abs(this.z);
        double absW = Math.abs(this.w);
        if (absX < absY && absX < absZ && absX < absW) {
            return 0;
        } else if (absY < absZ && absY < absW) {
            return 1;
        } else {
            return absZ < absW ? 2 : 3;
        }
    }

    public Vector4d floor() {
        this.x = Math.floor(this.x);
        this.y = Math.floor(this.y);
        this.z = Math.floor(this.z);
        this.w = Math.floor(this.w);
        return this;
    }

    public Vector4d floor(Vector4d dest) {
        dest.x = Math.floor(this.x);
        dest.y = Math.floor(this.y);
        dest.z = Math.floor(this.z);
        dest.w = Math.floor(this.w);
        return dest;
    }

    public Vector4d ceil() {
        this.x = Math.ceil(this.x);
        this.y = Math.ceil(this.y);
        this.z = Math.ceil(this.z);
        this.w = Math.ceil(this.w);
        return this;
    }

    public Vector4d ceil(Vector4d dest) {
        dest.x = Math.ceil(this.x);
        dest.y = Math.ceil(this.y);
        dest.z = Math.ceil(this.z);
        dest.w = Math.ceil(this.w);
        return dest;
    }

    public Vector4d round() {
        this.x = (double) Math.round(this.x);
        this.y = (double) Math.round(this.y);
        this.z = (double) Math.round(this.z);
        this.w = (double) Math.round(this.w);
        return this;
    }

    public Vector4d round(Vector4d dest) {
        dest.x = (double) Math.round(this.x);
        dest.y = (double) Math.round(this.y);
        dest.z = (double) Math.round(this.z);
        dest.w = (double) Math.round(this.w);
        return dest;
    }

    public boolean isFinite() {
        return Math.isFinite(this.x) && Math.isFinite(this.y) && Math.isFinite(this.z) && Math.isFinite(this.w);
    }

    public Vector4d absolute() {
        this.x = Math.abs(this.x);
        this.y = Math.abs(this.y);
        this.z = Math.abs(this.z);
        this.w = Math.abs(this.w);
        return this;
    }

    public Vector4d absolute(Vector4d dest) {
        dest.x = Math.abs(this.x);
        dest.y = Math.abs(this.y);
        dest.z = Math.abs(this.z);
        dest.w = Math.abs(this.w);
        return dest;
    }

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
