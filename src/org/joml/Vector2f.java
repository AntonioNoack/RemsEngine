package org.joml;

@SuppressWarnings("unused")
public class Vector2f {

    public float x;
    public float y;

    public Vector2f() {
    }

    public Vector2f(float d) {
        this.x = d;
        this.y = d;
    }

    public Vector2f(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public Vector2f(Vector2f v) {
        this.x = v.x;
        this.y = v.y;
    }

    public Vector2f(Vector2i v) {
        this.x = (float) v.x;
        this.y = (float) v.y;
    }

    public Vector2f(float[] xy) {
        this.x = xy[0];
        this.y = xy[1];
    }

    public Vector2f set(float d) {
        this.x = d;
        this.y = d;
        return this;
    }

    public Vector2f set(float x, float y) {
        this.x = x;
        this.y = y;
        return this;
    }

    public Vector2f set(double d) {
        this.x = (float) d;
        this.y = (float) d;
        return this;
    }

    public Vector2f set(double x, double y) {
        this.x = (float) x;
        this.y = (float) y;
        return this;
    }

    public Vector2f set(Vector2f v) {
        this.x = v.x;
        this.y = v.y;
        return this;
    }

    public Vector2f set(Vector2i v) {
        this.x = (float) v.x;
        this.y = (float) v.y;
        return this;
    }

    public Vector2f set(Vector2d v) {
        this.x = (float) v.x;
        this.y = (float) v.y;
        return this;
    }

    public Vector2f set(float[] xy) {
        this.x = xy[0];
        this.y = xy[1];
        return this;
    }

    public float get(int component) throws IllegalArgumentException {
        switch (component) {
            case 0:
                return this.x;
            case 1:
                return this.y;
            default:
                throw new IllegalArgumentException();
        }
    }

    public Vector2i get(int mode, Vector2i dest) {
        dest.x = Math.roundUsing(this.x, mode);
        dest.y = Math.roundUsing(this.y, mode);
        return dest;
    }

    public Vector2f get(Vector2f dest) {
        dest.x = this.x;
        dest.y = this.y;
        return dest;
    }

    public Vector2d get(Vector2d dest) {
        dest.x = this.x;
        dest.y = this.y;
        return dest;
    }

    public Vector2f setComponent(int component, float value) throws IllegalArgumentException {
        switch (component) {
            case 0:
                this.x = value;
                break;
            case 1:
                this.y = value;
                break;
            default:
                throw new IllegalArgumentException();
        }

        return this;
    }

    public Vector2f perpendicular() {
        float tmp = this.y;
        this.y = -this.x;
        this.x = tmp;
        return this;
    }

    public Vector2f sub(Vector2f v) {
        this.x -= v.x;
        this.y -= v.y;
        return this;
    }

    public Vector2f sub(Vector2f v, Vector2f dest) {
        dest.x = this.x - v.x;
        dest.y = this.y - v.y;
        return dest;
    }

    public Vector2f sub(float x, float y) {
        this.x -= x;
        this.y -= y;
        return this;
    }

    public Vector2f sub(float x, float y, Vector2f dest) {
        dest.x = this.x - x;
        dest.y = this.y - y;
        return dest;
    }

    public float dot(Vector2f v) {
        return this.x * v.x + this.y * v.y;
    }

    public float angle(Vector2f v) {
        float dot = this.x * v.x + this.y * v.y;
        float det = this.x * v.y - this.y * v.x;
        return Math.atan2(det, dot);
    }

    public float lengthSquared() {
        return this.x * this.x + this.y * this.y;
    }

    public static float lengthSquared(float x, float y) {
        return x * x + y * y;
    }

    public float length() {
        return Math.sqrt(this.x * this.x + this.y * this.y);
    }

    public static float length(float x, float y) {
        return Math.sqrt(x * x + y * y);
    }

    public float distance(Vector2f v) {
        float dx = this.x - v.x;
        float dy = this.y - v.y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    public float distanceSquared(Vector2f v) {
        float dx = this.x - v.x;
        float dy = this.y - v.y;
        return dx * dx + dy * dy;
    }

    public float distance(float x, float y) {
        float dx = this.x - x;
        float dy = this.y - y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    public float distanceSquared(float x, float y) {
        float dx = this.x - x;
        float dy = this.y - y;
        return dx * dx + dy * dy;
    }

    public static float distance(float x1, float y1, float x2, float y2) {
        float dx = x1 - x2;
        float dy = y1 - y2;
        return Math.sqrt(dx * dx + dy * dy);
    }

    public static float distanceSquared(float x1, float y1, float x2, float y2) {
        float dx = x1 - x2;
        float dy = y1 - y2;
        return dx * dx + dy * dy;
    }

    public Vector2f normalize() {
        float invLength = Math.invsqrt(this.x * this.x + this.y * this.y);
        this.x *= invLength;
        this.y *= invLength;
        return this;
    }

    public Vector2f normalize(Vector2f dest) {
        float invLength = Math.invsqrt(this.x * this.x + this.y * this.y);
        dest.x = this.x * invLength;
        dest.y = this.y * invLength;
        return dest;
    }

    public Vector2f normalize(float length) {
        float invLength = Math.invsqrt(this.x * this.x + this.y * this.y) * length;
        this.x *= invLength;
        this.y *= invLength;
        return this;
    }

    public Vector2f normalize(float length, Vector2f dest) {
        float invLength = Math.invsqrt(this.x * this.x + this.y * this.y) * length;
        dest.x = this.x * invLength;
        dest.y = this.y * invLength;
        return dest;
    }

    public Vector2f add(Vector2f v) {
        this.x += v.x;
        this.y += v.y;
        return this;
    }

    public Vector2f add(Vector2f v, Vector2f dest) {
        dest.x = this.x + v.x;
        dest.y = this.y + v.y;
        return dest;
    }

    public Vector2f add(float x, float y) {
        return this.add(x, y, this);
    }

    public Vector2f add(float x, float y, Vector2f dest) {
        dest.x = this.x + x;
        dest.y = this.y + y;
        return dest;
    }

    public Vector2f zero() {
        this.x = 0f;
        this.y = 0f;
        return this;
    }

    public Vector2f negate() {
        this.x = -this.x;
        this.y = -this.y;
        return this;
    }

    public Vector2f negate(Vector2f dest) {
        dest.x = -this.x;
        dest.y = -this.y;
        return dest;
    }

    public Vector2f mul(float scalar) {
        this.x *= scalar;
        this.y *= scalar;
        return this;
    }

    public Vector2f mul(float scalar, Vector2f dest) {
        dest.x = this.x * scalar;
        dest.y = this.y * scalar;
        return dest;
    }

    public Vector2f mul(float x, float y) {
        this.x *= x;
        this.y *= y;
        return this;
    }

    public Vector2f mul(float x, float y, Vector2f dest) {
        dest.x = this.x * x;
        dest.y = this.y * y;
        return dest;
    }

    public Vector2f mul(Vector2f v) {
        this.x *= v.x;
        this.y *= v.y;
        return this;
    }

    public Vector2f mul(Vector2f v, Vector2f dest) {
        dest.x = this.x * v.x;
        dest.y = this.y * v.y;
        return dest;
    }

    public Vector2f div(Vector2f v) {
        this.x /= v.x;
        this.y /= v.y;
        return this;
    }

    public Vector2f div(Vector2f v, Vector2f dest) {
        dest.x = this.x / v.x;
        dest.y = this.y / v.y;
        return dest;
    }

    public Vector2f div(float scalar) {
        float inv = 1f / scalar;
        this.x *= inv;
        this.y *= inv;
        return this;
    }

    public Vector2f div(float scalar, Vector2f dest) {
        float inv = 1f / scalar;
        dest.x = this.x * inv;
        dest.y = this.y * inv;
        return dest;
    }

    public Vector2f div(float x, float y) {
        this.x /= x;
        this.y /= y;
        return this;
    }

    public Vector2f div(float x, float y, Vector2f dest) {
        dest.x = this.x / x;
        dest.y = this.y / y;
        return dest;
    }

    public Vector2f mul(Matrix2f mat) {
        float rx = mat.m00 * this.x + mat.m10 * this.y;
        float ry = mat.m01 * this.x + mat.m11 * this.y;
        this.x = rx;
        this.y = ry;
        return this;
    }

    public Vector2f mul(Matrix2f mat, Vector2f dest) {
        float rx = mat.m00 * this.x + mat.m10 * this.y;
        float ry = mat.m01 * this.x + mat.m11 * this.y;
        dest.x = rx;
        dest.y = ry;
        return dest;
    }

    public Vector2f mul(Matrix2d mat) {
        double rx = mat.m00 * (double) this.x + mat.m10 * (double) this.y;
        double ry = mat.m01 * (double) this.x + mat.m11 * (double) this.y;
        this.x = (float) rx;
        this.y = (float) ry;
        return this;
    }

    public Vector2f mul(Matrix2d mat, Vector2f dest) {
        double rx = mat.m00 * (double) this.x + mat.m10 * (double) this.y;
        double ry = mat.m01 * (double) this.x + mat.m11 * (double) this.y;
        dest.x = (float) rx;
        dest.y = (float) ry;
        return dest;
    }

    public Vector2f mulTranspose(Matrix2f mat) {
        float rx = mat.m00 * this.x + mat.m01 * this.y;
        float ry = mat.m10 * this.x + mat.m11 * this.y;
        this.x = rx;
        this.y = ry;
        return this;
    }

    public Vector2f mulTranspose(Matrix2f mat, Vector2f dest) {
        float rx = mat.m00 * this.x + mat.m01 * this.y;
        float ry = mat.m10 * this.x + mat.m11 * this.y;
        dest.x = rx;
        dest.y = ry;
        return dest;
    }

    public Vector2f mulPosition(Matrix3x2f mat) {
        this.x = mat.m00 * this.x + mat.m10 * this.y + mat.m20;
        this.y = mat.m01 * this.x + mat.m11 * this.y + mat.m21;
        return this;
    }

    public Vector2f mulPosition(Matrix3x2f mat, Vector2f dest) {
        dest.x = mat.m00 * this.x + mat.m10 * this.y + mat.m20;
        dest.y = mat.m01 * this.x + mat.m11 * this.y + mat.m21;
        return dest;
    }

    public Vector2f mulDirection(Matrix3x2f mat) {
        this.x = mat.m00 * this.x + mat.m10 * this.y;
        this.y = mat.m01 * this.x + mat.m11 * this.y;
        return this;
    }

    public Vector2f mulDirection(Matrix3x2f mat, Vector2f dest) {
        dest.x = mat.m00 * this.x + mat.m10 * this.y;
        dest.y = mat.m01 * this.x + mat.m11 * this.y;
        return dest;
    }

    public Vector2f lerp(Vector2f other, float t) {
        this.x += (other.x - this.x) * t;
        this.y += (other.y - this.y) * t;
        return this;
    }

    public Vector2f lerp(Vector2f other, float t, Vector2f dest) {
        dest.x = this.x + (other.x - this.x) * t;
        dest.y = this.y + (other.y - this.y) * t;
        return dest;
    }

    public int hashCode() {
        int result = Float.floatToIntBits(this.x);
        result = 31 * result + Float.floatToIntBits(this.y);
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
            Vector2f other = (Vector2f) obj;
            if (Float.floatToIntBits(this.x) != Float.floatToIntBits(other.x)) {
                return false;
            } else {
                return Float.floatToIntBits(this.y) == Float.floatToIntBits(other.y);
            }
        }
    }

    public boolean equals(Vector2f v, float delta) {
        if (this == v) {
            return true;
        } else if (v == null) {
            return false;
        } else if (!Runtime.equals(this.x, v.x, delta)) {
            return false;
        } else {
            return Runtime.equals(this.y, v.y, delta);
        }
    }

    public boolean equals(float x, float y) {
        if (Float.floatToIntBits(this.x) != Float.floatToIntBits(x)) {
            return false;
        } else {
            return Float.floatToIntBits(this.y) == Float.floatToIntBits(y);
        }
    }

    public String toString() {
        return "(" + x + "," + y + ")";
    }

    public Vector2f fma(Vector2f a, Vector2f b) {
        this.x += a.x * b.x;
        this.y += a.y * b.y;
        return this;
    }

    public Vector2f fma(float a, Vector2f b) {
        this.x += a * b.x;
        this.y += a * b.y;
        return this;
    }

    public Vector2f fma(Vector2f a, Vector2f b, Vector2f dest) {
        dest.x = this.x + a.x * b.x;
        dest.y = this.y + a.y * b.y;
        return dest;
    }

    public Vector2f fma(float a, Vector2f b, Vector2f dest) {
        dest.x = this.x + a * b.x;
        dest.y = this.y + a * b.y;
        return dest;
    }

    public Vector2f min(Vector2f v) {
        this.x = java.lang.Math.min(this.x, v.x);
        this.y = java.lang.Math.min(this.y, v.y);
        return this;
    }

    public Vector2f min(Vector2f v, Vector2f dest) {
        dest.x = java.lang.Math.min(this.x, v.x);
        dest.y = java.lang.Math.min(this.y, v.y);
        return dest;
    }

    public Vector2f max(Vector2f v) {
        this.x = java.lang.Math.max(this.x, v.x);
        this.y = java.lang.Math.max(this.y, v.y);
        return this;
    }

    public Vector2f max(Vector2f v, Vector2f dest) {
        dest.x = java.lang.Math.max(this.x, v.x);
        dest.y = java.lang.Math.max(this.y, v.y);
        return dest;
    }

    public int maxComponent() {
        float absX = Math.abs(this.x);
        float absY = Math.abs(this.y);
        return absX >= absY ? 0 : 1;
    }

    public int minComponent() {
        float absX = Math.abs(this.x);
        float absY = Math.abs(this.y);
        return absX < absY ? 0 : 1;
    }

    public Vector2f floor() {
        this.x = Math.floor(this.x);
        this.y = Math.floor(this.y);
        return this;
    }

    public Vector2f floor(Vector2f dest) {
        dest.x = Math.floor(this.x);
        dest.y = Math.floor(this.y);
        return dest;
    }

    public Vector2f ceil() {
        this.x = Math.ceil(this.x);
        this.y = Math.ceil(this.y);
        return this;
    }

    public Vector2f ceil(Vector2f dest) {
        dest.x = Math.ceil(this.x);
        dest.y = Math.ceil(this.y);
        return dest;
    }

    public Vector2f round() {
        this.x = Math.ceil(this.x);
        this.y = Math.ceil(this.y);
        return this;
    }

    public Vector2f round(Vector2f dest) {
        dest.x = (float) Math.round(this.x);
        dest.y = (float) Math.round(this.y);
        return dest;
    }

    public boolean isFinite() {
        return Math.isFinite(this.x) && Math.isFinite(this.y);
    }

    public Vector2f absolute() {
        this.x = Math.abs(this.x);
        this.y = Math.abs(this.y);
        return this;
    }

    public Vector2f absolute(Vector2f dest) {
        dest.x = Math.abs(this.x);
        dest.y = Math.abs(this.y);
        return dest;
    }

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
