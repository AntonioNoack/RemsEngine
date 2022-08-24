package org.joml;

@SuppressWarnings("unused")
public class Vector4f {

    public float x;
    public float y;
    public float z;
    public float w;

    public Vector4f() {
        this.w = 1f;
    }

    public Vector4f(Vector4f v) {
        this.x = v.x;
        this.y = v.y;
        this.z = v.z;
        this.w = v.w;
    }

    public Vector4f(Vector4i v) {
        this.x = (float) v.x;
        this.y = (float) v.y;
        this.z = (float) v.z;
        this.w = (float) v.w;
    }

    public Vector4f(Vector3f v, float w) {
        this.x = v.x;
        this.y = v.y;
        this.z = v.z;
        this.w = w;
    }

    public Vector4f(Vector3i v, float w) {
        this.x = (float) v.x;
        this.y = (float) v.y;
        this.z = (float) v.z;
        this.w = w;
    }

    public Vector4f(Vector2f v, float z, float w) {
        this.x = v.x;
        this.y = v.y;
        this.z = z;
        this.w = w;
    }

    public Vector4f(Vector2i v, float z, float w) {
        this.x = (float) v.x;
        this.y = (float) v.y;
        this.z = z;
        this.w = w;
    }

    public Vector4f(float d) {
        this.x = d;
        this.y = d;
        this.z = d;
        this.w = d;
    }

    public Vector4f(float x, float y, float z, float w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
    }

    public Vector4f(float[] xyzw) {
        this.x = xyzw[0];
        this.y = xyzw[1];
        this.z = xyzw[2];
        this.w = xyzw[3];
    }

    public Vector4f set(Vector4f v) {
        this.x = v.x;
        this.y = v.y;
        this.z = v.z;
        this.w = v.w;
        return this;
    }

    public Vector4f set(Vector4i v) {
        this.x = (float) v.x;
        this.y = (float) v.y;
        this.z = (float) v.z;
        this.w = (float) v.w;
        return this;
    }

    public Vector4f set(Vector4d v) {
        this.x = (float) v.x;
        this.y = (float) v.y;
        this.z = (float) v.z;
        this.w = (float) v.w;
        return this;
    }

    public Vector4f set(Vector3f v, float w) {
        this.x = v.x;
        this.y = v.y;
        this.z = v.z;
        this.w = w;
        return this;
    }

    public Vector4f set(Vector3i v, float w) {
        this.x = (float) v.x;
        this.y = (float) v.y;
        this.z = (float) v.z;
        this.w = w;
        return this;
    }

    public Vector4f set(Vector2f v, float z, float w) {
        this.x = v.x;
        this.y = v.y;
        this.z = z;
        this.w = w;
        return this;
    }

    public Vector4f set(Vector2i v, float z, float w) {
        this.x = (float) v.x;
        this.y = (float) v.y;
        this.z = z;
        this.w = w;
        return this;
    }

    public Vector4f set(float d) {
        this.x = d;
        this.y = d;
        this.z = d;
        this.w = d;
        return this;
    }

    public Vector4f set(float x, float y, float z, float w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
        return this;
    }

    public Vector4f set(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
        return this;
    }

    public Vector4f set(double d) {
        this.x = (float) d;
        this.y = (float) d;
        this.z = (float) d;
        this.w = (float) d;
        return this;
    }

    public Vector4f set(double x, double y, double z, double w) {
        this.x = (float) x;
        this.y = (float) y;
        this.z = (float) z;
        this.w = (float) w;
        return this;
    }

    public Vector4f set(float[] xyzw) {
        this.x = xyzw[0];
        this.y = xyzw[1];
        this.z = xyzw[2];
        this.w = xyzw[3];
        return this;
    }

