package org.joml;

@SuppressWarnings("unused")
public class Vector2d {
    
    public double x;
    public double y;

    public Vector2d() {
    }

    public Vector2d(double d) {
        this.x = d;
        this.y = d;
    }

    public Vector2d(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public Vector2d(Vector2d v) {
        this.x = v.x;
        this.y = v.y;
    }

    public Vector2d(Vector2f v) {
        this.x = v.x;
        this.y = v.y;
    }

    public Vector2d(Vector2i v) {
        this.x = v.x;
        this.y = v.y;
    }

    public Vector2d(double[] xy) {
        this.x = xy[0];
        this.y = xy[1];
    }

    public Vector2d(float[] xy) {
        this.x = xy[0];
        this.y = xy[1];
    }

    public Vector2d set(double d) {
        this.x = d;
        this.y = d;
        return this;
    }

    public Vector2d set(double x, double y) {
        this.x = x;
        this.y = y;
        return this;
    }

    public Vector2d set(Vector2d v) {
        this.x = v.x;
        this.y = v.y;
        return this;
    }

    public Vector2d set(Vector2f v) {
        this.x = v.x;
        this.y = v.y;
        return this;
    }

    public Vector2d set(Vector2i v) {
        this.x = v.x;
        this.y = v.y;
        return this;
    }

    public Vector2d set(double[] xy) {
        this.x = xy[0];
        this.y = xy[1];
        return this;
    }

    public Vector2d set(float[] xy) {
        this.x = xy[0];
        this.y = xy[1];
        return this;
    }

    public double get(int component) throws IllegalArgumentException {
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
        dest.x = (float) this.x;
        dest.y = (float) this.y;
        return dest;
    }

    public Vector2d get(Vector2d dest) {
        dest.x = this.x;
        dest.y = this.y;
        return dest;
    }

    public Vector2d setComponent(int component, double value) throws IllegalArgumentException {
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

    public Vector2d perpendicular() {
        double tmp = this.y;
        this.y = -this.x;
        this.x = tmp;
        return this;
    }

    public Vector2d sub(Vector2d v) {
        this.x -= v.x;
        this.y -= v.y;
        return this;
    }

    public Vector2d sub(double x, double y) {
        this.x -= x;
        this.y -= y;
        return this;
    }

    public Vector2d sub(double x, double y, Vector2d dest) {
        dest.x = this.x - x;
        dest.y = this.y - y;
        return dest;
    }

    public Vector2d sub(Vector2f v) {
        this.x -= v.x;
        this.y -= v.y;
        return this;
    }

    public Vector2d sub(Vector2d v, Vector2d dest) {
        dest.x = this.x - v.x;
        dest.y = this.y - v.y;
        return dest;
    }

    public Vector2d sub(Vector2f v, Vector2d dest) {
        dest.x = this.x - (double) v.x;
        dest.y = this.y - (double) v.y;
        return dest;
    }

    public Vector2d mul(double scalar) {
        this.x *= scalar;
        this.y *= scalar;
        return this;
    }

    public Vector2d mul(double scalar, Vector2d dest) {
        dest.x = this.x * scalar;
        dest.y = this.y * scalar;
        return dest;
    }

    public Vector2d mul(double x, double y) {
        this.x *= x;
        this.y *= y;
        return this;
    }

    public Vector2d mul(double x, double y, Vector2d dest) {
        dest.x = this.x * x;
        dest.y = this.y * y;
        return dest;
    }

    public Vector2d mul(Vector2d v) {
        this.x *= v.x;
        this.y *= v.y;
        return this;
    }

    public Vector2d mul(Vector2d v, Vector2d dest) {
        dest.x = this.x * v.x;
        dest.y = this.y * v.y;
        return dest;
    }

    public Vector2d div(double scalar) {
        double inv = 1.0 / scalar;
        this.x *= inv;
        this.y *= inv;
        return this;
    }

    public Vector2d div(double scalar, Vector2d dest) {
        double inv = 1.0 / scalar;
        dest.x = this.x * inv;
        dest.y = this.y * inv;
        return dest;
    }

    public Vector2d div(double x, double y) {
        this.x /= x;
        this.y /= y;
        return this;
    }

    public Vector2d div(double x, double y, Vector2d dest) {
        dest.x = this.x / x;
        dest.y = this.y / y;
        return dest;
    }

    public Vector2d div(Vector2d v) {
        this.x /= v.x;
        this.y /= v.y;
        return this;
    }

    public Vector2d div(Vector2f v) {
        this.x /= v.x;
        this.y /= v.y;
        return this;
    }

    public Vector2d div(Vector2f v, Vector2d dest) {
        dest.x = this.x / (double) v.x;
        dest.y = this.y / (double) v.y;
        return dest;
    }

    public Vector2d div(Vector2d v, Vector2d dest) {
        dest.x = this.x / v.x;
        dest.y = this.y / v.y;
        return dest;
    }

    public Vector2d mul(Matrix2f mat) {
        double rx = (double) mat.m00 * this.x + (double) mat.m10 * this.y;
        double ry = (double) mat.m01 * this.x + (double) mat.m11 * this.y;
        this.x = rx;
        this.y = ry;
        return this;
    }

    public Vector2d mul(Matrix2d mat) {
        double rx = mat.m00 * this.x + mat.m10 * this.y;
        double ry = mat.m01 * this.x + mat.m11 * this.y;
        this.x = rx;
        this.y = ry;
        return this;
    }

    public Vector2d mul(Matrix2d mat, Vector2d dest) {
        double rx = mat.m00 * this.x + mat.m10 * this.y;
        double ry = mat.m01 * this.x + mat.m11 * this.y;
        dest.x = rx;
        dest.y = ry;
        return dest;
    }

    public Vector2d mul(Matrix2f mat, Vector2d dest) {
        double rx = (double) mat.m00 * this.x + (double) mat.m10 * this.y;
        double ry = (double) mat.m01 * this.x + (double) mat.m11 * this.y;
        dest.x = rx;
        dest.y = ry;
        return dest;
    }

    public Vector2d mulTranspose(Matrix2d mat) {
        double rx = mat.m00 * this.x + mat.m01 * this.y;
        double ry = mat.m10 * this.x + mat.m11 * this.y;
        this.x = rx;
        this.y = ry;
        return this;
    }

    public Vector2d mulTranspose(Matrix2d mat, Vector2d dest) {
        double rx = mat.m00 * this.x + mat.m01 * this.y;
        double ry = mat.m10 * this.x + mat.m11 * this.y;
        dest.x = rx;
        dest.y = ry;
        return dest;
    }

    public Vector2d mulTranspose(Matrix2f mat) {
        double rx = (double) mat.m00 * this.x + (double) mat.m01 * this.y;
        double ry = (double) mat.m10 * this.x + (double) mat.m11 * this.y;
        this.x = rx;
        this.y = ry;
        return this;
    }

    public Vector2d mulTranspose(Matrix2f mat, Vector2d dest) {
        double rx = (double) mat.m00 * this.x + (double) mat.m01 * this.y;
        double ry = (double) mat.m10 * this.x + (double) mat.m11 * this.y;
        dest.x = rx;
        dest.y = ry;
        return dest;
    }

    public Vector2d mulPosition(Matrix3x2d mat) {
        double rx = mat.m00 * this.x + mat.m10 * this.y + mat.m20;
        double ry = mat.m01 * this.x + mat.m11 * this.y + mat.m21;
        this.x = rx;
        this.y = ry;
        return this;
    }

    public Vector2d mulPosition(Matrix3x2d mat, Vector2d dest) {
        double rx = mat.m00 * this.x + mat.m10 * this.y + mat.m20;
        double ry = mat.m01 * this.x + mat.m11 * this.y + mat.m21;
        dest.x = rx;
        dest.y = ry;
        return dest;
    }

    public Vector2d mulDirection(Matrix3x2d mat) {
        double rx = mat.m00 * this.x + mat.m10 * this.y;
        double ry = mat.m01 * this.x + mat.m11 * this.y;
        this.x = rx;
        this.y = ry;
        return this;
    }

    public Vector2d mulDirection(Matrix3x2d mat, Vector2d dest) {
        double rx = mat.m00 * this.x + mat.m10 * this.y;
        double ry = mat.m01 * this.x + mat.m11 * this.y;
        dest.x = rx;
        dest.y = ry;
        return dest;
    }

    public double dot(Vector2d v) {
        return this.x * v.x + this.y * v.y;
    }

    public double angle(Vector2d v) {
        double dot = this.x * v.x + this.y * v.y;
        double det = this.x * v.y - this.y * v.x;
        return Math.atan2(det, dot);
    }

    public double lengthSquared() {
        return this.x * this.x + this.y * this.y;
    }

    public static double lengthSquared(double x, double y) {
        return x * x + y * y;
    }

    public double length() {
        return Math.sqrt(this.x * this.x + this.y * this.y);
    }

    public static double length(double x, double y) {
        return Math.sqrt(x * x + y * y);
    }

    public double distance(Vector2d v) {
        double dx = this.x - v.x;
        double dy = this.y - v.y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    public double distanceSquared(Vector2d v) {
        double dx = this.x - v.x;
        double dy = this.y - v.y;
        return dx * dx + dy * dy;
    }

    public double distance(Vector2f v) {
        double dx = this.x - (double) v.x;
        double dy = this.y - (double) v.y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    public double distanceSquared(Vector2f v) {
        double dx = this.x - (double) v.x;
        double dy = this.y - (double) v.y;
        return dx * dx + dy * dy;
    }

    public double distance(double x, double y) {
        double dx = this.x - x;
        double dy = this.y - y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    public double distanceSquared(double x, double y) {
        double dx = this.x - x;
        double dy = this.y - y;
        return dx * dx + dy * dy;
    }

    public static double distance(double x1, double y1, double x2, double y2) {
        double dx = x1 - x2;
        double dy = y1 - y2;
        return Math.sqrt(dx * dx + dy * dy);
    }

    public static double distanceSquared(double x1, double y1, double x2, double y2) {
        double dx = x1 - x2;
        double dy = y1 - y2;
        return dx * dx + dy * dy;
    }

    public Vector2d normalize() {
        double invLength = Math.invsqrt(this.x * this.x + this.y * this.y);
        this.x *= invLength;
        this.y *= invLength;
        return this;
    }

    public Vector2d normalize(Vector2d dest) {
        double invLength = Math.invsqrt(this.x * this.x + this.y * this.y);
        dest.x = this.x * invLength;
        dest.y = this.y * invLength;
        return dest;
    }

    public Vector2d normalize(double length) {
        double invLength = Math.invsqrt(this.x * this.x + this.y * this.y) * length;
        this.x *= invLength;
        this.y *= invLength;
        return this;
    }

    public Vector2d normalize(double length, Vector2d dest) {
        double invLength = Math.invsqrt(this.x * this.x + this.y * this.y) * length;
        dest.x = this.x * invLength;
        dest.y = this.y * invLength;
        return dest;
    }

    public Vector2d add(Vector2d v) {
        this.x += v.x;
        this.y += v.y;
        return this;
    }

    public Vector2d add(double x, double y) {
        this.x += x;
        this.y += y;
        return this;
    }

    public Vector2d add(double x, double y, Vector2d dest) {
        dest.x = this.x + x;
        dest.y = this.y + y;
        return dest;
    }

    public Vector2d add(Vector2f v) {
        this.x += v.x;
        this.y += v.y;
        return this;
    }

    public Vector2d add(Vector2d v, Vector2d dest) {
        dest.x = this.x + v.x;
        dest.y = this.y + v.y;
        return dest;
    }

    public Vector2d add(Vector2f v, Vector2d dest) {
        dest.x = this.x + (double) v.x;
        dest.y = this.y + (double) v.y;
        return dest;
    }

    public Vector2d zero() {
        this.x = 0.0;
        this.y = 0.0;
        return this;
    }

    public Vector2d negate() {
        this.x = -this.x;
        this.y = -this.y;
        return this;
    }

    public Vector2d negate(Vector2d dest) {
        dest.x = -this.x;
        dest.y = -this.y;
        return dest;
    }

    public Vector2d lerp(Vector2d other, double t) {
        this.x += (other.x - this.x) * t;
        this.y += (other.y - this.y) * t;
        return this;
    }

    public Vector2d lerp(Vector2d other, double t, Vector2d dest) {
        dest.x = this.x + (other.x - this.x) * t;
        dest.y = this.y + (other.y - this.y) * t;
        return dest;
    }

    public int hashCode() {
        int result = 1;
        long temp = Double.doubleToLongBits(this.x);
        result = 31 * result + (int) (temp ^ temp >>> 32);
        temp = Double.doubleToLongBits(this.y);
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
            Vector2d other = (Vector2d) obj;
            if (Double.doubleToLongBits(this.x) != Double.doubleToLongBits(other.x)) {
                return false;
            } else {
                return Double.doubleToLongBits(this.y) == Double.doubleToLongBits(other.y);
            }
        }
    }

    public boolean equals(Vector2d v, double delta) {
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

    public boolean equals(double x, double y) {
        if (Double.doubleToLongBits(this.x) != Double.doubleToLongBits(x)) {
            return false;
        } else {
            return Double.doubleToLongBits(this.y) == Double.doubleToLongBits(y);
        }
    }

    public String toString() {
        return "(" + x + "," + y + ")";
    }

    public Vector2d fma(Vector2d a, Vector2d b) {
        this.x += a.x * b.x;
        this.y += a.y * b.y;
        return this;
    }

    public Vector2d fma(double a, Vector2d b) {
        this.x += a * b.x;
        this.y += a * b.y;
        return this;
    }

    public Vector2d fma(Vector2d a, Vector2d b, Vector2d dest) {
        dest.x = this.x + a.x * b.x;
        dest.y = this.y + a.y * b.y;
        return dest;
    }

    public Vector2d fma(double a, Vector2d b, Vector2d dest) {
        dest.x = this.x + a * b.x;
        dest.y = this.y + a * b.y;
        return dest;
    }

    public Vector2d min(Vector2d v) {
        this.x = java.lang.Math.min(this.x, v.x);
        this.y = java.lang.Math.min(this.y, v.y);
        return this;
    }

    public Vector2d min(Vector2d v, Vector2d dest) {
        dest.x = java.lang.Math.min(this.x, v.x);
        dest.y = java.lang.Math.min(this.y, v.y);
        return dest;
    }

    public Vector2d max(Vector2d v) {
        this.x = java.lang.Math.max(this.x, v.x);
        this.y = java.lang.Math.max(this.y, v.y);
        return this;
    }

    public Vector2d max(Vector2d v, Vector2d dest) {
        dest.x = java.lang.Math.max(this.x, v.x);
        dest.y = java.lang.Math.max(this.y, v.y);
        return dest;
    }

    public int maxComponent() {
        double absX = Math.abs(this.x);
        double absY = Math.abs(this.y);
        return absX >= absY ? 0 : 1;
    }

    public int minComponent() {
        double absX = Math.abs(this.x);
        double absY = Math.abs(this.y);
        return absX < absY ? 0 : 1;
    }

    public Vector2d floor() {
        this.x = Math.floor(this.x);
        this.y = Math.floor(this.y);
        return this;
    }

    public Vector2d floor(Vector2d dest) {
        dest.x = Math.floor(this.x);
        dest.y = Math.floor(this.y);
        return dest;
    }

    public Vector2d ceil() {
        this.x = Math.ceil(this.x);
        this.y = Math.ceil(this.y);
        return this;
    }

    public Vector2d ceil(Vector2d dest) {
        dest.x = Math.ceil(this.x);
        dest.y = Math.ceil(this.y);
        return dest;
    }

    public Vector2d round() {
        this.x = (double) Math.round(this.x);
        this.y = (double) Math.round(this.y);
        return this;
    }

    public Vector2d round(Vector2d dest) {
        dest.x = (double) Math.round(this.x);
        dest.y = (double) Math.round(this.y);
        return dest;
    }

    public boolean isFinite() {
        return Math.isFinite(this.x) && Math.isFinite(this.y);
    }

    public Vector2d absolute() {
        this.x = Math.abs(this.x);
        this.y = Math.abs(this.y);
        return this;
    }

    public Vector2d absolute(Vector2d dest) {
        dest.x = Math.abs(this.x);
        dest.y = Math.abs(this.y);
        return dest;
    }

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}