    public Vector4f setComponent(int component, float value) throws IllegalArgumentException {
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

    public Vector4f sub(Vector4f v) {
        this.x -= v.x;
        this.y -= v.y;
        this.z -= v.z;
        this.w -= v.w;
        return this;
    }

    public Vector4f sub(float x, float y, float z, float w) {
        this.x -= x;
        this.y -= y;
        this.z -= z;
        this.w -= w;
        return this;
    }

    public Vector4f sub(Vector4f v, Vector4f dest) {
        dest.x = this.x - v.x;
        dest.y = this.y - v.y;
        dest.z = this.z - v.z;
        dest.w = this.w - v.w;
        return dest;
    }

    public Vector4f sub(float x, float y, float z, float w, Vector4f dest) {
        dest.x = this.x - x;
        dest.y = this.y - y;
        dest.z = this.z - z;
        dest.w = this.w - w;
        return dest;
    }

    public Vector4f add(Vector4f v) {
        this.x += v.x;
        this.y += v.y;
        this.z += v.z;
        this.w += v.w;
        return this;
    }

    public Vector4f add(Vector4f v, Vector4f dest) {
        dest.x = this.x + v.x;
        dest.y = this.y + v.y;
        dest.z = this.z + v.z;
        dest.w = this.w + v.w;
        return dest;
    }

    public Vector4f add(float x, float y, float z, float w) {
        this.x += x;
        this.y += y;
        this.z += z;
        this.w += w;
        return this;
    }

    public Vector4f add(float x, float y, float z, float w, Vector4f dest) {
        dest.x = this.x + x;
        dest.y = this.y + y;
        dest.z = this.z + z;
        dest.w = this.w + w;
        return dest;
    }

    public Vector4f fma(Vector4f a, Vector4f b) {
        this.x = Math.fma(a.x, b.x, this.x);
        this.y = Math.fma(a.y, b.y, this.y);
        this.z = Math.fma(a.z, b.z, this.z);
        this.w = Math.fma(a.w, b.w, this.w);
        return this;
    }

    public Vector4f fma(float a, Vector4f b) {
        this.x = Math.fma(a, b.x, this.x);
        this.y = Math.fma(a, b.y, this.y);
        this.z = Math.fma(a, b.z, this.z);
        this.w = Math.fma(a, b.w, this.w);
        return this;
    }

    public Vector4f fma(Vector4f a, Vector4f b, Vector4f dest) {
        dest.x = Math.fma(a.x, b.x, this.x);
        dest.y = Math.fma(a.y, b.y, this.y);
        dest.z = Math.fma(a.z, b.z, this.z);
        dest.w = Math.fma(a.w, b.w, this.w);
        return dest;
    }

    public Vector4f fma(float a, Vector4f b, Vector4f dest) {
        dest.x = Math.fma(a, b.x, this.x);
        dest.y = Math.fma(a, b.y, this.y);
        dest.z = Math.fma(a, b.z, this.z);
        dest.w = Math.fma(a, b.w, this.w);
        return dest;
    }

    public Vector4f mulAdd(Vector4f a, Vector4f b) {
        this.x = Math.fma(this.x, a.x, b.x);
        this.y = Math.fma(this.y, a.y, b.y);
        this.z = Math.fma(this.z, a.z, b.z);
        return this;
    }

    public Vector4f mulAdd(float a, Vector4f b) {
        this.x = Math.fma(this.x, a, b.x);
        this.y = Math.fma(this.y, a, b.y);
        this.z = Math.fma(this.z, a, b.z);
        return this;
    }

    public Vector4f mulAdd(Vector4f a, Vector4f b, Vector4f dest) {
        dest.x = Math.fma(this.x, a.x, b.x);
        dest.y = Math.fma(this.y, a.y, b.y);
        dest.z = Math.fma(this.z, a.z, b.z);
        return dest;
    }

    public Vector4f mulAdd(float a, Vector4f b, Vector4f dest) {
        dest.x = Math.fma(this.x, a, b.x);
        dest.y = Math.fma(this.y, a, b.y);
        dest.z = Math.fma(this.z, a, b.z);
        return dest;
    }

    public Vector4f mul(Vector4f v) {
        this.x *= v.x;
        this.y *= v.y;
        this.z *= v.z;
        this.w *= v.w;
        return this;
    }

    public Vector4f mul(Vector4f v, Vector4f dest) {
        dest.x = this.x * v.x;
        dest.y = this.y * v.y;
        dest.z = this.z * v.z;
        dest.w = this.w * v.w;
        return dest;
    }

    public Vector4f div(Vector4f v) {
        this.x /= v.x;
        this.y /= v.y;
        this.z /= v.z;
        this.w /= v.w;
        return this;
    }

    public Vector4f div(Vector4f v, Vector4f dest) {
        dest.x = this.x / v.x;
        dest.y = this.y / v.y;
        dest.z = this.z / v.z;
        dest.w = this.w / v.w;
        return dest;
    }

    public Vector4f mul(Matrix4f mat) {
        return (mat.properties() & 2) != 0 ? this.mulAffine(mat, this) : this.mulGeneric(mat, this);
    }

    public Vector4f mul(Matrix4f mat, Vector4f dest) {
        return (mat.properties() & 2) != 0 ? this.mulAffine(mat, dest) : this.mulGeneric(mat, dest);
    }

    public Vector4f mulTranspose(Matrix4f mat) {
        return (mat.properties() & 2) != 0 ? this.mulAffineTranspose(mat, this) : this.mulGenericTranspose(mat, this);
    }

    public Vector4f mulTranspose(Matrix4f mat, Vector4f dest) {
        return (mat.properties() & 2) != 0 ? this.mulAffineTranspose(mat, dest) : this.mulGenericTranspose(mat, dest);
    }

    public Vector4f mulAffine(Matrix4f mat, Vector4f dest) {
        float x = this.x;
        float y = this.y;
        float z = this.z;
        float w = this.w;
        dest.x = Math.fma(mat.m00(), x, Math.fma(mat.m10(), y, Math.fma(mat.m20(), z, mat.m30() * w)));
        dest.y = Math.fma(mat.m01(), x, Math.fma(mat.m11(), y, Math.fma(mat.m21(), z, mat.m31() * w)));
        dest.z = Math.fma(mat.m02(), x, Math.fma(mat.m12(), y, Math.fma(mat.m22(), z, mat.m32() * w)));
        dest.w = w;
        return dest;
    }

    private Vector4f mulGeneric(Matrix4f mat, Vector4f dest) {
        float x = this.x;
        float y = this.y;
        float z = this.z;
        float w = this.w;
        dest.x = Math.fma(mat.m00(), x, Math.fma(mat.m10(), y, Math.fma(mat.m20(), z, mat.m30() * w)));
        dest.y = Math.fma(mat.m01(), x, Math.fma(mat.m11(), y, Math.fma(mat.m21(), z, mat.m31() * w)));
        dest.z = Math.fma(mat.m02(), x, Math.fma(mat.m12(), y, Math.fma(mat.m22(), z, mat.m32() * w)));
        dest.w = Math.fma(mat.m03(), x, Math.fma(mat.m13(), y, Math.fma(mat.m23(), z, mat.m33() * w)));
        return dest;
    }

    public Vector4f mulAffineTranspose(Matrix4f mat, Vector4f dest) {
        float x = this.x;
        float y = this.y;
        float z = this.z;
        float w = this.w;
        dest.x = Math.fma(mat.m00(), x, Math.fma(mat.m01(), y, mat.m02() * z));
        dest.y = Math.fma(mat.m10(), x, Math.fma(mat.m11(), y, mat.m12() * z));
        dest.z = Math.fma(mat.m20(), x, Math.fma(mat.m21(), y, mat.m22() * z));
        dest.w = Math.fma(mat.m30(), x, Math.fma(mat.m31(), y, mat.m32() * z + w));
        return dest;
    }

    private Vector4f mulGenericTranspose(Matrix4f mat, Vector4f dest) {
        float x = this.x;
        float y = this.y;
        float z = this.z;
        float w = this.w;
        dest.x = Math.fma(mat.m00(), x, Math.fma(mat.m01(), y, Math.fma(mat.m02(), z, mat.m03() * w)));
        dest.y = Math.fma(mat.m10(), x, Math.fma(mat.m11(), y, Math.fma(mat.m12(), z, mat.m13() * w)));
        dest.z = Math.fma(mat.m20(), x, Math.fma(mat.m21(), y, Math.fma(mat.m22(), z, mat.m23() * w)));
        dest.w = Math.fma(mat.m30(), x, Math.fma(mat.m31(), y, Math.fma(mat.m32(), z, mat.m33() * w)));
        return dest;
    }

    public Vector4f mul(Matrix4x3f mat) {
        float x = this.x;
        float y = this.y;
        float z = this.z;
        float w = this.w;
        this.x = Math.fma(mat.m00(), x, Math.fma(mat.m10(), y, Math.fma(mat.m20(), z, mat.m30() * w)));
        this.y = Math.fma(mat.m01(), x, Math.fma(mat.m11(), y, Math.fma(mat.m21(), z, mat.m31() * w)));
        this.z = Math.fma(mat.m02(), x, Math.fma(mat.m12(), y, Math.fma(mat.m22(), z, mat.m32() * w)));
        this.w = w;
        return this;
    }

    public Vector4f mul(Matrix4x3f mat, Vector4f dest) {
        float x = this.x;
        float y = this.y;
        float z = this.z;
        float w = this.w;
        dest.x = Math.fma(mat.m00(), x, Math.fma(mat.m10(), y, Math.fma(mat.m20(), z, mat.m30() * w)));
        dest.y = Math.fma(mat.m01(), x, Math.fma(mat.m11(), y, Math.fma(mat.m21(), z, mat.m31() * w)));
        dest.z = Math.fma(mat.m02(), x, Math.fma(mat.m12(), y, Math.fma(mat.m22(), z, mat.m32() * w)));
        dest.w = w;
        return dest;
    }

    public Vector4f mulProject(Matrix4f mat, Vector4f dest) {
        float x = this.x;
        float y = this.y;
        float z = this.z;
        float w = this.w;
        float invW = 1f / Math.fma(mat.m03(), x, Math.fma(mat.m13(), y, Math.fma(mat.m23(), z, mat.m33() * w)));
        dest.x = Math.fma(mat.m00(), x, Math.fma(mat.m10(), y, Math.fma(mat.m20(), z, mat.m30() * w))) * invW;
        dest.y = Math.fma(mat.m01(), x, Math.fma(mat.m11(), y, Math.fma(mat.m21(), z, mat.m31() * w))) * invW;
        dest.z = Math.fma(mat.m02(), x, Math.fma(mat.m12(), y, Math.fma(mat.m22(), z, mat.m32() * w))) * invW;
        dest.w = 1f;
        return dest;
    }

    public Vector4f mulProject(Matrix4f mat) {
        float x = this.x;
        float y = this.y;
        float z = this.z;
        float w = this.w;
        float invW = 1f / Math.fma(mat.m03(), x, Math.fma(mat.m13(), y, Math.fma(mat.m23(), z, mat.m33() * w)));
        this.x = Math.fma(mat.m00(), x, Math.fma(mat.m10(), y, Math.fma(mat.m20(), z, mat.m30() * w))) * invW;
        this.y = Math.fma(mat.m01(), x, Math.fma(mat.m11(), y, Math.fma(mat.m21(), z, mat.m31() * w))) * invW;
        this.z = Math.fma(mat.m02(), x, Math.fma(mat.m12(), y, Math.fma(mat.m22(), z, mat.m32() * w))) * invW;
        this.w = 1f;
        return this;
    }

    public Vector3f mulProject(Matrix4f mat, Vector3f dest) {
        float x = this.x;
        float y = this.y;
        float z = this.z;
        float w = this.w;
        float invW = 1f / Math.fma(mat.m03(), x, Math.fma(mat.m13(), y, Math.fma(mat.m23(), z, mat.m33() * w)));
        dest.x = Math.fma(mat.m00(), x, Math.fma(mat.m10(), y, Math.fma(mat.m20(), z, mat.m30() * w))) * invW;
        dest.y = Math.fma(mat.m01(), x, Math.fma(mat.m11(), y, Math.fma(mat.m21(), z, mat.m31() * w))) * invW;
        dest.z = Math.fma(mat.m02(), x, Math.fma(mat.m12(), y, Math.fma(mat.m22(), z, mat.m32() * w))) * invW;
        return dest;
    }

    public Vector4f mul(float scalar) {
        this.x *= scalar;
        this.y *= scalar;
        this.z *= scalar;
        this.w *= scalar;
        return this;
    }

    public Vector4f mul(float scalar, Vector4f dest) {
        dest.x = this.x * scalar;
        dest.y = this.y * scalar;
        dest.z = this.z * scalar;
        dest.w = this.w * scalar;
        return dest;
    }

    public Vector4f mul(float x, float y, float z, float w) {
        this.x *= x;
        this.y *= y;
        this.z *= z;
        this.w *= w;
        return this;
    }

    public Vector4f mul(float x, float y, float z, float w, Vector4f dest) {
        dest.x = this.x * x;
        dest.y = this.y * y;
        dest.z = this.z * z;
        dest.w = this.w * w;
        return dest;
    }

    public Vector4f div(float scalar) {
        float inv = 1f / scalar;
        this.x *= inv;
        this.y *= inv;
        this.z *= inv;
        this.w *= inv;
        return this;
    }

    public Vector4f div(float scalar, Vector4f dest) {
        float inv = 1f / scalar;
        dest.x = this.x * inv;
        dest.y = this.y * inv;
        dest.z = this.z * inv;
        dest.w = this.w * inv;
        return dest;
    }

    public Vector4f div(float x, float y, float z, float w) {
        this.x /= x;
        this.y /= y;
        this.z /= z;
        this.w /= w;
        return this;
    }

    public Vector4f div(float x, float y, float z, float w, Vector4f dest) {
        dest.x = this.x / x;
        dest.y = this.y / y;
        dest.z = this.z / z;
        dest.w = this.w / w;
        return dest;
    }

    public Vector4f rotate(Quaternionf quat) {
        return quat.transform(this, this);
    }

    public Vector4f rotate(Quaternionf quat, Vector4f dest) {
        return quat.transform(this, dest);
    }

    public Vector4f rotateAbout(float angle, float x, float y, float z) {
        if (y == 0f && z == 0f && Math.absEqualsOne(x)) {
            return this.rotateX(x * angle, this);
        } else if (x == 0f && z == 0f && Math.absEqualsOne(y)) {
            return this.rotateY(y * angle, this);
        } else {
            return x == 0f && y == 0f && Math.absEqualsOne(z) ? this.rotateZ(z * angle, this) : this.rotateAxisInternal(angle, x, y, z, this);
        }
    }

    public Vector4f rotateAxis(float angle, float aX, float aY, float aZ, Vector4f dest) {
        if (aY == 0f && aZ == 0f && Math.absEqualsOne(aX)) {
            return this.rotateX(aX * angle, dest);
        } else if (aX == 0f && aZ == 0f && Math.absEqualsOne(aY)) {
            return this.rotateY(aY * angle, dest);
        } else {
            return aX == 0f && aY == 0f && Math.absEqualsOne(aZ) ? this.rotateZ(aZ * angle, dest) : this.rotateAxisInternal(angle, aX, aY, aZ, dest);
        }
    }

    private Vector4f rotateAxisInternal(float angle, float aX, float aY, float aZ, Vector4f dest) {
        float halfAngle = angle * 0.5f;
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

    public Vector4f rotateX(float angle) {
        float sin = Math.sin(angle);
        float cos = Math.cosFromSin(sin, angle);
        float y = this.y * cos - this.z * sin;
        float z = this.y * sin + this.z * cos;
        this.y = y;
        this.z = z;
        return this;
    }

    public Vector4f rotateX(float angle, Vector4f dest) {
        float sin = Math.sin(angle);
        float cos = Math.cosFromSin(sin, angle);
        float y = this.y * cos - this.z * sin;
        float z = this.y * sin + this.z * cos;
        dest.x = this.x;
        dest.y = y;
        dest.z = z;
        dest.w = this.w;
        return dest;
    }

    public Vector4f rotateY(float angle) {
        float sin = Math.sin(angle);
        float cos = Math.cosFromSin(sin, angle);
        float x = this.x * cos + this.z * sin;
        float z = -this.x * sin + this.z * cos;
        this.x = x;
        this.z = z;
        return this;
    }

    public Vector4f rotateY(float angle, Vector4f dest) {
        float sin = Math.sin(angle);
        float cos = Math.cosFromSin(sin, angle);
        float x = this.x * cos + this.z * sin;
        float z = -this.x * sin + this.z * cos;
        dest.x = x;
        dest.y = this.y;
        dest.z = z;
        dest.w = this.w;
        return dest;
    }

    public Vector4f rotateZ(float angle) {
        float sin = Math.sin(angle);
        float cos = Math.cosFromSin(sin, angle);
        float x = this.x * cos - this.y * sin;
        float y = this.x * sin + this.y * cos;
        this.x = x;
        this.y = y;
        return this;
    }

    public Vector4f rotateZ(float angle, Vector4f dest) {
        float sin = Math.sin(angle);
        float cos = Math.cosFromSin(sin, angle);
        float x = this.x * cos - this.y * sin;
        float y = this.x * sin + this.y * cos;
        dest.x = x;
        dest.y = y;
        dest.z = this.z;
        dest.w = this.w;
        return dest;
    }

    public float lengthSquared() {
        return Math.fma(this.x, this.x, Math.fma(this.y, this.y, Math.fma(this.z, this.z, this.w * this.w)));
    }

    public static float lengthSquared(float x, float y, float z, float w) {
        return Math.fma(x, x, Math.fma(y, y, Math.fma(z, z, w * w)));
    }

    public static float lengthSquared(int x, int y, int z, int w) {
        return Math.fma((float) x, (float) x, Math.fma((float) y, (float) y, Math.fma((float) z, (float) z, (float) (w * w))));
    }

    public float length() {
        return Math.sqrt(Math.fma(this.x, this.x, Math.fma(this.y, this.y, Math.fma(this.z, this.z, this.w * this.w))));
    }

    public static float length(float x, float y, float z, float w) {
        return Math.sqrt(Math.fma(x, x, Math.fma(y, y, Math.fma(z, z, w * w))));
    }

    public Vector4f normalize() {
        float invLength = 1f / this.length();
        this.x *= invLength;
        this.y *= invLength;
        this.z *= invLength;
        this.w *= invLength;
        return this;
    }

    public Vector4f normalize(Vector4f dest) {
        float invLength = 1f / this.length();
        dest.x = this.x * invLength;
        dest.y = this.y * invLength;
        dest.z = this.z * invLength;
        dest.w = this.w * invLength;
        return dest;
    }

    public Vector4f normalize(float length) {
        float invLength = 1f / this.length() * length;
        this.x *= invLength;
        this.y *= invLength;
        this.z *= invLength;
        this.w *= invLength;
        return this;
    }

    public Vector4f normalize(float length, Vector4f dest) {
        float invLength = 1f / this.length() * length;
        dest.x = this.x * invLength;
        dest.y = this.y * invLength;
        dest.z = this.z * invLength;
        dest.w = this.w * invLength;
        return dest;
    }

    public Vector4f normalize3() {
        float invLength = Math.invsqrt(Math.fma(this.x, this.x, Math.fma(this.y, this.y, this.z * this.z)));
        this.x *= invLength;
        this.y *= invLength;
        this.z *= invLength;
        this.w *= invLength;
        return this;
    }

    public Vector4f normalize3(Vector4f dest) {
        float invLength = Math.invsqrt(Math.fma(this.x, this.x, Math.fma(this.y, this.y, this.z * this.z)));
        dest.x = this.x * invLength;
        dest.y = this.y * invLength;
        dest.z = this.z * invLength;
        dest.w = this.w * invLength;
        return dest;
    }

    public float distance(Vector4f v) {
        float dx = this.x - v.x;
        float dy = this.y - v.y;
        float dz = this.z - v.z;
        float dw = this.w - v.w;
        return Math.sqrt(Math.fma(dx, dx, Math.fma(dy, dy, Math.fma(dz, dz, dw * dw))));
    }

    public float distance(float x, float y, float z, float w) {
        float dx = this.x - x;
        float dy = this.y - y;
        float dz = this.z - z;
        float dw = this.w - w;
        return Math.sqrt(Math.fma(dx, dx, Math.fma(dy, dy, Math.fma(dz, dz, dw * dw))));
    }

    public float distanceSquared(Vector4f v) {
        float dx = this.x - v.x;
        float dy = this.y - v.y;
        float dz = this.z - v.z;
        float dw = this.w - v.w;
        return Math.fma(dx, dx, Math.fma(dy, dy, Math.fma(dz, dz, dw * dw)));
    }

    public float distanceSquared(float x, float y, float z, float w) {
        float dx = this.x - x;
        float dy = this.y - y;
        float dz = this.z - z;
        float dw = this.w - w;
        return Math.fma(dx, dx, Math.fma(dy, dy, Math.fma(dz, dz, dw * dw)));
    }

    public static float distance(float x1, float y1, float z1, float w1, float x2, float y2, float z2, float w2) {
        float dx = x1 - x2;
        float dy = y1 - y2;
        float dz = z1 - z2;
        float dw = w1 - w2;
        return Math.sqrt(Math.fma(dx, dx, Math.fma(dy, dy, Math.fma(dz, dz, dw * dw))));
    }

    public static float distanceSquared(float x1, float y1, float z1, float w1, float x2, float y2, float z2, float w2) {
        float dx = x1 - x2;
        float dy = y1 - y2;
        float dz = z1 - z2;
        float dw = w1 - w2;
        return Math.fma(dx, dx, Math.fma(dy, dy, Math.fma(dz, dz, dw * dw)));
    }

    public float dot(Vector4f v) {
        return Math.fma(this.x, v.x, Math.fma(this.y, v.y, Math.fma(this.z, v.z, this.w * v.w)));
    }

    public float dot(float x, float y, float z, float w) {
        return Math.fma(this.x, x, Math.fma(this.y, y, Math.fma(this.z, z, this.w * w)));
    }

    public float angleCos(Vector4f v) {
        float x = this.x;
        float y = this.y;
        float z = this.z;
        float w = this.w;
        float length1Squared = Math.fma(x, x, Math.fma(y, y, Math.fma(z, z, w * w)));
        float length2Squared = Math.fma(v.x, v.x, Math.fma(v.y, v.y, Math.fma(v.z, v.z, v.w * v.w)));
        float dot = Math.fma(x, v.x, Math.fma(y, v.y, Math.fma(z, v.z, w * v.w)));
        return dot / Math.sqrt(length1Squared * length2Squared);
    }

    public float angle(Vector4f v) {
        float cos = this.angleCos(v);
        cos = java.lang.Math.min(cos, 1f);
        cos = java.lang.Math.max(cos, -1f);
        return Math.acos(cos);
    }

    public Vector4f zero() {
        this.x = 0f;
        this.y = 0f;
        this.z = 0f;
        this.w = 0f;
        return this;
    }

    public Vector4f negate() {
        this.x = -this.x;
        this.y = -this.y;
        this.z = -this.z;
        this.w = -this.w;
        return this;
    }

    public Vector4f negate(Vector4f dest) {
        dest.x = -this.x;
        dest.y = -this.y;
        dest.z = -this.z;
        dest.w = -this.w;
        return dest;
    }

    public String toString() {
        return "(" + x + "," + y + "," + z + "," + w + ")";
    }

    public Vector4f min(Vector4f v) {
        float x = this.x;
        float y = this.y;
        float z = this.z;
        float w = this.w;
        this.x = java.lang.Math.min(x, v.x);
        this.y = java.lang.Math.min(y, v.y);
        this.z = java.lang.Math.min(z, v.z);
        this.w = java.lang.Math.min(w, v.w);
        return this;
    }

    public Vector4f min(Vector4f v, Vector4f dest) {
        float x = this.x;
        float y = this.y;
        float z = this.z;
        float w = this.w;
        dest.x = java.lang.Math.min(x, v.x);
        dest.y = java.lang.Math.min(y, v.y);
        dest.z = java.lang.Math.min(z, v.z);
        dest.w = java.lang.Math.min(w, v.w);
        return dest;
    }

    public Vector4f max(Vector4f v) {
        float x = this.x;
        float y = this.y;
        float z = this.z;
        float w = this.w;
        this.x = java.lang.Math.max(x, v.x);
        this.y = java.lang.Math.max(y, v.y);
        this.z = java.lang.Math.max(z, v.z);
        this.w = java.lang.Math.max(w, v.w);
        return this;
    }

    public Vector4f max(Vector4f v, Vector4f dest) {
        float x = this.x;
        float y = this.y;
        float z = this.z;
        float w = this.w;
        dest.x = java.lang.Math.max(x, v.x);
        dest.y = java.lang.Math.max(y, v.y);
        dest.z = java.lang.Math.max(z, v.z);
        dest.w = java.lang.Math.max(w, v.w);
        return dest;
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
            Vector4f other = (Vector4f) obj;
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

    public boolean equals(Vector4f v, float delta) {
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

    public Vector4f smoothStep(Vector4f v, float t, Vector4f dest) {
        float t2 = t * t;
        float t3 = t2 * t;
        float x = this.x;
        float y = this.y;
        float z = this.z;
        float w = this.w;
        dest.x = (x + x - v.x - v.x) * t3 + (3f * v.x - 3f * x) * t2 + x * t + x;
        dest.y = (y + y - v.y - v.y) * t3 + (3f * v.y - 3f * y) * t2 + y * t + y;
        dest.z = (z + z - v.z - v.z) * t3 + (3f * v.z - 3f * z) * t2 + z * t + z;
        dest.w = (w + w - v.w - v.w) * t3 + (3f * v.w - 3f * w) * t2 + w * t + w;
        return dest;
    }

    public Vector4f hermite(Vector4f t0, Vector4f v1, Vector4f t1, float t, Vector4f dest) {
        float t2 = t * t;
        float t3 = t2 * t;
        float x = this.x;
        float y = this.y;
        float z = this.z;
        float w = this.w;
        dest.x = (x + x - v1.x - v1.x + t1.x + t0.x) * t3 + (3f * v1.x - 3f * x - t0.x - t0.x - t1.x) * t2 + x * t + x;
        dest.y = (y + y - v1.y - v1.y + t1.y + t0.y) * t3 + (3f * v1.y - 3f * y - t0.y - t0.y - t1.y) * t2 + y * t + y;
        dest.z = (z + z - v1.z - v1.z + t1.z + t0.z) * t3 + (3f * v1.z - 3f * z - t0.z - t0.z - t1.z) * t2 + z * t + z;
        dest.w = (w + w - v1.w - v1.w + t1.w + t0.w) * t3 + (3f * v1.w - 3f * w - t0.w - t0.w - t1.w) * t2 + w * t + w;
        return dest;
    }

    public Vector4f lerp(Vector4f other, float t) {
        this.x = Math.fma(other.x - this.x, t, this.x);
        this.y = Math.fma(other.y - this.y, t, this.y);
        this.z = Math.fma(other.z - this.z, t, this.z);
        this.w = Math.fma(other.w - this.w, t, this.w);
        return this;
    }

    public Vector4f lerp(Vector4f other, float t, Vector4f dest) {
        dest.x = Math.fma(other.x - this.x, t, this.x);
        dest.y = Math.fma(other.y - this.y, t, this.y);
        dest.z = Math.fma(other.z - this.z, t, this.z);
        dest.w = Math.fma(other.w - this.w, t, this.w);
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
        dest.x = this.x;
        dest.y = this.y;
        dest.z = this.z;
        dest.w = this.w;
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
        float absX = Math.abs(this.x);
        float absY = Math.abs(this.y);
        float absZ = Math.abs(this.z);
        float absW = Math.abs(this.w);
        if (absX >= absY && absX >= absZ && absX >= absW) {
            return 0;
        } else if (absY >= absZ && absY >= absW) {
            return 1;
        } else {
            return absZ >= absW ? 2 : 3;
        }
    }

    public int minComponent() {
        float absX = Math.abs(this.x);
        float absY = Math.abs(this.y);
        float absZ = Math.abs(this.z);
        float absW = Math.abs(this.w);
        if (absX < absY && absX < absZ && absX < absW) {
            return 0;
        } else if (absY < absZ && absY < absW) {
            return 1;
        } else {
            return absZ < absW ? 2 : 3;
        }
    }

    public Vector4f floor() {
        this.x = Math.floor(this.x);
        this.y = Math.floor(this.y);
        this.z = Math.floor(this.z);
        this.w = Math.floor(this.w);
        return this;
    }

    public Vector4f floor(Vector4f dest) {
        dest.x = Math.floor(this.x);
        dest.y = Math.floor(this.y);
        dest.z = Math.floor(this.z);
        dest.w = Math.floor(this.w);
        return dest;
    }

    public Vector4f ceil() {
        this.x = Math.ceil(this.x);
        this.y = Math.ceil(this.y);
        this.z = Math.ceil(this.z);
        this.w = Math.ceil(this.w);
        return this;
    }

    public Vector4f ceil(Vector4f dest) {
        dest.x = Math.ceil(this.x);
        dest.y = Math.ceil(this.y);
        dest.z = Math.ceil(this.z);
        dest.w = Math.ceil(this.w);
        return dest;
    }

    public Vector4f round() {
        this.x = (float) Math.round(this.x);
        this.y = (float) Math.round(this.y);
        this.z = (float) Math.round(this.z);
        this.w = (float) Math.round(this.w);
        return this;
    }

    public Vector4f round(Vector4f dest) {
        dest.x = (float) Math.round(this.x);
        dest.y = (float) Math.round(this.y);
        dest.z = (float) Math.round(this.z);
        dest.w = (float) Math.round(this.w);
        return dest;
    }

    public boolean isFinite() {
        return Math.isFinite(this.x) && Math.isFinite(this.y) && Math.isFinite(this.z) && Math.isFinite(this.w);
    }

    public Vector4f absolute() {
        this.x = Math.abs(this.x);
        this.y = Math.abs(this.y);
        this.z = Math.abs(this.z);
        this.w = Math.abs(this.w);
        return this;
    }

    public Vector4f absolute(Vector4f dest) {
        dest.x = Math.abs(this.x);
        dest.y = Math.abs(this.y);
        dest.z = Math.abs(this.z);
        dest.w = Math.abs(this.w);
        return dest;
    }

}
